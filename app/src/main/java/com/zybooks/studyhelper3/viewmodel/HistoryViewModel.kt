package com.zybooks.studyhelper3.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.repo.StudyRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val studyRepo = StudyRepository.getInstance(application)

    val completedSubjectsLiveData: LiveData<List<Subject>> = studyRepo.getCompletedSubjects()
    
    val totalTasks: LiveData<Int> = studyRepo.getTotalSubjectCount()
    val completedCount: LiveData<Int> = studyRepo.getCompletedSubjectCount()
    
    private val _streakCount = MutableLiveData<Int>(0)
    val streakCount: LiveData<Int> = _streakCount

    val incompleteCount: LiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(totalTasks) { total ->
            value = (total ?: 0) - (completedCount.value ?: 0)
        }
        addSource(completedCount) { completed ->
            value = (totalTasks.value ?: 0) - (completed ?: 0)
            calculateStreak()
        }
    }
    
    val completionPercentage: LiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(totalTasks) { total ->
            value = calculatePercentage(total ?: 0, completedCount.value ?: 0)
        }
        addSource(completedCount) { completed ->
            value = calculatePercentage(totalTasks.value ?: 0, completed ?: 0)
        }
    }

    private fun calculatePercentage(total: Int, completed: Int): Int {
        return if (total == 0) 0 else (completed * 100 / total)
    }

    private fun calculateStreak() {
        viewModelScope.launch {
            val dates = studyRepo.getCompletionDates()
            if (dates.isEmpty()) {
                _streakCount.postValue(0)
                return@launch
            }

            val today = LocalDate.now(ZoneOffset.UTC)

            val uniqueDates = dates.map {
                Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
            }.distinct().sortedDescending()

            if (uniqueDates.isEmpty()) {
                _streakCount.postValue(0)
                return@launch
            }

            var streak = 0
            val mostRecent = uniqueDates.first()
            
            // Check if streak is still active (completed something today or yesterday)
            if (mostRecent.isBefore(today.minusDays(1))) {
                _streakCount.postValue(0)
                return@launch
            }
            
            streak = 1
            var currentCheckDate = mostRecent

            // Count backwards
            for (i in 1 until uniqueDates.size) {
                val previousDate = uniqueDates[i]
                if (previousDate == currentCheckDate.minusDays(1)) {
                    streak++
                    currentCheckDate = previousDate
                } else {
                    break
                }
            }
            _streakCount.postValue(streak)
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            studyRepo.updateSubject(subject)
        }
    }
}
