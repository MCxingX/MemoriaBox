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
import com.memoriabox.utils.BackupManager
import com.memoriabox.utils.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class MainViewModel(
    application: Application,
    private val boxRepository: com.memoriabox.repository.BoxRepository,
    private val logRepository: LogRepository,
    private val backupManager: BackupManager
) : AndroidViewModel(application) {

    val boxes = boxRepository.getAllActiveBoxes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedBoxes = boxRepository.getArchivedBoxes()
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
}

class CalendarViewModel(
    application: Application,
    private val eventRepository: EventRepository
) : AndroidViewModel(application) {

    val allEvents = eventRepository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getEventsForDay(timestamp: Long, events: List<Event>): List<Event> {
        val dayStart = (timestamp / 86400000) * 86400000
        val dayEnd = dayStart + 86400000 - 1
        return events.filter { it.date >= dayStart && it.date <= dayEnd }
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
    private val logRepository: LogRepository
) : AndroidViewModel(application) {

    val friends = friendRepository.getAllFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFriend(name: String, avatarUri: String? = null, birthdayDate: Long? = null) = viewModelScope.launch {
        val friend = Friend(name = name, avatarUri = avatarUri, birthdayDate = birthdayDate)
        friendRepository.insertFriend(friend)
    }

    fun updateFriend(friend: Friend) = viewModelScope.launch {
        friendRepository.updateFriend(friend)
    }

    fun deleteFriend(friend: Friend) = viewModelScope.launch {
        friendRepository.deleteFriend(friend)
    }

    fun addLabel(friendId: String, label: String) = viewModelScope.launch {
        friendRepository.addRelation(FriendRelation(friendId, label))
    }

    fun removeLabel(friendId: String, label: String) = viewModelScope.launch {
        friendRepository.removeRelation(FriendRelation(friendId, label))
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

fun createMainViewModel(application: Application): MainViewModel {
    val app = application as com.memoriabox.MemoriaApp
    return MainViewModel(
        application,
        com.memoriabox.repository.BoxRepository(app.database.boxDao()),
        LogRepository(app.database.logDao()),
        app.backupManager
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
        EventRepository(app.database.eventDao())
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
        LogRepository(app.database.logDao())
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
