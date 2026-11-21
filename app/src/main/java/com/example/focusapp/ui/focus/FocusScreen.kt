package com.example.focusapp.ui.focus

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // permissão do microfone
    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        temPermissao = concedida
    }

    LaunchedEffect(Unit) {
        if (!temPermissao) {
            pedirPermissao.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(temPermissao) {
        viewModel.definirPermissaoMicrofone(temPermissao)
    }

    // avisa o ViewModel quando o app inicia ou para
    DisposableEffect(activity) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_START) viewModel.aoIniciarApp()
            if (evento == Lifecycle.Event.ON_STOP) viewModel.aoPararApp()
        }

        activity.lifecycle.addObserver(observador)

        onDispose {
            activity.lifecycle.removeObserver(observador)
            viewModel.pausarSensor()
        }
    }

    // Impede o botão voltar durante o foco
    BackHandler(enabled = viewModel.focando) { }

    // Mantém a tela ligada durante o foco
    val view = LocalView.current

    DisposableEffect(viewModel.focando) {
        view.keepScreenOn = viewModel.focando

        onDispose {
            view.keepScreenOn = false
        }
    }

    val ruidoDb = viewModel.ruidoDb

    val descricaoRuido = when {
        ruidoDb < 40 -> "Silencioso"
        ruidoDb < 60 -> "Moderado"
        else -> "Barulhento"
    }

    val textoRuido = when {
        !temPermissao -> "Microfone sem permissão"
        !viewModel.focando -> "O ruído será medido durante o foco"
        viewModel.pausado -> "Medição de ruído pausada"
        else -> "Ruído Atual: $ruidoDb dB ($descricaoRuido)"
    }

    // Exibe a tela adequada
    if (viewModel.focando) {

        TelaFocoAtiva(
            materia = viewModel.materia,
            textoRuido = textoRuido,
            tempoRestanteSegundos = viewModel.tempoRestanteSegundos,
            pausado = viewModel.pausado,
            onRetomar = viewModel::retomarFoco,
            onEncerrar = viewModel::encerrarSessao
        )

    } else {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Timer") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Ver Histórico"
                            )
                        }
                    },
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                Text(
                    text = viewModel.materia.ifBlank { "Estudo livre" },
                    style = MaterialTheme.typography.titleLarge
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        Text(
                            text = formatarTempo(viewModel.tempoRestanteSegundos),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (viewModel.emDescanso) {
                            Text(
                                text = "DESCANSO",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Text("Aproveite para descansar. Quando o descanso acabar, vire o celular para o próximo foco.")

                            OutlinedButton(onClick = viewModel::pularDescanso) {
                                Text("Pular descanso")
                            }
                        } else {
                            Text(
                                text = "AGUARDANDO CELULAR",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Text("Vire a tela para baixo na mesa")
                        }

                        if (!viewModel.acelerometroExiste) {
                            Text(
                                text = "Este aparelho não possui acelerômetro",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        HorizontalDivider()

                        Text(textoRuido)

                        if (!temPermissao) {
                            OutlinedButton(
                                onClick = {
                                    pedirPermissao.launch(
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                }
                            ) {
                                Text("Permitir microfone")
                            }
                        }
                    }
                }
                if (viewModel.sessaoConcluida) {
                    Text(
                        text = "Sessão concluída! Parabéns pelos ${viewModel.focoMinutos} minutos de foco.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Tela de foco
@Composable
private fun TelaFocoAtiva(
    materia: String,
    textoRuido: String,
    tempoRestanteSegundos: Int,
    pausado: Boolean,
    onRetomar: () -> Unit,
    onEncerrar: () -> Unit
) {
    val corFundo = MaterialTheme.colorScheme.onBackground
    val corTexto = MaterialTheme.colorScheme.background
    val tempoFormatado = formatarTempo(tempoRestanteSegundos)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(corFundo)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterVertically
        )
    ) {

        if (materia.isNotBlank()) {
            Text(
                text = materia,
                style = MaterialTheme.typography.titleLarge,
                color = corTexto
            )
        }

        Text(
            text = tempoFormatado,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = corTexto
        )

        Text(
            text = if (pausado) "ESTADO: PAUSADO" else "ESTADO: FOCANDO",
            style = MaterialTheme.typography.titleMedium,
            color = corTexto
        )

        Text(
            text = if (pausado) {
                "Vire o celular para baixo por 2 segundos para continuar."
            } else {
                "Foco ativo. Para pausar, desvire o celular. Não saia do aplicativo!"
            },
            color = corTexto
        )

        Text(
            text = textoRuido,
            color = corTexto
        )

        if (pausado) {
            Button(onClick = onRetomar) {
                Text("Retomar")
            }

            TextButton(
                onClick = onEncerrar,
                colors = ButtonDefaults.textButtonColors(contentColor = corTexto)
            ) {
                Text("Encerrar sessão")
            }
        }
    }
}

// segundos -> HH : MM : SS
private fun formatarTempo(totalSegundos: Int): String {
    val horas = totalSegundos / 3600
    val minutos = (totalSegundos % 3600) / 60
    val segundos = totalSegundos % 60

    return "%02d : %02d : %02d".format(horas, minutos, segundos)
}
