package com.example.focusapp.ui.avaliacao

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.focusapp.ui.focus.FocusViewModel

private val FATORES = listOf("Silêncio", "Cansaço", "Celular", "Conforto", "Iluminação")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvaliacaoScreen(
    viewModel: FocusViewModel,
    onConcluir: () -> Unit
) {
    val activity = LocalContext.current as ComponentActivity
    val sessao = viewModel.sessaoPendente

    var estrelas by remember { mutableIntStateOf(0) }
    val fatoresEscolhidos = remember { mutableStateListOf<String>() }

    // depois de salvar (ou pular), a sessão pendente some e a tela fecha
    LaunchedEffect(viewModel.aguardandoAvaliacao) {
        if (!viewModel.aguardandoAvaliacao) onConcluir()
    }

    // se o app sair de cena aqui, a sessão não pode se perder
    DisposableEffect(activity) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_STOP) {
                viewModel.salvarPendenteSemAvaliacao()
            }
        }

        activity.lifecycle.addObserver(observador)

        onDispose {
            activity.lifecycle.removeObserver(observador)
        }
    }

    // o usuário precisa salvar ou pular
    BackHandler(enabled = true) { }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avaliação") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->

        if (sessao == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Sessão concluída",
                style = MaterialTheme.typography.headlineSmall
            )

            val resumoDb = if (sessao.mediaDb > 0) " • ${sessao.mediaDb} dB médio" else ""

            Text(
                text = "${sessao.duracaoMinutos} min • ${sessao.materia}$resumoDb",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Como foi a qualidade do estudo?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (nota in 1..5) {
                    IconButton(onClick = { estrelas = nota }) {
                        Icon(
                            imageVector = if (nota <= estrelas) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$nota estrelas",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = "O que influenciou?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (fator in FATORES) {
                    FilterChip(
                        selected = fator in fatoresEscolhidos,
                        onClick = {
                            if (fator in fatoresEscolhidos) {
                                fatoresEscolhidos.remove(fator)
                            } else {
                                fatoresEscolhidos.add(fator)
                            }
                        },
                        label = { Text(fator) }
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.salvarAvaliacao(estrelas, fatoresEscolhidos.toList())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (viewModel.descansoMinutos > 0) "Salvar e descansar" else "Salvar"
                )
            }

            TextButton(
                onClick = viewModel::pularAvaliacao,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pular avaliação")
            }
        }
    }
}
