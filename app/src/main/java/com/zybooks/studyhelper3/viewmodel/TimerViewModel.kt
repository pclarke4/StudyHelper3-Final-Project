package com.zybooks.studyhelper3.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.repo.StudyRepository
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val studyRepo = StudyRepository.getInstance(application)

    val activeSubjects: LiveData<List<Subject>> = studyRepo.getActiveSubjects()

    fun getSubject(subjectId: Long): LiveData<Subject?> {
        return studyRepo.getSubject(subjectId)
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            studyRepo.updateSubject(subject)
        }
    }
}
