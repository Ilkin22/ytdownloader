package com.ytdownloader.app.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ytdownloader.app.presentation.library.LibraryScreen
import com.ytdownloader.app.presentation.queue.QueueScreen
import com.ytdownloader.app.presentation.settings.SettingsScreen
import com.ytdownloader.app.presentation.home.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Queue : Screen("queue")
    object Library : Screen("library")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(initialSharedUrl: String? = null) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                initialUrl = initialSharedUrl,
                onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Queue.route) {
            QueueScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
