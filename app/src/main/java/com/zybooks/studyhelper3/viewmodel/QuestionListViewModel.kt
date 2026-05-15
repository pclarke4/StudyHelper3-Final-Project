package com.zybooks.studyhelper3.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.zybooks.studyhelper3.model.Question
import com.zybooks.studyhelper3.repo.StudyRepository
import kotlinx.coroutines.launch

class QuestionListViewModel(application: Application) : AndroidViewModel(application) {

   private val studyRepo = StudyRepository.getInstance(application)

   private val subjectIdLiveData = MutableLiveData<Long>()

   val questionListLiveData: LiveData<List<Question>> =
      subjectIdLiveData.switchMap { subjectId ->
         studyRepo.getQuestions(subjectId)
      }

   fun loadQuestions(subjectId: Long) {
      subjectIdLiveData.value = subjectId
   }

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
}