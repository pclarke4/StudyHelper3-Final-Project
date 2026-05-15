package com.zybooks.studyhelper3.repo

import androidx.room.TypeConverter
import com.zybooks.studyhelper3.model.Priority

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(priority: String): Priority {
        return try {
            Priority.valueOf(priority)
        } catch (e: Exception) {
            Priority.MEDIUM
        }
    }
}
