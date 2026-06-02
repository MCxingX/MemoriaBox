package com.memoriabox.database

import androidx.room.TypeConverter
import com.memoriabox.data.model.*

class Converters {
    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = EventType.valueOf(value)

    @TypeConverter
    fun fromBgType(value: BgType): String = value.name

    @TypeConverter
    fun toBgType(value: String): BgType = BgType.valueOf(value)

    @TypeConverter
    fun fromTodoStatus(value: TodoStatus): String = value.name

    @TypeConverter
    fun toTodoStatus(value: String): TodoStatus = TodoStatus.valueOf(value)
}
