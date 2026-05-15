package com.zybooks.studyhelper3.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.repo.StudyRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val studyRepo = StudyRepository.getInstance(application)
    private val _selectedDate = MutableLiveData<Long>()
    private val dbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    init {
        // Default to today in UTC to match the picker logic
        _selectedDate.value = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    val selectedDate: LiveData<Long> = _selectedDate

    val subjectsForDate: LiveData<List<Subject>> = _selectedDate.switchMap { timestamp ->
        // Convert the selection (UTC millis) to LocalDate
        val localDate = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate()
        val dateString = localDate.format(dbDateFormatter)
        
        studyRepo.getSubjectsForDate(dateString)
    }

    fun selectDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }
}
