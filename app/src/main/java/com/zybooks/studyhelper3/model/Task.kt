package com.zybooks.studyhelper3.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    var title: String = "",

    var description: String = "",

    var category: String = "General",

    var priority: Priority = Priority.MEDIUM,

    var difficulty: Int = 3,

    @ColumnInfo(name = "due_date")
    var dueDate: Long? = null,

    @ColumnInfo(name = "is_completed")
    var isCompleted: Boolean = false,

    @ColumnInfo(name = "is_recurring")
    var isRecurring: Boolean = false,

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "time_spent_ms")
    var timeSpentMs: Long = 0,

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = System.currentTimeMillis()
)
