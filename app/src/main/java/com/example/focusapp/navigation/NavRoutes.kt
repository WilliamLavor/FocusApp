package com.example.focusapp.navigation

sealed class NavRoutes(val route: String) {

    data object NovaSessao : NavRoutes("nova_sessao")

    data object Avaliacao : NavRoutes("avaliacao")

    data object Locais : NavRoutes("locais")

    data object Ajustes : NavRoutes("ajustes")

    data object Metricas : NavRoutes("metricas")

    data object Focus : NavRoutes("focus")

    data object History : NavRoutes("history")

    data object Details : NavRoutes("details/{sessionId}") {
        fun createRoute(sessionId: Int): String = "details/$sessionId"
    }
}