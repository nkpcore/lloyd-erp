package com.lloyd.attendance.feature.feedback

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.vector.ImageVector

enum class FeedbackCategory(
    val apiValue: String,
    val displayName: String,
    val icon: ImageVector
) {
    GENERAL("general", "General Feedback", Icons.Filled.Feedback),
    BUG("bug", "Report Bug / Glitch", Icons.Filled.BugReport),
    FEATURE("feature", "Feature Suggestion", Icons.Filled.Lightbulb),
    ATTENDANCE("attendance", "Attendance Discrepancy", Icons.Filled.DateRange),
    MARKS("marks", "Internal Marks Query", Icons.Filled.Grade),
    UI("ui", "UI / Theme Polish", Icons.Filled.Palette);

    companion object {
        fun fromApi(value: String?): FeedbackCategory =
            entries.find { it.apiValue.equals(value, ignoreCase = true) } ?: GENERAL
    }
}

sealed interface FeedbackUiState {
    data object Idle : FeedbackUiState
    data object Submitting : FeedbackUiState
    data class Success(val id: String, val submittedAt: String) : FeedbackUiState
    data class Error(val message: String) : FeedbackUiState
}
