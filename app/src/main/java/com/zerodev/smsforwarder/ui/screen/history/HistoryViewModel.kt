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
 * ViewModel for the history screen with pagination and delete support.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    
    companion object {
        private const val PAGE_SIZE = 15
    }
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    private var currentPage = 0
    private var allHistoryLoaded = false
    
    init {
        loadFirstPage()
        loadStatistics()
    }
    
    /**
     * Load the first page of history.
     */
    private fun loadFirstPage() {
        currentPage = 0
        allHistoryLoaded = false
        _uiState.value = _uiState.value.copy(
            history = emptyList(),
            isLoading = true,
            isLoadingMore = false
        )
        loadHistoryPage()
    }
    
    /**
     * Load the next page of history.
     */
    fun loadNextPage() {
        if (_uiState.value.isLoadingMore || allHistoryLoaded) return
        
        currentPage++
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        loadHistoryPage()
    }
    
    /**
     * Refresh the history by reloading from the first page.
     */
    fun refreshHistory() {
        loadFirstPage()
        loadStatistics()
    }
    
    /**
     * Load a specific page of history.
     */
    private fun loadHistoryPage() {
        viewModelScope.launch {
            historyRepository.getHistoryPaginated(currentPage, PAGE_SIZE)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = error.message ?: "Unknown error occurred"
                    )
                }
                .collect { pageHistory ->
                    if (pageHistory.size < PAGE_SIZE) {
                        allHistoryLoaded = true
                    }
                    
                    val currentHistory = if (currentPage == 0) {
                        pageHistory
                    } else {
                        _uiState.value.history + pageHistory
                    }
                    
                    val matchedCount = currentHistory.count { it.matchedRule }
                    
                    _uiState.value = _uiState.value.copy(
                        history = currentHistory,
                        matchedCount = matchedCount,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMorePages = !allHistoryLoaded,
                        error = null
                    )
                }
        }
    }
    
    /**
     * Delete a specific history entry.
     * @param historyId The ID of the history entry to delete
     */
    fun deleteHistoryEntry(historyId: Long) {
        viewModelScope.launch {
            try {
                historyRepository.deleteHistoryById(historyId)
                
                // Remove from current list
                val updatedHistory = _uiState.value.history.filter { it.id != historyId }
                val matchedCount = updatedHistory.count { it.matchedRule }
                
                _uiState.value = _uiState.value.copy(
                    history = updatedHistory,
                    matchedCount = matchedCount
                )
                
                // Reload statistics
                loadStatistics()
                
                // If we have fewer than PAGE_SIZE items and haven't loaded all, load more
                if (updatedHistory.size < PAGE_SIZE && !allHistoryLoaded) {
                    loadNextPage()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete history entry"
                )
            }
        }
    }
    
    /**
     * Clear all history entries.
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                historyRepository.deleteAllHistory()
                _uiState.value = _uiState.value.copy(
                    history = emptyList(),
                    matchedCount = 0
                )
                loadStatistics()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to clear history"
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