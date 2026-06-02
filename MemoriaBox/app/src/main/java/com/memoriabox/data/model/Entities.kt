package com.memoriabox.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventType {
    COUNTDOWN, ANNIVERSARY, ELAPSED, BIRTHDAY, TODO
}

enum class BgType {
    COLOR, IMAGE
}

enum class CardLayoutMode {
    SINGLE_COLUMN, GRID_2X4, GRID_3X3, FLOW
}

enum class ImportMode {
    OVERWRITE, MERGE_SKIP, MERGE_REPLACE
}

enum class TodoStatus {
    PENDING, COMPLETED, CANCELLED
}

@Entity(tableName = "boxes")
data class Box(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "\uD83D\uDCE6",
    @ColumnInfo(name = "bg_type")
    val bgType: BgType = BgType.COLOR,
    @ColumnInfo(name = "bg_value")
    val bgValue: String = "#7C4DFF",
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "events")
data class Event(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(name = "box_id", index = true)
    val boxId: String,
    val name: String,
    val date: Long,
    val lunar: String? = null,
    val type: EventType = EventType.COUNTDOWN,
    val note: String = "",
    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean = false,
    @ColumnInfo(name = "reminder_days")
    val reminderDays: Int = 1,
    @ColumnInfo(name = "alarm_enabled")
    val alarmEnabled: Boolean = false,
    @ColumnInfo(name = "alarm_time")
    val alarmTime: String = "09:00",
    @ColumnInfo(name = "card_style_json")
    val cardStyleJson: String? = null,
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String? = null,
    @ColumnInfo(name = "is_birthday")
    val isBirthday: Boolean = false,
    @ColumnInfo(name = "repeat_yearly")
    val repeatYearly: Boolean = false,
    @ColumnInfo(name = "todo_status")
    val todoStatus: TodoStatus = TodoStatus.PENDING,
    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String? = null,
    @ColumnInfo(name = "birthday_date")
    val birthdayDate: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "friend_relations", primaryKeys = ["friend_id", "label"])
data class FriendRelation(
    @ColumnInfo(name = "friend_id")
    val friendId: String,
    val label: String
)

@Entity(tableName = "event_labels", primaryKeys = ["event_id", "label"])
data class EventLabel(
    @ColumnInfo(name = "event_id")
    val eventId: String,
    val label: String
)

@Entity(tableName = "labels")
data class Label(
    @PrimaryKey
    val name: String,
    val color: String = "#7C4DFF",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String,
    @ColumnInfo(name = "target_id")
    val targetId: String,
    @ColumnInfo(name = "target_name")
    val targetName: String,
    val result: String = "success",
    val extra: String? = null
)

data class CardStyle(
    val backgroundColor: String? = null,
    val backgroundImage: String? = null,
    val textColor: String? = "#000000",
    val cornerRadius: Float = 16f,
    val shadowSize: Float = 4f,
    val borderColor: String? = null,
    val showLunar: Boolean = true,
    val showNote: Boolean = true
)

data class PushPlusConfig(
    val token: String = "",
    val enabled: Boolean = false,
    val channel: String = "wechat"
)

data class BackupConfig(
    val autoBackupDelay: Long = 20000L,
    val maxAutoBackups: Int = 5,
    val maxLogEntries: Int = 2000,
    val backupPassword: String = "",
    val backupDirUri: String = ""
)
