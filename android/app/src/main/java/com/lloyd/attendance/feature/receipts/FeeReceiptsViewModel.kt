package com.lloyd.attendance.feature.receipts

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lloyd.attendance.api.ErpApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeeReceiptsViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = ErpApiClient(application)

    private val _uiState = MutableStateFlow<FeeReceiptsUiState>(FeeReceiptsUiState.Loading)
    val uiState: StateFlow<FeeReceiptsUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(FeeTab.RECEIPTS)
    val selectedTab: StateFlow<FeeTab> = _selectedTab.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: FeeTab) {
        _selectedTab.value = tab
    }

    fun loadData() {
        _uiState.value = FeeReceiptsUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactions = apiClient.getFeeTransactions(page = 1, size = 50)
                val receipts = apiClient.getFeeReceipts(page = 1, size = 50)
                withContext(Dispatchers.Main) {
                    _uiState.value = FeeReceiptsUiState.Success(
                        transactions = transactions,
                        receipts = receipts
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = FeeReceiptsUiState.Error(
                        e.message ?: "Failed to load fee records. Please check connection."
                    )
                }
            }
        }
    }

    fun printReceipt(context: Context, paidLogId: Long, receiptNo: String) {
        if (_isPrinting.value) return
        _isPrinting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val html = apiClient.getReceiptPrintHtml(paidLogId)
                withContext(Dispatchers.Main) {
                    _isPrinting.value = false
                    val jobName = if (receiptNo.isNotBlank()) "Receipt_$receiptNo" else "Fee_Receipt_$paidLogId"
                    ReceiptPrintAdapter.printHtml(context, html, jobName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isPrinting.value = false
                }
            }
        }
    }
}
