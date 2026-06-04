package com.memoriabox.repository

import com.memoriabox.data.dao.BoxDao
import com.memoriabox.data.dao.EventDao
import com.memoriabox.data.dao.FriendDao
import com.memoriabox.data.dao.LabelDao
import com.memoriabox.data.dao.LogDao
import com.memoriabox.data.dao.DiaryDao
import com.memoriabox.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BoxRepository(private val boxDao: BoxDao) {
    fun getAllActiveBoxes(): Flow<List<Box>> = boxDao.getAllActiveBoxes()
    fun getArchivedBoxes(): Flow<List<Box>> = boxDao.getArchivedBoxes()
    suspend fun getBoxById(id: String): Box? = boxDao.getBoxById(id)
    suspend fun insertBox(box: Box): Long = boxDao.insertBox(box)
    suspend fun updateBox(box: Box) = boxDao.updateBox(box)
    suspend fun deleteBox(box: Box) = boxDao.deleteBox(box)
    suspend fun archiveBox(id: String) = boxDao.archiveBox(id)
    suspend fun restoreBox(id: String) = boxDao.restoreBox(id)
    suspend fun updateBoxOrder(id: String, order: Int) = boxDao.updateBoxOrder(id, order)
    fun getEventCountByBoxId(boxId: String): Flow<Int> = boxDao.getEventCountByBoxId(boxId)
}

class EventRepository(private val eventDao: EventDao) {
    fun getEventsByBoxId(boxId: String): Flow<List<Event>> = eventDao.getEventsByBoxId(boxId)
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()
    fun getTodoEvents(): Flow<List<Event>> = eventDao.getTodoEvents()
    fun getBirthdayEvents(): Flow<List<Event>> = eventDao.getBirthdayEvents()
    suspend fun getEventById(id: String): Event? = eventDao.getEventById(id)
    suspend fun insertEvent(event: Event): Long = eventDao.insertEvent(event)
    suspend fun updateEvent(event: Event) = eventDao.updateEvent(event)
    suspend fun deleteEvent(event: Event) = eventDao.deleteEvent(event)
    suspend fun moveEventToBox(id: String, newBoxId: String) = eventDao.moveEventToBox(id, newBoxId)
    suspend fun moveEventsToBox(ids: List<String>, newBoxId: String) = eventDao.moveEventsToBox(ids, newBoxId)
    suspend fun deleteEventsByIds(ids: List<String>) = eventDao.deleteEventsByIds(ids)
    suspend fun getEventsByIds(ids: List<String>): List<Event> = eventDao.getEventsByIds(ids)
    suspend fun updatePinned(id: String, isPinned: Boolean) = eventDao.updatePinned(id, isPinned)
}

class LogRepository(private val logDao: LogDao) {
    fun getRecentLogs(limit: Int = 100): Flow<List<LogEntry>> = logDao.getRecentLogs(limit)
    fun getAllLogs(): Flow<List<LogEntry>> = logDao.getAllLogs()
    fun getLogsByOperation(filter: String): Flow<List<LogEntry>> = logDao.getLogsByOperation("%$filter%")

    suspend fun logOperation(
        operation: String,
        targetId: String,
        targetName: String,
        result: String = "success",
        extra: String? = null
    ) {
        val log = LogEntry(
            operation = operation,
            targetId = targetId,
            targetName = targetName,
            result = result,
            extra = extra
        )
        logDao.insertLog(log)
        val count = logDao.getLogCount()
        if (count > 2000) {
            logDao.deleteOldestLogs(count - 2000)
        }
    }

    suspend fun logBoxOperation(operation: String, boxId: String, boxName: String, result: String = "success") {
        logOperation("BOX_$operation", boxId, boxName, result)
    }

    suspend fun logEventOperation(operation: String, eventId: String, eventName: String, result: String = "success") {
        logOperation("EVENT_$operation", eventId, eventName, result)
    }

    suspend fun logBackupOperation(operation: String, result: String = "success", extra: String? = null) {
        logOperation("BACKUP_$operation", "backup", "Backup", result, extra)
    }
}

class FriendRepository(private val friendDao: FriendDao, private val labelDao: LabelDao) {
    fun getAllFriends(): Flow<List<Friend>> = friendDao.getAllFriends()
    suspend fun getFriendById(id: String): Friend? = friendDao.getFriendById(id)
    suspend fun insertFriend(friend: Friend): Long = friendDao.insertFriend(friend)
    suspend fun updateFriend(friend: Friend) = friendDao.updateFriend(friend)
    suspend fun deleteFriend(friend: Friend) = friendDao.deleteFriend(friend)
    suspend fun addRelation(relation: FriendRelation) = labelDao.addFriendRelation(relation)
    suspend fun removeRelation(relation: FriendRelation) = labelDao.removeFriendRelation(relation)
    fun getFriendsByLabel(label: String): Flow<List<String>> = labelDao.getFriendsByLabel(label)
    fun getAllFriendRelations(): Flow<List<FriendRelation>> = labelDao.getAllFriendRelations()
}

class LabelRepository(private val labelDao: LabelDao) {
    fun getAllLabels(): Flow<List<Label>> = labelDao.getAllLabels()
    suspend fun getLabel(name: String): Label? = labelDao.getLabel(name)
    suspend fun insertLabel(label: Label): Long = labelDao.insertLabel(label)
    suspend fun deleteLabel(label: Label) = labelDao.deleteLabel(label)
    fun getEventLabels(eventId: String): Flow<List<String>> = labelDao.getEventLabels(eventId)
    suspend fun addEventLabel(eventLabel: EventLabel) = labelDao.addEventLabel(eventLabel)
    suspend fun removeEventLabel(eventLabel: EventLabel) = labelDao.removeEventLabel(eventLabel)
}

class DiaryRepository(private val diaryDao: DiaryDao) {
    fun getAllDiaries(): Flow<List<DiaryEntry>> = diaryDao.getAllDiaries()
    fun getDiariesBetween(start: Long, end: Long): Flow<List<DiaryEntry>> = diaryDao.getDiariesBetween(start, end)
    fun getMediaForDiary(diaryId: String): Flow<List<DiaryMedia>> = diaryDao.getMediaForDiary(diaryId)
    fun getMediaForDiaries(diaryIds: List<String>): Flow<List<DiaryMedia>> = diaryDao.getMediaForDiaries(diaryIds)
    suspend fun getDiaryByDateStart(dateStart: Long): DiaryEntry? = diaryDao.getDiaryByDateStart(dateStart)
    suspend fun getMediaForDiaryOnce(diaryId: String): List<DiaryMedia> = diaryDao.getMediaForDiaryOnce(diaryId)

    suspend fun saveDiary(diary: DiaryEntry, media: List<DiaryMedia>) {
        diaryDao.upsertDiary(diary)
        diaryDao.deleteMediaForDiary(diary.id)
        if (media.isNotEmpty()) {
            diaryDao.upsertMedia(media.mapIndexed { index, item -> item.copy(diaryId = diary.id, sortOrder = index) })
        }
    }

    suspend fun deleteDiary(diary: DiaryEntry) {
        diaryDao.deleteMediaForDiary(diary.id)
        diaryDao.deleteDiary(diary)
    }
}
