package com.zerodev.smsforwarder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zerodev.smsforwarder.ui.navigation.SmsForwarderNavigation
import com.zerodev.smsforwarder.ui.theme.SMSForwarderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMSForwarderTheme {
                SmsForwarderNavigation()
            }
        }
    }
}