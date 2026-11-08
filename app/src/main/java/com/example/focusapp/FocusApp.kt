package com.example.focusapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.focusapp.navigation.AppNavHost

@Composable
fun FocusApp() {
    val navController = rememberNavController()
    AppNavHost(navController = navController)
}