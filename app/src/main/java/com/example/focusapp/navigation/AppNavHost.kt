package com.example.focusapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.focusapp.ui.avaliacao.AvaliacaoScreen
import com.example.focusapp.ui.details.DetailScreen
import com.example.focusapp.ui.focus.FocusScreen
import com.example.focusapp.ui.focus.FocusViewModel
import com.example.focusapp.ui.history.HistoryScreen
import com.example.focusapp.ui.sessao.NovaSessaoScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // mesmo ViewModel nas duas telas (Nova sessão e timer)
    val focusViewModel: FocusViewModel = viewModel()

    // quando o foco termina, abre a avaliação
    LaunchedEffect(focusViewModel.aguardandoAvaliacao) {
        if (focusViewModel.aguardandoAvaliacao) {
            navController.navigate(NavRoutes.Avaliacao.route) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.NovaSessao.route,
        modifier = modifier
    ) {

        // 0. Tela de Nova sessão
        composable(route = NavRoutes.NovaSessao.route) {
            NovaSessaoScreen(
                viewModel = focusViewModel,
                onIniciar = {
                    navController.navigate(NavRoutes.Focus.route)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.History.route)
                }
            )
        }

        // 1. Tela de Foco
        composable(route = NavRoutes.Focus.route) {
            FocusScreen(
                viewModel = focusViewModel,
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.History.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Tela de Avaliação da sessão
        composable(route = NavRoutes.Avaliacao.route) {
            AvaliacaoScreen(
                viewModel = focusViewModel,
                onConcluir = {
                    navController.popBackStack()
                }
            )
        }

        // 2. Tela de Histórico
        composable(route = NavRoutes.History.route) {
            HistoryScreen(
                onNavigateToDetails = { sessionId ->
                    navController.navigate(NavRoutes.Details.createRoute(sessionId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 3. Tela de Detalhes
        composable(
            route = NavRoutes.Details.route, // "details/{sessionId}"
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