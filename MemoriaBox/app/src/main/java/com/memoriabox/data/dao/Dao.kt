package com.memoriabox.data.dao

import androidx.room.*
import com.memoriabox.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BoxDao {
    @Query("SELECT * FROM boxes WHERE is_archived = 0 ORDER BY sort_order ASC, created_at ASC")
    fun getAllActiveBoxes(): Flow<List<Box>>

    @Query("SELECT * FROM boxes WHERE is_archived = 1 ORDER BY created_at DESC")
    fun getArchivedBoxes(): Flow<List<Box>>

    @Query("SELECT * FROM boxes WHERE id = :id")
    suspend fun getBoxById(id: String): Box?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBox(box: Box): Long

    @Update
    suspend fun updateBox(box: Box)

    @Delete
    suspend fun deleteBox(box: Box)

    @Query("UPDATE boxes SET is_archived = 1 WHERE id = :id")
    suspend fun archiveBox(id: String)

    @Query("UPDATE boxes SET is_archived = 0 WHERE id = :id")
    suspend fun restoreBox(id: String)

    @Query("UPDATE boxes SET sort_order = :order WHERE id = :id")
    suspend fun updateBoxOrder(id: String, order: Int)

    @Query("SELECT COUNT(*) FROM events WHERE box_id = :boxId AND id NOT LIKE 'milestone_%'")
    fun getEventCountByBoxId(boxId: String): Flow<Int>
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE box_id = :boxId AND id NOT LIKE 'milestone_%' ORDER BY is_pinned DESC, date ASC")
    fun getEventsByBoxId(boxId: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id NOT LIKE 'milestone_%' ORDER BY is_pinned DESC, date ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE type = 'TODO' AND id NOT LIKE 'milestone_%' ORDER BY due_date ASC")
    fun getTodoEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE type = 'BIRTHDAY' AND id NOT LIKE 'milestone_%' ORDER BY date ASC")
    fun getBirthdayEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("UPDATE events SET box_id = :newBoxId WHERE id = :id")
    suspend fun moveEventToBox(id: String, newBoxId: String)

    @Query("UPDATE events SET box_id = :newBoxId WHERE id IN (:ids)")
    suspend fun moveEventsToBox(ids: List<String>, newBoxId: String)

    @Query("DELETE FROM events WHERE id IN (:ids)")
    suspend fun deleteEventsByIds(ids: List<String>)

    @Query("SELECT * FROM events WHERE id IN (:ids)")
    suspend fun getEventsByIds(ids: List<String>): List<Event>

    @Query("UPDATE events SET is_pinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean)

    @Query("SELECT * FROM events WHERE date >= :now AND id NOT LIKE 'milestone_%' ORDER BY date ASC LIMIT 1")
    suspend fun getNextUpcomingEvent(now: Long): Event?

    @Query("SELECT COUNT(*) FROM events WHERE date >= :start AND date <= :end AND id NOT LIKE 'milestone_%'")
    suspend fun getEventCountBetween(start: Long, end: Long): Int
}

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels ORDER BY name ASC")
    fun getAllLabels(): Flow<List<Label>>

    @Query("SELECT * FROM labels WHERE name = :name")
    suspend fun getLabel(name: String): Label?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: Label): Long

    @Delete
    suspend fun deleteLabel(label: Label)

    @Query("SELECT label FROM event_labels WHERE event_id = :eventId")
    fun getEventLabels(eventId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addEventLabel(eventLabel: EventLabel)

    @Delete
    suspend fun removeEventLabel(eventLabel: EventLabel)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE operation LIKE :filter ORDER BY timestamp DESC")
    fun getLogsByOperation(filter: String): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntry): Long

    @Query("DELETE FROM logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldLogs(cutoffTimestamp: Long)

    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestLogs(count: Int)

    @Query("SELECT COUNT(*) FROM logs")
    suspend fun getLogCount(): Int
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY date_start DESC")
    fun getAllDiaries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date_start = :dateStart LIMIT 1")
    suspend fun getDiaryByDateStart(dateStart: Long): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE date_start BETWEEN :start AND :end ORDER BY date_start ASC")
    fun getDiariesBetween(start: Long, end: Long): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date_start BETWEEN :start AND :end ORDER BY date_start ASC, created_at ASC")
    suspend fun getDiariesBetweenOnce(start: Long, end: Long): List<DiaryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiary(diary: DiaryEntry)

    @Delete
    suspend fun deleteDiary(diary: DiaryEntry)

    @Query("SELECT * FROM diary_media WHERE diary_id = :diaryId ORDER BY sort_order ASC")
    fun getMediaForDiary(diaryId: String): Flow<List<DiaryMedia>>

    @Query("SELECT * FROM diary_media WHERE diary_id IN (:diaryIds) ORDER BY sort_order ASC")
    fun getMediaForDiaries(diaryIds: List<String>): Flow<List<DiaryMedia>>

    @Query("SELECT * FROM diary_media WHERE diary_id IN (:diaryIds) ORDER BY diary_id ASC, sort_order ASC")
    suspend fun getMediaForDiariesOnce(diaryIds: List<String>): List<DiaryMedia>

    @Query("SELECT * FROM diary_media WHERE diary_id = :diaryId ORDER BY sort_order ASC")
    suspend fun getMediaForDiaryOnce(diaryId: String): List<DiaryMedia>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedia(media: List<DiaryMedia>)

    @Query("DELETE FROM diary_media WHERE diary_id = :diaryId")
    suspend fun deleteMediaForDiary(diaryId: String)
}
