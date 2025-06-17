package com.zerodev.smsforwarder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zerodev.smsforwarder.ui.screen.history.HistoryScreen
import com.zerodev.smsforwarder.ui.screen.rules.RulesScreen
import com.zerodev.smsforwarder.ui.screen.settings.SettingsScreen

/**
 * Main navigation setup for the SMS Forwarder app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsForwarderNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Rules.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Rules.route) {
                RulesScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Rules,
        Screen.History,
        Screen.Settings
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination to avoid building up a large stack
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

/**
 * Navigation screens definition.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Rules : Screen("rules", "Rules", Icons.Filled.Home)
    object History : Screen("history", "History", Icons.Filled.Star)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
} 