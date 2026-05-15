package com.zybooks.studyhelper3.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.zybooks.studyhelper3.model.Question
import com.zybooks.studyhelper3.repo.StudyRepository
import kotlinx.coroutines.launch

class QuestionEditViewModel(application: Application) : AndroidViewModel(application) {

    private val studyRepo = StudyRepository.getInstance(application)

    fun getQuestion(questionId: Long): LiveData<Question?> {
        return studyRepo.getQuestion(questionId)
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
}