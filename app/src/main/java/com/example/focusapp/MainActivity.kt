package com.example.focusapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.focusapp.ui.FocusApp
import com.example.focusapp.ui.theme.FocusAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)

        setContent {
            val sistemaEscuro = isSystemInDarkTheme()

            // começa com o tema do celular; depois vale o que a pessoa escolheu
            var escuro by rememberSaveable {
                mutableStateOf(
                    if (prefs.contains("escuro")) prefs.getBoolean("escuro", false) else sistemaEscuro
                )
            }

            //ícones das barras do sistema seguem o tema do app
            DisposableEffect(escuro) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { escuro },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { escuro }
                )
                onDispose { }
            }

            FocusAppTheme(darkTheme = escuro) {
                FocusApp(
                    escuro = escuro,
                    onAlternarTema = {
                        escuro = !escuro
                        prefs.edit().putBoolean("escuro", escuro).apply()
                    }
                )
            }
        }
    }
}