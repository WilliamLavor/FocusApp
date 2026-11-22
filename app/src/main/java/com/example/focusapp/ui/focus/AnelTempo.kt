package com.example.focusapp.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// anel de progresso com o conteúdo (o tempo) no meio
@Composable
fun AnelTempo(
    progresso: Float,
    modifier: Modifier = Modifier,
    corAnel: Color = MaterialTheme.colorScheme.primary,
    corTrilho: Color = MaterialTheme.colorScheme.primaryContainer,
    conteudo: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progresso.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = corAnel,
            trackColor = corTrilho,
            strokeWidth = 10.dp
        )

        conteudo()
    }
}
