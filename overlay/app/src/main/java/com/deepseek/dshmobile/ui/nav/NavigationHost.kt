package com.deepseek.dshmobile.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deepseek.dshmobile.ui.screens.ChatScreen
import com.deepseek.dshmobile.ui.screens.SessionListScreen
import com.deepseek.dshmobile.ui.screens.SettingsScreen
import com.deepseek.dshmobile.ui.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Sessions : Screen("sessions")
    object Chat : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
    object Settings : Screen("settings")
}

@Composable
fun NavigationHost(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Sessions.route
    ) {
        composable(Screen.Sessions.route) {
            val sessions by viewModel.sessions.collectAsState()
            SessionListScreen(
                sessions = sessions,
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId))
                },
                onNewSession = {
                    viewModel.createSession("新会话")
                },
                onDeleteSession = { sessionId ->
                    viewModel.deleteSession(sessionId)
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("sessionId") { defaultValue = "" })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
            val messages by viewModel.getMessages(sessionId).collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()

            ChatScreen(
                sessionId = sessionId,
                messages = messages,
                isLoading = isLoading,
                onSend = { text -> viewModel.sendMessage(sessionId, text) },
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}
