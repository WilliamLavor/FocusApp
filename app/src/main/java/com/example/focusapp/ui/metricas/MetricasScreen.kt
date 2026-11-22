package com.example.focusapp.ui.metricas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.FocusSession
import com.example.focusapp.data.Local

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricasScreen(
    sessoes: List<FocusSession>,
    locais: List<Local>
) {
    val metricas = remember(sessoes, locais) { calcularMetricas(sessoes, locais) }
    val melhor = remember(metricas) { melhorLocal(metricas) }
    val maiorTempo = (metricas.maxOfOrNull { it.minutos } ?: 1).coerceAtLeast(1)

    //qual local ta aberto (chave = id, -1 = sem local)
    var aberto by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Métricas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Onde você rende mais",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Últimos 30 dias",
                style = MaterialTheme.typography.bodyMedium
            )

            if (sessoes.isEmpty()) {
                Text("Ainda não há sessões nos últimos 30 dias.")
            }

            if (melhor != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text = if (melhor.qualidade != null) "Seu melhor lugar" else "Local mais usado",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Text(
                            text = melhor.nome,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        val partes = mutableListOf<String>()
                        if (melhor.qualidade != null) {
                            partes.add("★ ${"%.1f".format(melhor.qualidade)} qualidade")
                        }
                        if (melhor.mediaDb > 0) partes.add("${melhor.mediaDb} dB")
                        partes.add(formatarHoras(melhor.minutos))

                        Text(
                            text = partes.joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (metricas.isNotEmpty()) {

                Text(
                    text = "Toque num local para ver os detalhes",
                    style = MaterialTheme.typography.bodySmall
                )

                for (metrica in metricas) {

                    val chave = metrica.localId ?: -1

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aberto = if (aberto == chave) null else chave },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = metrica.nome,
                                style = MaterialTheme.typography.titleSmall
                            )

                            val db = if (metrica.mediaDb > 0) "${metrica.mediaDb} dB • " else ""

                            Text(
                                text = "$db${formatarHoras(metrica.minutos)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        LinearProgressIndicator(
                            progress = { metrica.minutos / maiorTempo.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (aberto == chave) {
                            DetalhesLocal(metrica)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetalhesLocal(metrica: MetricaLocal) {

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            if (metrica.sessoes == 0) {
                Text("Ainda não há sessões aqui.")
            } else {
                val data = metrica.ultimaSessao?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                }

                Text("Sessões: ${metrica.sessoes}")
                Text("Tempo total: ${formatarHoras(metrica.minutos)}")
                Text(
                    "Ruído médio: " +
                        if (metrica.mediaDb > 0) "${metrica.mediaDb} dB" else "sem medição"
                )
                Text(
                    "Qualidade média: " +
                        if (metrica.qualidade != null) "★ ${"%.1f".format(metrica.qualidade)}" else "sem nota"
                )
                if (data != null) Text("Última sessão: $data")
            }
        }
    }
}
