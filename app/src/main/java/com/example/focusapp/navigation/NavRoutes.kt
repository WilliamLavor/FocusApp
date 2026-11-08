package com.example.focusapp.navigation

sealed class NavRoutes(val route: String) {

    data object Focus : NavRoutes("focus")

    data object History : NavRoutes("history")

    data object Details : NavRoutes("details/{sessionId}") {
        fun createRoute(sessionId: Int): String = "details/$sessionId"
    }
}