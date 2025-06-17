package com.zerodev.smsforwarder.ui.screen.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerodev.smsforwarder.data.repository.RuleRepository
import com.zerodev.smsforwarder.domain.model.Rule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the rules screen.
 */
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()
    
    init {
        loadRules()
    }
    
    /**
     * Load all rules from the repository.
     */
    private fun loadRules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            ruleRepository.getAllRules()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error occurred"
                    )
                }
                .collect { rules ->
                    _uiState.value = _uiState.value.copy(
                        rules = rules,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }
    
    /**
     * Create a new rule.
     */
    fun createRule(rule: Rule) {
        viewModelScope.launch {
            try {
                ruleRepository.createRule(rule)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create rule"
                )
            }
        }
    }
    
    /**
     * Toggle the active status of a rule.
     */
    fun toggleRuleStatus(ruleId: Long, isActive: Boolean) {
        viewModelScope.launch {
            try {
                ruleRepository.toggleRuleStatus(ruleId, isActive)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update rule"
                )
            }
        }
    }
    
    /**
     * Delete a rule.
     */
    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            try {
                ruleRepository.deleteRule(ruleId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete rule"
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