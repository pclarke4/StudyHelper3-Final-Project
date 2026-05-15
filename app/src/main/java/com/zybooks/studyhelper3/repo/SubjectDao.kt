package com.zybooks.studyhelper3.repo

import androidx.lifecycle.LiveData
import androidx.room.*
import com.zybooks.studyhelper3.model.Subject

@Dao
interface SubjectDao {
   @Query("SELECT * FROM subjects WHERE id = :id")
   fun getSubject(id: Long): LiveData<Subject?>

   @Query("SELECT * FROM subjects ORDER BY text COLLATE NOCASE")
   fun getSubjects(): LiveData<List<Subject>>

   @Query("SELECT * FROM subjects WHERE is_completed = 0 ORDER BY updated DESC")
   fun getActiveSubjects(): LiveData<List<Subject>>

   @Query("SELECT * FROM subjects WHERE is_completed = 1 ORDER BY updated DESC")
   fun getCompletedSubjects(): LiveData<List<Subject>>

   @Query("SELECT * FROM subjects ORDER BY updated DESC")
   fun getSubjectsNewestFirst(): LiveData<List<Subject>>

   @Query("SELECT * FROM subjects ORDER BY updated ASC")
   fun getSubjectsOldestFirst(): LiveData<List<Subject>>
   
   @Query("SELECT * FROM subjects ORDER BY priority DESC, updated DESC")
   fun getSubjectsByPriority(): LiveData<List<Subject>>

   @Query("SELECT * FROM subjects ORDER BY difficulty DESC, updated DESC")
   fun getSubjectsByDifficulty(): LiveData<List<Subject>>

   @Query("SELECT * FROM subjects WHERE text LIKE :searchQuery ORDER BY updated DESC")
   fun searchSubjects(searchQuery: String): LiveData<List<Subject>>

   @Query("SELECT * FROM subjects WHERE is_completed = 0 AND due_date IS NOT NULL ORDER BY due_date ASC")
   fun getUpcomingSubjects(): LiveData<List<Subject>>

   @Query("SELECT COUNT(*) FROM subjects")
   fun getTotalSubjectCount(): LiveData<Int>

   @Query("SELECT COUNT(*) FROM subjects WHERE is_completed = 1")
   fun getCompletedSubjectCount(): LiveData<Int>

   @Query("SELECT SUM(time_spent_ms) FROM subjects")
   fun getTotalTimeSpent(): LiveData<Long?>

   @Query("SELECT * FROM subjects WHERE due_date = :date")
   fun getSubjectsForDate(date: String): LiveData<List<Subject>>

   @Query("SELECT completion_date FROM subjects WHERE is_completed = 1 AND completion_date IS NOT NULL ORDER BY completion_date DESC")
   suspend fun getCompletionDates(): List<Long>

   @Query("SELECT * FROM subjects")
   suspend fun getSubjectsSync(): List<Subject>

   @Insert(onConflict = OnConflictStrategy.REPLACE)
   suspend fun addSubject(subject: Subject): Long

   @Update
   suspend fun updateSubject(subject: Subject)

   @Delete
   suspend fun deleteSubject(subject: Subject)
}