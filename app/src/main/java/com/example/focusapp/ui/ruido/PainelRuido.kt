package com.example.focusapp.ui.ruido

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// a partir daqui o ruído conta como "barulhento"
const val LIMITE_BARULHENTO_DB = 60

fun classificarRuido(db: Int): String = when {
    db < 40 -> "Silencioso"
    db < LIMITE_BARULHENTO_DB -> "Moderado"
    else -> "Barulhento"
}

// Card com o dB atual, gráfico das leituras e média/pico/mínimo
@Composable
fun PainelRuido(
    leituras: List<Int>,
    atual: Int,
    media: Int,
    pico: Int,
    minimo: Int,
    picos: Int,
    rotuloAtual: String = "Agora",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Ambiente sonoro",
            style = MaterialTheme.typography.titleMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = rotuloAtual,
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = "$atual dB",
                    style = MaterialTheme.typography.displaySmall
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = classificarRuido(atual),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Grafico(leituras = resumir(leituras, 32))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Estatistica("Média", media, Modifier.weight(1f))
            Estatistica("Pico", pico, Modifier.weight(1f))
            Estatistica("Mín.", minimo, Modifier.weight(1f))
        }

        if (picos > 0) {
            val texto = if (picos == 1) {
                "1 pico de ruído detectado. Considere fones com cancelamento."
            } else {
                "$picos picos de ruído detectados. Considere fones com cancelamento."
            }

            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun Grafico(leituras: List<Int>) {
    val corNormal = MaterialTheme.colorScheme.primary
    val corPico = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (valor in leituras) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight((valor / 100f).coerceIn(0.05f, 1f))
                    .background(
                        color = if (valor >= LIMITE_BARULHENTO_DB) corPico else corNormal,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun Estatistica(titulo: String, valor: Int, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$valor",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "$titulo dB",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// junta as leituras em poucas barras, guardando o maior valor de cada grupo
private fun resumir(leituras: List<Int>, barras: Int): List<Int> {
    if (leituras.size <= barras) return leituras.toList()

    val tamanho = leituras.size / barras.toDouble()

    return List(barras) { i ->
        val inicio = (i * tamanho).toInt()
        val fim = maxOf(((i + 1) * tamanho).toInt().coerceAtMost(leituras.size), inicio + 1)
        leituras.subList(inicio, fim).max()
    }
}
