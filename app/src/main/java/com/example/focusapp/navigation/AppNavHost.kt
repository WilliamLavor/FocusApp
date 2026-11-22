package com.example.focusapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.focusapp.ui.locais.LocaisScreen
import com.example.focusapp.ui.locais.LocaisViewModel
import com.example.focusapp.ui.metricas.MetricasScreen
import com.example.focusapp.ui.metricas.MetricasViewModel
import com.example.focusapp.ui.sessao.NovaSessaoScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    escuro: Boolean,
    onAlternarTema: () -> Unit,
    modifier: Modifier = Modifier
) {
    //mesmo ViewModel nas duas telas
    val focusViewModel: FocusViewModel = viewModel()
    val locaisViewModel: LocaisViewModel = viewModel()
    val locais by locaisViewModel.locais.collectAsState(initial = emptyList())

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
                locais = locais,
                escuro = escuro,
                onAlternarTema = onAlternarTema,
                onGerenciarLocais = {
                    navController.navigate(NavRoutes.Locais.route)
                },
                onNavigateToMetricas = {
                    navController.navigate(NavRoutes.Metricas.route)
                },
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

        // Tela de Locais
        composable(route = NavRoutes.Locais.route) {
            LocaisScreen(
                locais = locais,
                onSalvar = locaisViewModel::salvar,
                onExcluir = locaisViewModel::excluir,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Tela de Métricas
        composable(route = NavRoutes.Metricas.route) {
            val metricasViewModel: MetricasViewModel = viewModel()
            val sessoes by metricasViewModel.sessoes.collectAsState(initial = emptyList())

            MetricasScreen(
                sessoes = sessoes,
                locais = locais,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Tela de Avaliação
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