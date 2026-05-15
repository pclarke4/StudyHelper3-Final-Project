package com.zybooks.studyhelper3.repo

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.zybooks.studyhelper3.model.Question
import com.zybooks.studyhelper3.model.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudyRepository private constructor(context: Context) {

   companion object {
      private var instance: StudyRepository? = null

      fun getInstance(context: Context): StudyRepository {
         if (instance == null) {
            instance = StudyRepository(context)
         }
         return instance!!
      }
   }

   private val database : StudyDatabase = Room.databaseBuilder(
      context.applicationContext,
      StudyDatabase::class.java,
      "study.db"
   )
      .fallbackToDestructiveMigration()
      .build()

   private val subjectDao = database.subjectDao()
   private val questionDao = database.questionDao()

   init {
      CoroutineScope(Dispatchers.IO).launch {
         val subjects = subjectDao.getSubjectsSync()
         if (subjects.isEmpty()) {
            addStarterData()
         }
      }
   }

   fun getSubject(subjectId: Long): LiveData<Subject?> = subjectDao.getSubject(subjectId)

   fun getSubjects(): LiveData<List<Subject>> = subjectDao.getSubjects()

   fun getActiveSubjects(): LiveData<List<Subject>> = subjectDao.getActiveSubjects()

   fun getCompletedSubjects(): LiveData<List<Subject>> = subjectDao.getCompletedSubjects()

   fun getSubjectsNewestFirst(): LiveData<List<Subject>> = subjectDao.getSubjectsNewestFirst()

   fun getSubjectsOldestFirst(): LiveData<List<Subject>> = subjectDao.getSubjectsOldestFirst()
   
   fun getSubjectsByPriority(): LiveData<List<Subject>> = subjectDao.getSubjectsByPriority()

   fun getSubjectsByDifficulty(): LiveData<List<Subject>> = subjectDao.getSubjectsByDifficulty()

   fun searchSubjects(query: String): LiveData<List<Subject>> = subjectDao.searchSubjects("%$query%")

   fun getTotalSubjectCount(): LiveData<Int> = subjectDao.getTotalSubjectCount()

   fun getCompletedSubjectCount(): LiveData<Int> = subjectDao.getCompletedSubjectCount()

   fun getTotalTimeSpent(): LiveData<Long?> = subjectDao.getTotalTimeSpent()

   fun getSubjectsForDate(date: String): LiveData<List<Subject>> = 
      subjectDao.getSubjectsForDate(date)

   suspend fun getCompletionDates(): List<Long> = subjectDao.getCompletionDates()

   suspend fun addSubject(subject: Subject): Long {
      return withContext(Dispatchers.IO) {
         val id = subjectDao.addSubject(subject)
         subject.id = id
         id
      }
   }

   suspend fun updateSubject(subject: Subject) {
      withContext(Dispatchers.IO) {
         subjectDao.updateSubject(subject)
      }
   }

   suspend fun deleteSubject(subject: Subject) {
      withContext(Dispatchers.IO) {
         subjectDao.deleteSubject(subject)
      }
   }

   fun getQuestion(questionId: Long): LiveData<Question?> = questionDao.getQuestion(questionId)

   fun getQuestions(subjectId: Long): LiveData<List<Question>> = questionDao.getQuestions(subjectId)

   suspend fun addQuestion(question: Question): Long {
      return withContext(Dispatchers.IO) {
         val id = questionDao.addQuestion(question)
         question.id = id
         id
      }
   }

   suspend fun updateQuestion(question: Question) {
      withContext(Dispatchers.IO) {
         questionDao.updateQuestion(question)
      }
   }

   suspend fun deleteQuestion(question: Question) {
      withContext(Dispatchers.IO) {
         questionDao.deleteQuestion(question)
      }
   }

   private suspend fun addStarterData() {
      val mathId = subjectDao.addSubject(Subject(title = "Math"))
      questionDao.addQuestion(
         Question(
            text = "What is 2 + 3?",
            answer = "2 + 3 = 5",
            subjectId = mathId
         )
      )
      questionDao.addQuestion(
         Question(
            text = "What is pi?",
            answer = "The ratio of a circle's circumference to its diameter.",
            subjectId = mathId
         )
      )

      val historyId = subjectDao.addSubject(Subject(title = "History"))
      questionDao.addQuestion(
         Question(
            text = "On what date was the U.S. Declaration of Independence adopted?",
            answer = "July 4, 1776",
            subjectId = historyId
         )
      )

      subjectDao.addSubject(Subject(title = "Computing"))
      subjectDao.addSubject(Subject(title = "Mobile Programming"))
   }
}
