package com.example.focusapp.ui.metricas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle
import java.util.Locale

// barras simples, uma por dia, feitas só com Box
@Composable
fun GraficoSemana(dias: List<DiaMinutos>) {
    val maior = (dias.maxOfOrNull { it.minutos } ?: 0).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (dia in dias) {

            val altura = if (dia.minutos > 0) {
                (100f * dia.minutos / maior).coerceAtLeast(4f)
            } else {
                2f
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (dia.minutos > 0) formatarHoras(dia.minutos) else "",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(altura.dp)
                        .background(
                            color = if (dia.minutos > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                Text(
                    text = dia.data.dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("pt-BR"))
                        .take(3),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
