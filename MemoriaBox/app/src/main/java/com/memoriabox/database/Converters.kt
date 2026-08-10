package com.memoriabox.database

import androidx.room.TypeConverter
import com.memoriabox.data.model.*

class Converters {
    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = runCatching {
        EventType.valueOf(value)
    }.getOrDefault(EventType.COUNTDOWN)

    @TypeConverter
    fun fromBgType(value: BgType): String = value.name

    @TypeConverter
    fun toBgType(value: String): BgType = runCatching {
        BgType.valueOf(value)
    }.getOrDefault(BgType.COLOR)

    @TypeConverter
    fun fromTodoStatus(value: TodoStatus): String = value.name

    @TypeConverter
    fun toTodoStatus(value: String): TodoStatus = runCatching {
        TodoStatus.valueOf(value)
    }.getOrDefault(TodoStatus.PENDING)

    @TypeConverter
    fun fromTodoPriority(value: TodoPriority): String = value.name

    @TypeConverter
    fun toTodoPriority(value: String): TodoPriority = runCatching {
        TodoPriority.valueOf(value)
    }.getOrDefault(TodoPriority.MEDIUM)

    @TypeConverter
    fun fromGiftStatus(value: GiftStatus): String = value.name

    @TypeConverter
    fun toGiftStatus(value: String): GiftStatus = runCatching {
        GiftStatus.valueOf(value)
    }.getOrDefault(GiftStatus.PLANNED)

    @TypeConverter
    fun fromRepeatMode(value: RepeatMode): String = value.name

    @TypeConverter
    fun toRepeatMode(value: String): RepeatMode = runCatching {
        RepeatMode.valueOf(value)
    }.getOrDefault(RepeatMode.NONE)

    @TypeConverter
    fun fromDiaryMediaType(value: DiaryMediaType?): String? = value?.name

    @TypeConverter
    fun toDiaryMediaType(value: String?): DiaryMediaType? = value?.let {
        runCatching { DiaryMediaType.valueOf(it) }.getOrNull()
    }
}
