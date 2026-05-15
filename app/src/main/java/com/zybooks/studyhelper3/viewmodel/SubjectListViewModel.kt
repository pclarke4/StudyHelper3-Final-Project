package com.zybooks.studyhelper3.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.zybooks.studyhelper3.model.Priority
import com.zybooks.studyhelper3.model.Subject
import com.zybooks.studyhelper3.repo.StudyRepository
import kotlinx.coroutines.launch

enum class SubjectSortOrder {
   ALPHABETIC, NEW_FIRST, OLD_FIRST, PRIORITY, DIFFICULTY
}

enum class SubjectFilter {
   ALL, COMPLETED, INCOMPLETE, HIGH_PRIORITY
}

class SubjectListViewModel(application: Application) : AndroidViewModel(application) {

   private val studyRepo = StudyRepository.getInstance(application)

   private val sortOrderLiveData = MutableLiveData(SubjectSortOrder.NEW_FIRST)
   private val filterLiveData = MutableLiveData(SubjectFilter.ALL)
   private val searchQueryLiveData = MutableLiveData("")

   private val combinedState = MediatorLiveData<Triple<String, SubjectFilter, SubjectSortOrder>>().apply {
      addSource(searchQueryLiveData) { q -> value = Triple(q ?: "", filterLiveData.value ?: SubjectFilter.ALL, sortOrderLiveData.value ?: SubjectSortOrder.NEW_FIRST) }
      addSource(filterLiveData) { f -> value = Triple(searchQueryLiveData.value ?: "", f ?: SubjectFilter.ALL, sortOrderLiveData.value ?: SubjectSortOrder.NEW_FIRST) }
      addSource(sortOrderLiveData) { s -> value = Triple(searchQueryLiveData.value ?: "", filterLiveData.value ?: SubjectFilter.ALL, s ?: SubjectSortOrder.NEW_FIRST) }
      value = Triple("", SubjectFilter.ALL, SubjectSortOrder.NEW_FIRST)
   }

   val subjectListLiveData: LiveData<List<Subject>> = combinedState.switchMap { (query, filter, sort) ->
      studyRepo.getSubjects().map { allSubjects ->
         allSubjects
            .filter { subject ->
               val matchesSearch = query.isBlank() || subject.title.contains(query, ignoreCase = true)
               val matchesFilter = when (filter) {
                  SubjectFilter.ALL -> true
                  SubjectFilter.COMPLETED -> subject.isCompleted
                  SubjectFilter.INCOMPLETE -> !subject.isCompleted
                  SubjectFilter.HIGH_PRIORITY -> subject.priority == Priority.HIGH
               }
               matchesSearch && matchesFilter
            }
            .sortedWith(when (sort) {
               SubjectSortOrder.ALPHABETIC -> compareBy { it.title.lowercase() }
               SubjectSortOrder.NEW_FIRST -> compareByDescending { it.updatedAt }
               SubjectSortOrder.OLD_FIRST -> compareBy { it.updatedAt }
               SubjectSortOrder.PRIORITY -> compareByDescending<Subject> { it.priority }.thenByDescending { it.updatedAt }
               SubjectSortOrder.DIFFICULTY -> compareByDescending<Subject> { it.difficulty }.thenByDescending { it.updatedAt }
            })
      }
   }

   fun setSearchQuery(query: String) {
      searchQueryLiveData.value = query
   }

   fun setFilter(filter: SubjectFilter) {
      filterLiveData.value = filter
   }

   fun setSortOrder(sortOrder: SubjectSortOrder) {
      sortOrderLiveData.value = sortOrder
   }

   val totalSubjects = studyRepo.getTotalSubjectCount()
   private val completedSubjectsCount = studyRepo.getCompletedSubjectCount()

   val progressPercentage: LiveData<Int> = MediatorLiveData<Int>().apply {
      addSource(totalSubjects) { total ->
         value = calculatePercentage(total ?: 0, completedSubjectsCount.value ?: 0)
      }
      addSource(completedSubjectsCount) { completed ->
         value = calculatePercentage(totalSubjects.value ?: 0, completed ?: 0)
      }
   }

   private fun calculatePercentage(total: Int, completed: Int): Int {
      return if (total == 0) 0 else (completed * 100 / total)
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
