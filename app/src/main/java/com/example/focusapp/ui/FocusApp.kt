package com.example.focusapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.focusapp.navigation.AppNavHost

@Composable
fun FocusApp(
    escuro: Boolean,
    onAlternarTema: () -> Unit
) {
    val navController = rememberNavController()
    AppNavHost(
        navController = navController,
        escuro = escuro,
        onAlternarTema = onAlternarTema
    )
}