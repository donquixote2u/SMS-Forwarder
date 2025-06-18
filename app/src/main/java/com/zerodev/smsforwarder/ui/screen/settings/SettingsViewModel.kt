package com.zerodev.smsforwarder.ui.screen.settings

import androidx.lifecycle.ViewModel
import com.zerodev.smsforwarder.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for Settings Screen to provide dependency injection.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    val appRepository: AppRepository
) : ViewModel() 