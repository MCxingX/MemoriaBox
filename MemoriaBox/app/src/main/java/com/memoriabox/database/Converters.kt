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
    fun fromRepeatMode(value: RepeatMode): String = value.name

    @TypeConverter
    fun toRepeatMode(value: String): RepeatMode = runCatching {
        RepeatMode.valueOf(value)
    }.getOrDefault(RepeatMode.NONE)
}
