package com.zybooks.studyhelper3.repo

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zybooks.studyhelper3.model.Question
import com.zybooks.studyhelper3.model.Subject

@Database(entities = [Question::class, Subject::class], version = 3)
@TypeConverters(Converters::class)
abstract class StudyDatabase : RoomDatabase() {

   abstract fun questionDao(): QuestionDao
   abstract fun subjectDao(): SubjectDao
}
