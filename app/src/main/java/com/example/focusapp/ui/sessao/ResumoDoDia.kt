package com.example.focusapp.ui.sessao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.metricas.formatarHoras

// meta de hoje e sequência de dias; meta 0 = sem meta
@Composable
fun ResumoDoDia(minutosHoje: Int, meta: Int, sequencia: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = if (meta > 0) {
                    "Hoje: ${formatarHoras(minutosHoje)} de ${formatarHoras(meta)}"
                } else {
                    "Hoje: ${formatarHoras(minutosHoje)}"
                },
                style = MaterialTheme.typography.titleMedium
            )

            if (meta > 0) {
                LinearProgressIndicator(
                    progress = { (minutosHoje / meta.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (minutosHoje >= meta) {
                    Text(
                        text = "Meta batida!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (sequencia > 0) {
                Text(
                    text = "Sequência: $sequencia ${if (sequencia == 1) "dia" else "dias"} seguidos",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
