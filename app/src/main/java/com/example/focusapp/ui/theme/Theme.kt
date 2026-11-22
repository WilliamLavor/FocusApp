package com.example.focusapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Azul80,
    onPrimary = Color(0xFF0B2236),
    primaryContainer = AzulEscuro30,
    onPrimaryContainer = Color(0xFFD2E6F8),
    secondary = AzulAcinzentado80,
    onSecondary = Color(0xFF12293B),
    secondaryContainer = Color(0xFF243A4E),
    onSecondaryContainer = Color(0xFFD6E6F4),
    tertiary = Laranja80,
    onTertiary = Color(0xFF3A2400),
    background = FundoEscuro,
    onBackground = TextoClaro,
    surface = FundoEscuro,
    onSurface = TextoClaro,
    surfaceVariant = Color(0xFF253443),
    onSurfaceVariant = Color(0xFFB7C7D6),
    outline = Color(0xFF7F93A6),
    surfaceContainerLowest = Color(0xFF070E14),
    surfaceContainerLow = Color(0xFF101B25),
    surfaceContainer = Color(0xFF142230),
    surfaceContainerHigh = Color(0xFF1B2C3B),
    surfaceContainerHighest = Color(0xFF223445)
)

private val LightColorScheme = lightColorScheme(
    primary = Azul40,
    onPrimary = Color.White,
    primaryContainer = AzulClaro90,
    onPrimaryContainer = Color(0xFF0F2F4B),
    secondary = AzulAcinzentado40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEBF5),
    onSecondaryContainer = Color(0xFF17324A),
    tertiary = Laranja60,
    onTertiary = Color(0xFF3A2400),
    background = FundoClaro,
    onBackground = TextoEscuro,
    surface = FundoClaro,
    onSurface = TextoEscuro,
    surfaceVariant = Color(0xFFE2EDF6),
    onSurfaceVariant = Color(0xFF41546A),
    outline = Color(0xFF7A8FA3),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FBFE),
    surfaceContainer = Color(0xFFEDF4FA),
    surfaceContainerHigh = Color(0xFFE6EFF7),
    surfaceContainerHighest = Color(0xFFDFEAF4)
)

@Composable
fun FocusAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // desligado pra usar as cores do nosso design (a cor dinâmica trocaria pelas do papel de parede)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}