package com.example.focusapp.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.focusapp.ui.ajustes.AjustesScreen
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

    // barra de baixo só nas telas principais
    val rotaAtual = navController.currentBackStackEntryAsState().value?.destination?.route
    val abas = listOf(
        Aba("Timer", Icons.Default.Timer, NavRoutes.NovaSessao.route),
        Aba("Métricas", Icons.Default.BarChart, NavRoutes.Metricas.route),
        Aba("Ajustes", Icons.Default.Settings, NavRoutes.Ajustes.route)
    )
    val mostrarBarra = abas.any { it.rota == rotaAtual }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar {
                    for (aba in abas) {
                        NavigationBarItem(
                            selected = rotaAtual == aba.rota,
                            onClick = {
                                navController.navigate(aba.rota) {
                                    popUpTo(NavRoutes.NovaSessao.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(aba.icone, contentDescription = aba.nome) },
                            label = { Text(aba.nome) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = NavRoutes.NovaSessao.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {

            //nova sessão
            composable(route = NavRoutes.NovaSessao.route) {
                NovaSessaoScreen(
                    viewModel = focusViewModel,
                    locais = locais,
                    onGerenciarLocais = {
                        navController.navigate(NavRoutes.Locais.route)
                    },
                    onIniciar = {
                        navController.navigate(NavRoutes.Focus.route)
                    }
                )
            }

            //timer
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

            //locais
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

            //métricas
            composable(route = NavRoutes.Metricas.route) {
                val metricasViewModel: MetricasViewModel = viewModel()
                val sessoes by metricasViewModel.sessoes.collectAsState(initial = emptyList())

                MetricasScreen(
                    sessoes = sessoes,
                    locais = locais
                )
            }

            //ajustes
            composable(route = NavRoutes.Ajustes.route) {
                AjustesScreen(
                    viewModel = focusViewModel,
                    escuro = escuro,
                    onAlternarTema = onAlternarTema,
                    onMeusLocais = {
                        navController.navigate(NavRoutes.Locais.route)
                    },
                    onHistorico = {
                        navController.navigate(NavRoutes.History.route)
                    }
                )
            }

            //avaliação
            composable(route = NavRoutes.Avaliacao.route) {
                AvaliacaoScreen(
                    viewModel = focusViewModel,
                    onConcluir = {
                        navController.popBackStack()
                    }
                )
            }

            //histórico
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

            //detalhes
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
}

private class Aba(val nome: String, val icone: ImageVector, val rota: String)
