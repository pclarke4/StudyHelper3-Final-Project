package com.zybooks.studyhelper3.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.zybooks.studyhelper3.model.Question
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.repo.StudyRepository
import kotlinx.coroutines.launch

class QuestionDetailViewModel(application: Application) : AndroidViewModel(application) {

   private val studyRepo = StudyRepository.getInstance(application)

   private val questionIdLiveData = MutableLiveData<Long>()

   val questionLiveData: LiveData<Question?> =
      questionIdLiveData.switchMap { questionId ->
         studyRepo.getQuestion(questionId)
      }

   fun loadQuestion(questionId: Long) {
      questionIdLiveData.value = questionId
   }

   fun getQuestions(subjectId: Long): LiveData<List<Question>> = studyRepo.getQuestions(subjectId)

   fun addQuestion(question: Question) {
      viewModelScope.launch {
         studyRepo.addQuestion(question)
      }
   }

   fun updateQuestion(question: Question) {
      viewModelScope.launch {
         studyRepo.updateQuestion(question)
      }
   }

   fun deleteQuestion(question: Question) {
      viewModelScope.launch {
         studyRepo.deleteQuestion(question)
      }
   }

   fun getSubject(subjectId: Long): LiveData<Subject?> = studyRepo.getSubject(subjectId)

   // Suspend version for sequential operations in Activity
   suspend fun addSubjectSync(subject: Subject): Long {
      return studyRepo.addSubject(subject)
   }

   fun addSubject(subject: Subject) {
      viewModelScope.launch {
         studyRepo.addSubject(subject)
      }
   }

   fun updateSubject(subject: Subject) {
      viewModelScope.launch {
         studyRepo.updateSubject(subject)
      }
   }

   fun deleteSubject(subject: Subject) {
      viewModelScope.launch {
         studyRepo.deleteSubject(subject)
      }
   }
}
