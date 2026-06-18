package com.memoriabox.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.memoriabox.data.model.*
import com.memoriabox.repository.EventRepository
import com.memoriabox.repository.FriendRepository
import com.memoriabox.repository.LogRepository
import com.memoriabox.repository.LabelRepository
import com.memoriabox.repository.DiaryRepository
import com.memoriabox.utils.BackupManager
import com.memoriabox.utils.AppSettings
import com.memoriabox.utils.MonthlySummaryHelper
import com.memoriabox.utils.MonthlySummaryUiState
import com.memoriabox.utils.NotificationHelper
import com.memoriabox.utils.SystemCalendarHelper
import com.memoriabox.utils.startOfMonth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class MainViewModel(
    application: Application,
    private val boxRepository: com.memoriabox.repository.BoxRepository,
    private val eventRepository: EventRepository,
    private val logRepository: LogRepository,
    private val backupManager: BackupManager,
    private val notificationHelper: NotificationHelper
) : AndroidViewModel(application) {

    val boxes = boxRepository.getAllActiveBoxes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedBoxes = boxRepository.getArchivedBoxes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvents = eventRepository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs = logRepository.getRecentLogs(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createBox(name: String, icon: String, bgType: BgType, bgValue: String) = viewModelScope.launch {
        try {
            val box = Box(name = name, icon = icon, bgType = bgType, bgValue = bgValue)
            boxRepository.insertBox(box)
            logRepository.logBoxOperation("CREATE", box.id, box.name)
            backupManager.onDataChanged()
        } catch (e: Exception) {
            logRepository.logBoxOperation("CREATE", "", name, "failed: ${e.message}")
        }
    }

    fun updateBox(box: Box) = viewModelScope.launch {
        try {
            boxRepository.updateBox(box)
            logRepository.logBoxOperation("UPDATE", box.id, box.name)
            backupManager.onDataChanged()
        } catch (e: Exception) {
            logRepository.logBoxOperation("UPDATE", box.id, box.name, "failed: ${e.message}")
        }
    }

    fun deleteBox(box: Box) = viewModelScope.launch {
        try {
            boxRepository.deleteBox(box)
            logRepository.logBoxOperation("DELETE", box.id, box.name)
            backupManager.onDataChanged()
        } catch (e: Exception) {
            logRepository.logBoxOperation("DELETE", box.id, box.name, "failed: ${e.message}")
        }
    }

    fun archiveBox(id: String, name: String) = viewModelScope.launch {
        try {
            boxRepository.archiveBox(id)
            logRepository.logBoxOperation("ARCHIVE", id, name)
        } catch (e: Exception) {
            logRepository.logBoxOperation("ARCHIVE", id, name, "failed: ${e.message}")
        }
    }

    fun restoreBox(id: String, name: String) = viewModelScope.launch {
        try {
            boxRepository.restoreBox(id)
            logRepository.logBoxOperation("RESTORE", id, name)
        } catch (e: Exception) {
            logRepository.logBoxOperation("RESTORE", id, name, "failed: ${e.message}")
        }
    }

    fun createQuickEvent(event: Event) = viewModelScope.launch {
        try {
            val targetEvent = if (event.boxId.isBlank()) {
                val defaultBox = Box(
                    id = "default_1",
                    name = "我的日子",
                    icon = "*",
                    bgType = BgType.COLOR,
                    bgValue = "#7C4DFF"
                )
                boxRepository.insertBox(defaultBox)
                event.copy(boxId = defaultBox.id)
            } else {
                event
            }

            eventRepository.insertEvent(targetEvent)
            runEventSideEffects(targetEvent, notificationHelper, backupManager)
            logRepository.logEventOperation("QUICK_CREATE", targetEvent.id, targetEvent.name)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Quick create event failed", e)
        }
    }

    fun updateQuickEvent(event: Event) = viewModelScope.launch {
        try {
            eventRepository.getEventById(event.id)?.let { notificationHelper.cancelReminder(it) }
            eventRepository.updateEvent(event)
            runEventSideEffects(event, notificationHelper, backupManager)
            logRepository.logEventOperation("QUICK_UPDATE", event.id, event.name)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Quick update event failed", e)
        }
    }

    fun deleteQuickEvent(event: Event) = viewModelScope.launch {
        try {
            notificationHelper.cancelReminder(event)
            eventRepository.deleteEvent(event)
            logRepository.logEventOperation("QUICK_DELETE", event.id, event.name)
            backupManager.onDataChanged()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Quick delete event failed", e)
        }
    }

    fun togglePinned(event: Event) = viewModelScope.launch {
        try {
            eventRepository.updatePinned(event.id, !event.isPinned)
            logRepository.logEventOperation("PIN", event.id, event.name)
            backupManager.onDataChanged()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Toggle pin failed", e)
        }
    }
}

class BoxDetailViewModel(
    application: Application,
    private val eventRepository: EventRepository,
    private val boxRepository: com.memoriabox.repository.BoxRepository,
    private val logRepository: LogRepository,
    private val backupManager: BackupManager,
    private val notificationHelper: NotificationHelper
) : AndroidViewModel(application) {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    private val _box = MutableStateFlow<Box?>(null)

    val events = _events.asStateFlow()
    val box = _box.asStateFlow()
    val allBoxes = boxRepository.getAllActiveBoxes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadBox(boxId: String) = viewModelScope.launch {
        try {
            val b = boxRepository.getBoxById(boxId)
            _box.value = b
            eventRepository.getEventsByBoxId(boxId).collect { newEvents ->
                _events.value = newEvents
            }
        } catch (e: Exception) {
            Log.e("BoxDetailVM", "Load failed", e)
        }
    }

    fun createEvent(event: Event) = viewModelScope.launch {
        try {
            eventRepository.insertEvent(event)
            runEventSideEffects(event, notificationHelper, backupManager)
            logRepository.logEventOperation("CREATE", event.id, event.name)
        } catch (e: Exception) {
            Log.e("BoxDetailVM", "Create event failed", e)
        }
    }

    fun updateEvent(event: Event) = viewModelScope.launch {
        try {
            eventRepository.getEventById(event.id)?.let { notificationHelper.cancelReminder(it) }
            eventRepository.updateEvent(event)
            runEventSideEffects(event, notificationHelper, backupManager)
            logRepository.logEventOperation("UPDATE", event.id, event.name)
        } catch (e: Exception) {
            Log.e("BoxDetailVM", "Update event failed", e)
        }
    }

    fun deleteEvent(event: Event) = viewModelScope.launch {
        try {
            notificationHelper.cancelReminder(event)
            eventRepository.deleteEvent(event)
            logRepository.logEventOperation("DELETE", event.id, event.name)
            backupManager.onDataChanged()
        } catch (e: Exception) {
            Log.e("BoxDetailVM", "Delete event failed", e)
        }
    }

    fun deleteEvents(ids: Set<String>) = viewModelScope.launch {
        if (ids.isEmpty()) return@launch
        try {
            val selectedEvents = eventRepository.getEventsByIds(ids.toList())
            selectedEvents.forEach { notificationHelper.cancelReminder(it) }
            eventRepository.deleteEventsByIds(ids.toList())
            logRepository.logEventOperation("BATCH_DELETE", ids.joinToString(), "${ids.size} events")
            backupManager.onDataChanged()
        } catch (e: Exception) {
            Log.e("BoxDetailVM", "Batch delete failed", e)
        }
    }

    fun moveEvents(ids: Set<String>, targetBoxId: String) = viewModelScope.launch {
        if (ids.isEmpty()) return@launch
        try {
            eventRepository.moveEventsToBox(ids.toList(), targetBoxId)
            logRepository.logEventOperation("BATCH_MOVE", ids.joinToString(), "${ids.size} events")
            backupManager.onDataChanged()
        } catch (e: Exception) {
            Log.e("BoxDetailVM", "Batch move failed", e)
        }
    }
}

class CalendarViewModel(
    application: Application,
    private val eventRepository: EventRepository,
    private val diaryRepository: DiaryRepository,
    private val backupManager: BackupManager
) : AndroidViewModel(application) {

    val allEvents = eventRepository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDiaries = diaryRepository.getAllDiaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allDiaryMedia = allDiaries
        .flatMapLatest { diaries ->
            if (diaries.isEmpty()) flowOf(emptyList()) else diaryRepository.getMediaForDiaries(diaries.map { it.id })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDiaryMedia = MutableStateFlow<List<DiaryMedia>>(emptyList())
    val selectedDiaryMedia = _selectedDiaryMedia.asStateFlow()

    private val _monthlySummary = MutableStateFlow(MonthlySummaryUiState())
    val monthlySummary = _monthlySummary.asStateFlow()

    fun getEventsForDay(timestamp: Long, events: List<Event>): List<Event> {
        val dayStart = (timestamp / 86400000) * 86400000
        val dayEnd = dayStart + 86400000 - 1
        return events.filter { it.date >= dayStart && it.date <= dayEnd }
    }

    fun loadDiaryMedia(diaryId: String) = viewModelScope.launch {
        _selectedDiaryMedia.value = diaryRepository.getMediaForDiaryOnce(diaryId)
    }

    fun saveDiary(date: Long, content: String, mediaUris: List<String>, backgroundUri: String?) = viewModelScope.launch {
        saveDiaryInternal(null, date, content, mediaUris.mapIndexed { index, uri ->
            DiaryMedia(
                diaryId = "",
                mediaUri = uri,
                mediaType = inferDiaryMediaType(uri),
                sortOrder = index
            )
        }, backgroundUri)
    }

    fun saveDiaryWithMedia(existingDiary: DiaryEntry?, date: Long, content: String, mediaItems: List<DiaryMedia>, backgroundUri: String?) = viewModelScope.launch {
        saveDiaryInternal(existingDiary, date, content, mediaItems, backgroundUri)
    }

    fun loadMonthlySummary(monthStart: Long) = viewModelScope.launch {
        val context = getApplication<Application>()
        val normalizedMonth = startOfMonth(monthStart)
        _monthlySummary.value = _monthlySummary.value.copy(monthStart = normalizedMonth, isLoading = true)
        val (start, end) = MonthlySummaryHelper.monthRange(normalizedMonth)
        val diaries = diaryRepository.getDiariesBetweenOnce(start, end)
        val media = if (diaries.isEmpty()) emptyList() else diaryRepository.getMediaForDiariesOnce(diaries.map { it.id })
        _monthlySummary.value = MonthlySummaryHelper.buildSummary(
            monthStart = normalizedMonth,
            diaries = diaries,
            media = media,
            summaryEnabled = AppSettings.getMonthlySummaryTextEnabled(context),
            playMode = AppSettings.getMonthlySummaryPlayMode(context),
            playSpeedFactor = AppSettings.getMonthlySummaryPlaySpeedFactor(context)
        )
    }

    private suspend fun saveDiaryInternal(existingDiary: DiaryEntry?, date: Long, content: String, mediaItems: List<DiaryMedia>, backgroundUri: String?) {
        if (existingDiary != null && content.isBlank() && mediaItems.isEmpty() && backgroundUri == null) {
            diaryRepository.deleteDiary(existingDiary)
            _selectedDiaryMedia.value = emptyList()
            return
        }
        val dayStart = startOfDay(date)
        val diary = DiaryEntry(
            id = existingDiary?.id ?: java.util.UUID.randomUUID().toString(),
            dateStart = dayStart,
            content = content.trim(),
            backgroundMediaUri = backgroundUri,
            backgroundMediaType = backgroundUri?.let { inferDiaryMediaType(it) },
            createdAt = existingDiary?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val media = mediaItems.mapIndexed { index, item ->
            item.copy(
                diaryId = diary.id,
                mediaType = inferDiaryMediaType(item.mediaUri),
                sortOrder = index
            )
        }
        diaryRepository.saveDiary(diary, media)
        _selectedDiaryMedia.value = media
        backupManager.onDataChanged()
    }

    fun deleteDiary(diary: DiaryEntry) = viewModelScope.launch {
        diaryRepository.deleteDiary(diary)
        _selectedDiaryMedia.value = emptyList()
        backupManager.onDataChanged()
    }

    private fun startOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun inferDiaryMediaType(uri: String): DiaryMediaType {
        val videoExtensions = listOf(".mp4", ".mkv", ".webm", ".3gp", ".mov")
        return when {
            videoExtensions.any { uri.endsWith(it, ignoreCase = true) } -> DiaryMediaType.VIDEO
            else -> DiaryMediaType.IMAGE
        }
    }
}

class TodoViewModel(
    application: Application,
    private val eventRepository: EventRepository
) : AndroidViewModel(application) {

    val todoEvents = eventRepository.getTodoEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTodoStatus(event: Event) = viewModelScope.launch {
        val updated = event.copy(
            todoStatus = if (event.todoStatus == TodoStatus.PENDING) TodoStatus.COMPLETED else TodoStatus.PENDING
        )
        eventRepository.updateEvent(updated)
    }
}

class FriendViewModel(
    application: Application,
    private val friendRepository: FriendRepository,
    private val backupManager: BackupManager
) : AndroidViewModel(application) {

    val friends = friendRepository.getAllFriends()
        .map { list -> list.sortedWith(compareBy<Friend> { friendBirthdaySortBucket(it) }.thenBy { friendNextBirthdayDistance(it) }.thenBy { it.createdAt }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveFriend(existing: Friend?, name: String, birthdayDate: Long?) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@launch
        friendRepository.upsertFriend(
            Friend(
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                name = trimmed,
                avatarUri = existing?.avatarUri,
                birthdayDate = birthdayDate,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        )
        backupManager.onDataChanged()
    }

    fun deleteFriend(friend: Friend) = viewModelScope.launch {
        friendRepository.deleteFriend(friend)
        backupManager.onDataChanged()
    }
}

private fun friendBirthdaySortBucket(friend: Friend): Int {
    val distance = friendNextBirthdayDistance(friend)
    return when {
        distance == Int.MAX_VALUE -> 2
        distance <= 30 -> 0
        else -> 1
    }
}

private fun friendNextBirthdayDistance(friend: Friend): Int {
    val birthday = friend.birthdayDate ?: return Int.MAX_VALUE
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val source = Calendar.getInstance().apply { timeInMillis = birthday }
    val next = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        set(Calendar.MONTH, source.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH))
    }
    if (next.before(today)) next.add(Calendar.YEAR, 1)
    return TimeUnit.MILLISECONDS.toDays(next.timeInMillis - today.timeInMillis).toInt()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LogViewModel(
    application: Application,
    private val logRepository: LogRepository
) : AndroidViewModel(application) {

    private val _filter = MutableStateFlow("")

    val logs = _filter.flatMapLatest { filter ->
        if (filter.isEmpty()) logRepository.getRecentLogs(200)
        else logRepository.getLogsByOperation(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(f: String) { _filter.value = f }
}

class BackupViewModel(
    application: Application,
    private val logRepository: LogRepository,
    private val backupManager: BackupManager
) : AndroidViewModel(application) {

    data class OperationState(
        val inProgress: Boolean = false,
        val message: String? = null,
        val importRestored: Boolean = false,
        val importSummary: String? = null
    )

    private val _operationState = MutableStateFlow(OperationState())
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    init { backupManager.initialize() }

    fun saveBackupDirUri(uri: Uri) { backupManager.saveBackupDirUri(uri) }

    fun updateConfig(config: BackupConfig) {
        backupManager.updateConfig(config)
    }

    fun clearOperationMessage() {
        _operationState.value = _operationState.value.copy(message = null, importRestored = false)
    }

    fun triggerManualBackup(outputUri: Uri, password: String = "") = viewModelScope.launch {
        _operationState.value = OperationState(inProgress = true, message = "正在导出备份")
        runCatching {
            backupManager.performManualBackup(outputUri, password).getOrThrow()
        }.onSuccess { backupUri ->
            _operationState.value = OperationState(message = "备份导出成功：${backupUri?.lastPathSegment ?: "已保存"}")
            runCatching { logRepository.logBackupOperation("MANUAL", "success") }
        }.onFailure { e ->
            _operationState.value = OperationState(message = "备份导出失败：${e.message ?: "未知错误"}")
            runCatching { logRepository.logBackupOperation("MANUAL", "failed", e.message) }
        }
    }

    fun importBackup(uri: Uri, password: String = "") = viewModelScope.launch {
        _operationState.value = OperationState(inProgress = true, message = "正在导入备份")
        runCatching {
            backupManager.importBackup(uri, password).getOrThrow()
        }.onSuccess { result ->
            _operationState.value = OperationState(
                message = "备份导入成功：日子 ${result.events} 个，日记 ${result.diaries} 篇。",
                importRestored = true,
                importSummary = result.toSummary()
            )
            runCatching { logRepository.logBackupOperation("IMPORT", "success") }
        }.onFailure { e ->
            _operationState.value = OperationState(message = "备份导入失败：${e.message ?: "未知错误"}")
            runCatching { logRepository.logBackupOperation("IMPORT", "failed", e.message) }
        }
    }
}

private fun AndroidViewModel.runEventSideEffects(event: Event, notificationHelper: NotificationHelper, backupManager: BackupManager) {
    runCatching {
        if (event.reminderEnabled) {
            notificationHelper.scheduleReminder(event)
        }
    }.onFailure { Log.e("EventSideEffects", "Schedule reminder failed", it) }

    runCatching {
        if (event.calendarSyncEnabled && event.reminderEnabled) {
            SystemCalendarHelper(getApplication()).insertEvent(event)
        }
    }.onFailure { Log.e("EventSideEffects", "Sync system calendar failed", it) }

    runCatching {
        backupManager.onDataChanged()
    }.onFailure { Log.e("EventSideEffects", "Backup change tracking failed", it) }
}

class LabelViewModel(
    application: Application,
    private val labelRepository: LabelRepository,
    private val eventRepository: EventRepository
) : AndroidViewModel(application) {

    val labels = labelRepository.getAllLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createLabel(name: String, color: String = "#7C4DFF") = viewModelScope.launch {
        labelRepository.insertLabel(Label(name = name, color = color))
    }

    fun deleteLabel(label: Label) = viewModelScope.launch {
        labelRepository.deleteLabel(label)
    }

    fun addEventLabel(eventId: String, label: String) = viewModelScope.launch {
        labelRepository.addEventLabel(com.memoriabox.data.model.EventLabel(eventId, label))
    }

    fun removeEventLabel(eventId: String, label: String) = viewModelScope.launch {
        labelRepository.removeEventLabel(com.memoriabox.data.model.EventLabel(eventId, label))
    }
}

fun createMainViewModel(application: Application): MainViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return MainViewModel(
        application,
        com.memoriabox.repository.BoxRepository(app.database.boxDao()),
        EventRepository(app.database.eventDao()),
        LogRepository(app.database.logDao()),
        app.backupManager,
        NotificationHelper(application)
    )
}

fun createBoxDetailViewModel(application: Application): BoxDetailViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return BoxDetailViewModel(
        application,
        EventRepository(app.database.eventDao()),
        com.memoriabox.repository.BoxRepository(app.database.boxDao()),
        LogRepository(app.database.logDao()),
        app.backupManager,
        NotificationHelper(application)
    )
}

fun createCalendarViewModel(application: Application): CalendarViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return CalendarViewModel(
        application,
        EventRepository(app.database.eventDao()),
        DiaryRepository(app.database.diaryDao()),
        app.backupManager
    )
}

fun createTodoViewModel(application: Application): TodoViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return TodoViewModel(
        application,
        EventRepository(app.database.eventDao())
    )
}

fun createFriendViewModel(application: Application): FriendViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return FriendViewModel(application, FriendRepository(app.database.friendDao()), app.backupManager)
}

fun createLogViewModel(application: Application): LogViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return LogViewModel(application, LogRepository(app.database.logDao()))
}

fun createBackupViewModel(application: Application): BackupViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return BackupViewModel(application, LogRepository(app.database.logDao()), app.backupManager)
}

fun createLabelViewModel(application: Application): LabelViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return LabelViewModel(
        application,
        LabelRepository(app.database.labelDao()),
        EventRepository(app.database.eventDao())
    )
}
