package com.lloyd.attendance.feature.receipts

import com.lloyd.attendance.api.Models

sealed interface FeeReceiptsUiState {
    data object Loading : FeeReceiptsUiState
    data class Success(
        val transactions: List<Models.FeeTransactionItem>,
        val receipts: List<Models.FeeReceiptItem>
    ) : FeeReceiptsUiState
    data class Error(val message: String) : FeeReceiptsUiState
}

enum class FeeTab(val title: String) {
    RECEIPTS("Official Receipts"),
    TRANSACTIONS("Payment History")
}
