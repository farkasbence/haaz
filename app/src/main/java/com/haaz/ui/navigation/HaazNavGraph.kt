package com.haaz.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.haaz.ui.history.HistoryPage
import com.haaz.ui.home.HomePage

@Composable
fun HaazNavGraph() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") { backStackEntry ->
                val savedStateHandle = backStackEntry.savedStateHandle
                val selectedPrompt =
                    savedStateHandle.getStateFlow<String?>("selected_history_prompt", null)
                        .collectAsState().value
                val selectedClipFileName =
                    savedStateHandle.getStateFlow<String?>("selected_history_clip", null)
                        .collectAsState().value

                HomePage(
                    onOpenHistory = { navController.navigate("history") },
                    selectedHistoryPrompt = selectedPrompt,
                    selectedClipFileName = selectedClipFileName,
                    onHistoryConsumed = {
                        savedStateHandle["selected_history_prompt"] = null
                        savedStateHandle["selected_history_clip"] = null
                    }
                )
            }
            composable("history") {
                HistoryPage(
                    onBack = { navController.popBackStack() },
                    onPromptSelected = { query ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_history_prompt",
                            query
                        )
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_history_clip",
                            null
                        )
                        navController.popBackStack()
                    },
                    onClipSelected = { clip ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_history_prompt",
                            clip.prompt
                        )
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_history_clip",
                            clip.fileName
                        )
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
