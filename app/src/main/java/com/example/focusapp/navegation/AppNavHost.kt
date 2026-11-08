package com.example.focusapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.focusapp.navegation.NavRoutes
import com.example.focusapp.ui.focus.FocusScreen

// Importar os seus ecrãs reais quando estiverem concluídos
// import com.example.focusapp.ui.focus.FocusScreen
// import com.example.focusapp.ui.history.HistoryScreen
// import com.example.focusapp.ui.details.DetailScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Focus.route, // Utiliza a classe selada
        modifier = modifier
    ) {

        // 1. Ecrã de Foco
        composable(route = NavRoutes.Focus.route) {
            FocusScreen(
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.History.route)
                }
            )
        }

        // 2. Ecrã de Histórico
        composable(route = NavRoutes.History.route) {
            HistoryScreen(
                onNavigateToDetails = { sessionId ->
                    // Utiliza a função para injetar o ID com segurança
                    navController.navigate(NavRoutes.Details.createRoute(sessionId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 3. Ecrã de Detalhes
        composable(
            route = NavRoutes.Details.route, // Fica "details/{sessionId}"
            arguments = listOf(
                navArgument("sessionId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getInt("sessionId") ?: 0

            DetailScreen(
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}