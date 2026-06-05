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
import com.memoriabox.repository.LogRepository
import com.memoriabox.repository.FriendRepository
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
            if (targetEvent.reminderEnabled) {
                notificationHelper.scheduleReminder(targetEvent)
            }
            syncSystemCalendarIfNeeded(targetEvent)
            syncAnniversaryMilestones(targetEvent, eventRepository, notificationHelper)
            logRepository.logEventOperation("QUICK_CREATE", targetEvent.id, targetEvent.name)
            backupManager.onDataChanged()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Quick create event failed", e)
        }
    }

    fun updateQuickEvent(event: Event) = viewModelScope.launch {
        try {
            eventRepository.updateEvent(event)
            notificationHelper.cancelReminder(event)
            if (event.reminderEnabled) {
                notificationHelper.scheduleReminder(event)
            }
            syncSystemCalendarIfNeeded(event)
            syncAnniversaryMilestones(event, eventRepository, notificationHelper)
            logRepository.logEventOperation("QUICK_UPDATE", event.id, event.name)
            backupManager.onDataChanged()
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
    private val _layoutMode = MutableStateFlow(CardLayoutMode.GRID_2X4)

    val events = _events.asStateFlow()
    val box = _box.asStateFlow()
    val layoutMode = _layoutMode.asStateFlow()

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

    fun setLayoutMode(mode: CardLayoutMode) {
        _layoutMode.value = mode
    }

    fun createEvent(event: Event) = viewModelScope.launch {
        try {
            eventRepository.insertEvent(event)
            if (event.reminderEnabled) {
                notificationHelper.scheduleReminder(event)
            }
            syncSystemCalendarIfNeeded(event)
            syncAnniversaryMilestones(event, eventRepository, notificationHelper)
            logRepository.logEventOperation("CREATE", event.id, event.name)
            backupManager.onDataChanged()
        } catch (e: Exception) {
            Log.e("BoxDetailVM", "Create event failed", e)
        }
    }

    fun updateEvent(event: Event) = viewModelScope.launch {
        try {
            eventRepository.updateEvent(event)
            notificationHelper.cancelReminder(event)
            if (event.reminderEnabled) {
                notificationHelper.scheduleReminder(event)
            }
            syncSystemCalendarIfNeeded(event)
            syncAnniversaryMilestones(event, eventRepository, notificationHelper)
            logRepository.logEventOperation("UPDATE", event.id, event.name)
            backupManager.onDataChanged()
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
    private val diaryRepository: DiaryRepository
) : AndroidViewModel(application) {

    val allEvents = eventRepository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDiaries = diaryRepository.getAllDiaries()
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
    }

    fun deleteDiary(diary: DiaryEntry) = viewModelScope.launch {
        diaryRepository.deleteDiary(diary)
        _selectedDiaryMedia.value = emptyList()
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

    init { backupManager.initialize() }

    fun saveBackupDirUri(uri: Uri) { backupManager.saveBackupDirUri(uri) }

    fun updateConfig(config: BackupConfig) {
        backupManager.updateConfig(config)
    }

    fun triggerManualBackup(outputUri: Uri, password: String = "") = viewModelScope.launch {
        try {
            backupManager.performManualBackup(outputUri, password)
            logRepository.logBackupOperation("MANUAL", "success")
        } catch (e: Exception) {
            logRepository.logBackupOperation("MANUAL", "failed", e.message)
        }
    }

    fun importBackup(uri: Uri, password: String = "") = viewModelScope.launch {
        try {
            backupManager.importBackup(uri, password)
            logRepository.logBackupOperation("IMPORT", "success")
        } catch (e: Exception) {
            logRepository.logBackupOperation("IMPORT", "failed", e.message)
        }
    }
}

class FriendViewModel(
    application: Application,
    private val friendRepository: FriendRepository,
    private val eventRepository: EventRepository,
    private val boxRepository: com.memoriabox.repository.BoxRepository,
    private val logRepository: LogRepository,
    private val notificationHelper: NotificationHelper
) : AndroidViewModel(application) {

    val friends = friendRepository.getAllFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendRelations = friendRepository.getAllFriendRelations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFriend(name: String, avatarUri: String? = null, birthdayDate: Long? = null, labels: List<String> = emptyList()) = viewModelScope.launch {
        val friend = Friend(name = name, avatarUri = avatarUri, birthdayDate = birthdayDate)
        friendRepository.insertFriend(friend)
        labels.distinct().forEach { label ->
            friendRepository.addRelation(FriendRelation(friend.id, label))
        }
        syncFriendBirthdayEvent(friend)
    }

    fun updateFriend(friend: Friend) = viewModelScope.launch {
        friendRepository.updateFriend(friend)
        syncFriendBirthdayEvent(friend)
    }

    fun updateFriend(friend: Friend, labels: List<String>, previousLabels: List<String>) = viewModelScope.launch {
        friendRepository.updateFriend(friend)
        previousLabels.distinct().forEach { label ->
            friendRepository.removeRelation(FriendRelation(friend.id, label))
        }
        labels.distinct().forEach { label ->
            friendRepository.addRelation(FriendRelation(friend.id, label))
        }
        syncFriendBirthdayEvent(friend)
    }

    fun deleteFriend(friend: Friend) = viewModelScope.launch {
        deleteFriendBirthdayEvent(friend)
        friendRepository.deleteFriend(friend)
    }

    fun addLabel(friendId: String, label: String) = viewModelScope.launch {
        friendRepository.addRelation(FriendRelation(friendId, label))
    }

    fun removeLabel(friendId: String, label: String) = viewModelScope.launch {
        friendRepository.removeRelation(FriendRelation(friendId, label))
    }

    private suspend fun syncFriendBirthdayEvent(friend: Friend) {
        val birthdayEventId = friendBirthdayEventId(friend.id)
        val birthdayDate = friend.birthdayDate
        if (birthdayDate == null) {
            deleteFriendBirthdayEvent(friend)
            return
        }

        val defaultBox = boxRepository.getBoxById("default_1") ?: Box(
            id = "default_1",
            name = "我的日子",
            icon = "*",
            bgType = BgType.COLOR,
            bgValue = "#1677FF"
        ).also { boxRepository.insertBox(it) }

        val existingEvent = eventRepository.getEventById(birthdayEventId)
        val event = Event(
            id = birthdayEventId,
            boxId = existingEvent?.boxId ?: defaultBox.id,
            name = "${friend.name}的生日",
            date = birthdayDate,
            type = EventType.BIRTHDAY,
            note = "由好友资料自动生成",
            reminderEnabled = true,
            reminderDays = existingEvent?.reminderDays ?: 7,
            alarmEnabled = existingEvent?.alarmEnabled ?: false,
            alarmTime = existingEvent?.alarmTime ?: "09:00",
            avatarUri = friend.avatarUri ?: existingEvent?.avatarUri,
            pushPlusEnabled = existingEvent?.pushPlusEnabled ?: false,
            repeatMode = RepeatMode.YEARLY,
            repeatInterval = 1,
            reminderOffsets = existingEvent?.reminderOffsets ?: "0,1,7",
            gradientStart = existingEvent?.gradientStart ?: "#1677FF",
            gradientEnd = existingEvent?.gradientEnd ?: "#13C2C2",
            textColor = existingEvent?.textColor ?: "#FFFFFF",
            cardTemplate = existingEvent?.cardTemplate ?: "HERO",
            displayFields = existingEvent?.displayFields ?: "date,note,reminder",
            isBirthday = true,
            repeatYearly = true,
            createdAt = existingEvent?.createdAt ?: System.currentTimeMillis()
        )
        eventRepository.insertEvent(event)
        notificationHelper.cancelReminder(event)
        notificationHelper.scheduleReminder(event)
        logRepository.logEventOperation("FRIEND_BIRTHDAY_SYNC", event.id, event.name)
    }

    private suspend fun deleteFriendBirthdayEvent(friend: Friend) {
        val existingEvent = eventRepository.getEventById(friendBirthdayEventId(friend.id)) ?: return
        notificationHelper.cancelReminder(existingEvent)
        eventRepository.deleteEvent(existingEvent)
        logRepository.logEventOperation("FRIEND_BIRTHDAY_DELETE", existingEvent.id, existingEvent.name)
    }

    private fun friendBirthdayEventId(friendId: String): String = "friend_birthday_$friendId"
}

private fun AndroidViewModel.syncSystemCalendarIfNeeded(event: Event) {
    if (event.calendarSyncEnabled && event.reminderEnabled) {
        SystemCalendarHelper(getApplication()).insertEvent(event)
    }
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

private suspend fun syncAnniversaryMilestones(
    event: Event,
    eventRepository: EventRepository,
    notificationHelper: NotificationHelper
) {
    val milestoneSpecs = listOf(
        "100d" to "100天",
        "520d" to "520天",
        "666d" to "666天",
        "999d" to "999天",
        "1y" to "1周年",
        "2y" to "2周年",
        "3y" to "3周年",
        "5y" to "5周年",
        "10y" to "10周年"
    )
    if (event.id.startsWith("milestone_")) return
    if (event.type != EventType.ANNIVERSARY) {
        milestoneSpecs.forEach { (key, _) ->
            eventRepository.getEventById(milestoneEventId(event.id, key))?.let { milestone ->
                notificationHelper.cancelReminder(milestone)
                eventRepository.deleteEvent(milestone)
            }
        }
        return
    }

    milestoneSpecs.forEach { (key, label) ->
        val date = milestoneDate(event.date, key)
        val existing = eventRepository.getEventById(milestoneEventId(event.id, key))
        val milestone = Event(
            id = milestoneEventId(event.id, key),
            boxId = event.boxId,
            name = "${event.name} $label",
            date = date,
            type = EventType.ANNIVERSARY,
            note = "由“${event.name}”自动生成",
            reminderEnabled = true,
            reminderDays = existing?.reminderDays ?: 7,
            alarmTime = existing?.alarmTime ?: event.alarmTime,
            avatarUri = event.avatarUri,
            pushPlusEnabled = existing?.pushPlusEnabled ?: event.pushPlusEnabled,
            repeatMode = RepeatMode.NONE,
            reminderOffsets = existing?.reminderOffsets ?: "0,7",
            gradientStart = existing?.gradientStart ?: event.gradientStart,
            gradientEnd = existing?.gradientEnd ?: event.gradientEnd,
            textColor = existing?.textColor ?: event.textColor,
            cardTemplate = existing?.cardTemplate ?: event.cardTemplate,
            displayFields = existing?.displayFields ?: event.displayFields,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        eventRepository.insertEvent(milestone)
        notificationHelper.cancelReminder(milestone)
        notificationHelper.scheduleReminder(milestone)
    }
}

private fun milestoneEventId(sourceEventId: String, key: String): String = "milestone_${sourceEventId}_$key"

private fun milestoneDate(sourceDate: Long, key: String): Long {
    return Calendar.getInstance().apply {
        timeInMillis = sourceDate
        when (key) {
            "100d" -> add(Calendar.DAY_OF_YEAR, 100)
            "520d" -> add(Calendar.DAY_OF_YEAR, 520)
            "666d" -> add(Calendar.DAY_OF_YEAR, 666)
            "999d" -> add(Calendar.DAY_OF_YEAR, 999)
            "1y" -> add(Calendar.YEAR, 1)
            "2y" -> add(Calendar.YEAR, 2)
            "3y" -> add(Calendar.YEAR, 3)
            "5y" -> add(Calendar.YEAR, 5)
            "10y" -> add(Calendar.YEAR, 10)
        }
    }.timeInMillis
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
        DiaryRepository(app.database.diaryDao())
    )
}

fun createTodoViewModel(application: Application): TodoViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return TodoViewModel(
        application,
        EventRepository(app.database.eventDao())
    )
}

fun createLogViewModel(application: Application): LogViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return LogViewModel(application, LogRepository(app.database.logDao()))
}

fun createBackupViewModel(application: Application): BackupViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return BackupViewModel(application, LogRepository(app.database.logDao()), app.backupManager)
}

fun createFriendViewModel(application: Application): FriendViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return FriendViewModel(
        application,
        FriendRepository(app.database.friendDao(), app.database.labelDao()),
        EventRepository(app.database.eventDao()),
        com.memoriabox.repository.BoxRepository(app.database.boxDao()),
        LogRepository(app.database.logDao()),
        NotificationHelper(application)
    )
}

fun createLabelViewModel(application: Application): LabelViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return LabelViewModel(
        application,
        LabelRepository(app.database.labelDao()),
        EventRepository(app.database.eventDao())
    )
}
