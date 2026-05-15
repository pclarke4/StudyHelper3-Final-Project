package com.zybooks.studyhelper3.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "text")
    var title: String,

    var description: String = "",

    var category: String = "General",

    @ColumnInfo(name = "updated")
    var updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_completed")
    var isCompleted: Boolean = false,

    @ColumnInfo(name = "time_spent_ms")
    var timeSpentMs: Long = 0,

    var priority: Priority = Priority.MEDIUM,

    @ColumnInfo(name = "due_date")
    var dueDate: String? = null, // Stored as yyyy-MM-dd

    @ColumnInfo(name = "completion_date")
    var completionDate: Long? = null,

    var difficulty: Int = 3,

    @ColumnInfo(name = "is_recurring")
    var isRecurring: Boolean = false,

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis()
)
