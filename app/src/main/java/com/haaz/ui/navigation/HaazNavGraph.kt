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
                val selectedHistory =
                    savedStateHandle.getStateFlow<String?>("selected_history", null)
                        .collectAsState().value

                HomePage(
                    onOpenHistory = { navController.navigate("history") },
                    selectedHistory = selectedHistory,
                    onHistoryConsumed = { savedStateHandle["selected_history"] = null }
                )
            }
            composable("history") {
                HistoryPage(
                    onBack = { navController.popBackStack() },
                    onQuerySelected = { query ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_history",
                            query
                        )
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
