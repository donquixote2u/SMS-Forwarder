package com.zerodev.smsforwarder.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerodev.smsforwarder.domain.model.ForwardingStatus
import com.zerodev.smsforwarder.domain.model.ForwardingHistory
import com.zerodev.smsforwarder.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the history screen.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
        loadStatistics()
    }
    
    /**
     * Load forwarding history from the repository.
     */
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            historyRepository.getAllHistory()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error occurred"
                    )
                }
                .collect { history ->
                    val matchedCount = history.count { it.matchedRule }
                    _uiState.value = _uiState.value.copy(
                        history = history,
                        matchedCount = matchedCount,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }
    
    /**
     * Load statistics for the history.
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val stats = historyRepository.getHistoryStatistics()
                val totalCount = stats.values.sum()
                val successCount = stats[ForwardingStatus.SUCCESS] ?: 0
                val failedCount = stats[ForwardingStatus.FAILED] ?: 0
                
                _uiState.value = _uiState.value.copy(
                    totalCount = totalCount,
                    successCount = successCount,
                    failedCount = failedCount
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load statistics"
                )
            }
        }
    }
    
    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
} 