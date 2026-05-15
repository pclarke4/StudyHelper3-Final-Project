package com.zybooks.studyhelper3.repo

import androidx.lifecycle.LiveData
import androidx.room.*
import com.zybooks.studyhelper3.model.Question

@Dao
interface QuestionDao {
   @Query("SELECT * FROM Question WHERE id = :id")
   fun getQuestion(id: Long): LiveData<Question?>

   @Query("SELECT * FROM Question WHERE subject_id = :subjectId ORDER BY id")
   fun getQuestions(subjectId: Long): LiveData<List<Question>>

   @Insert(onConflict = OnConflictStrategy.REPLACE)
   suspend fun addQuestion(question: Question): Long

   @Update
   suspend fun updateQuestion(question: Question)

   @Delete
   suspend fun deleteQuestion(question: Question)
}