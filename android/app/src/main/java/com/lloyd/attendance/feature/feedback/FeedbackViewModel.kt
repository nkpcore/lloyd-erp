package com.lloyd.attendance.feature.feedback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedbackViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FeedbackRepository(application)

    private val _selectedCategory = MutableStateFlow(FeedbackCategory.GENERAL)
    val selectedCategory: StateFlow<FeedbackCategory> = _selectedCategory.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _uiState = MutableStateFlow<FeedbackUiState>(FeedbackUiState.Idle)
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun selectCategory(category: FeedbackCategory) {
        _selectedCategory.value = category
    }

    fun updateMessage(text: String) {
        if (text.length <= 2000) {
            _message.value = text
        }
    }

    fun submit() {
        val currentText = _message.value.trim()
        if (currentText.isEmpty()) {
            _uiState.value = FeedbackUiState.Error("Please enter your feedback message.")
            return
        }

        _uiState.value = FeedbackUiState.Submitting
        viewModelScope.launch {
            val result = repository.submitFeedback(
                category = _selectedCategory.value,
                message = currentText
            )
            result.onSuccess { (id, submittedAt) ->
                _uiState.value = FeedbackUiState.Success(id, submittedAt)
                _message.value = ""
            }.onFailure { err ->
                _uiState.value = FeedbackUiState.Error(err.message ?: "Failed to submit feedback. Check connection.")
            }
        }
    }

    fun resetState() {
        _uiState.value = FeedbackUiState.Idle
    }
}
