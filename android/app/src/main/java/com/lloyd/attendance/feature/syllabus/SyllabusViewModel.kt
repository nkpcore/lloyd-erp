package com.lloyd.attendance.feature.syllabus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SemesterFilter(val label: String, val semNumber: Int?) {
    ALL("All", null),
    SEM_1("Semester 1", 1),
    SEM_2("Semester 2", 2)
}

data class SyllabusUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val payload: SyllabusPayload? = null,
    val searchQuery: String = "",
    val selectedSemester: SemesterFilter = SemesterFilter.ALL,
    val selectedCategory: String = "All",
    val filteredCourses: List<SyllabusCourse> = emptyList()
)

class SyllabusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SyllabusRepository.getInstance(application)

    private val _uiState = MutableStateFlow(SyllabusUiState())
    val uiState: StateFlow<SyllabusUiState> = _uiState.asStateFlow()

    init {
        loadSyllabus()
    }

    fun loadSyllabus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val payload = repository.getSyllabus()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        payload = payload,
                        filteredCourses = applyFilters(payload.courses, state.searchQuery, state.selectedSemester, state.selectedCategory)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load syllabus") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val courses = state.payload?.courses ?: emptyList()
            state.copy(
                searchQuery = query,
                filteredCourses = applyFilters(courses, query, state.selectedSemester, state.selectedCategory)
            )
        }
    }

    fun onSemesterFilterChanged(filter: SemesterFilter) {
        _uiState.update { state ->
            val courses = state.payload?.courses ?: emptyList()
            state.copy(
                selectedSemester = filter,
                filteredCourses = applyFilters(courses, state.searchQuery, filter, state.selectedCategory)
            )
        }
    }

    fun onCategoryFilterChanged(category: String) {
        _uiState.update { state ->
            val courses = state.payload?.courses ?: emptyList()
            state.copy(
                selectedCategory = category,
                filteredCourses = applyFilters(courses, state.searchQuery, state.selectedSemester, category)
            )
        }
    }

    private fun applyFilters(
        courses: List<SyllabusCourse>,
        query: String,
        semester: SemesterFilter,
        category: String
    ): List<SyllabusCourse> {
        val q = query.trim().lowercase()
        return courses.filter { course ->
            val matchesSem = semester.semNumber == null || course.semesters.contains(semester.semNumber)
            val matchesCat = category == "All" || course.category.equals(category, ignoreCase = true) || course.type.equals(category, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                    course.code.lowercase().contains(q) ||
                    course.title.lowercase().contains(q) ||
                    course.shortTitle.lowercase().contains(q) ||
                    course.units.any { u ->
                        u.title.lowercase().contains(q) ||
                        u.topics.any { t -> t.lowercase().contains(q) } ||
                        u.applications.any { a -> a.lowercase().contains(q) }
                    }
            matchesSem && matchesCat && matchesQuery
        }
    }
}
