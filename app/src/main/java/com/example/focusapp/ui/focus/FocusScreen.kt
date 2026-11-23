package com.example.focusapp.ui.focus

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.focusapp.ui.ruido.PainelRuido
import com.example.focusapp.ui.ruido.classificarRuido

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

    //voltar não sai do foco
    BackHandler(enabled = viewModel.focando) { }

    //tela não apaga no foco
    val view = LocalView.current

    DisposableEffect(viewModel.focando) {
        view.keepScreenOn = viewModel.focando

        onDispose {
            view.keepScreenOn = false
        }
    }

    val ruidoDb = viewModel.ruidoDb

    val descricaoRuido = classificarRuido(ruidoDb)

    val textoRuido = when {
        !temPermissao -> "Microfone sem permissão"
        !viewModel.focando -> "O ruído será medido durante o foco"
        viewModel.pausado -> "Medição de ruído pausada"
        else -> "Ruído Atual: $ruidoDb dB ($descricaoRuido)"
    }

    // o anel mostra quanto falta do foco (ou do descanso)
    val totalSegundos = (
        if (viewModel.emDescanso) viewModel.descansoMinutos * 60 else viewModel.focoMinutos * 60
    ).coerceAtLeast(1)
    val progresso = viewModel.tempoRestanteSegundos / totalSegundos.toFloat()

    if (viewModel.focando) {

        TelaFocoAtiva(
            materia = viewModel.materia,
            textoRuido = textoRuido,
            tempoRestanteSegundos = viewModel.tempoRestanteSegundos,
            progresso = progresso,
            pausado = viewModel.pausado,
            rotuloSessao = if (viewModel.totalSessoes > 1) {
                "Sessão ${viewModel.sessaoAtual} de ${viewModel.totalSessoes}"
            } else {
                null
            },
            onRetomar = viewModel::retomarFoco,
            onEncerrar = viewModel::encerrarSessao,
            painelRuido = {
                if (viewModel.leituras.isNotEmpty()) {
                    PainelRuido(
                        leituras = viewModel.leituras,
                        atual = viewModel.ruidoDb,
                        media = viewModel.mediaAtualDb,
                        pico = viewModel.picoDb,
                        minimo = viewModel.minDb,
                        picos = viewModel.picosRuido,
                        rotuloAtual = "Última leitura"
                    )
                }
            }
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

                if (viewModel.localNome.isNotBlank()) {
                    Text(
                        text = "Local: ${viewModel.localNome}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (viewModel.estudoAtivo && viewModel.totalSessoes > 1) {
                    Text(
                        text = "Próxima: sessão ${viewModel.sessaoAtual} de ${viewModel.totalSessoes}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (!viewModel.estudoAtivo && !viewModel.emDescanso) {
                    //o que vai rodar se não voltar na setinha pra mudar
                    val total = viewModel.totalSessoes
                    Text(
                        text = "Serão $total ${if (total == 1) "sessão" else "sessões"} de ${viewModel.focoMinutos} min",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

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

                        AnelTempo(progresso = progresso) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatarTempo(viewModel.tempoRestanteSegundos),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "de ${formatarTempo(totalSegundos)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (viewModel.emDescanso) {
                            Text(
                                text = "DESCANSO",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Text(
                                if (viewModel.estudoAtivo) {
                                    "Quando acabar, a próxima sessão começa sozinha se o celular estiver virado."
                                } else {
                                    "Quando acabar, você avalia o estudo."
                                }
                            )

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

                        // no descanso não tem medição, então some essa parte
                        if (!viewModel.emDescanso) {
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
                }
                if (viewModel.sessaoConcluida) {
                    Text(
                        text = if (viewModel.totalSessoes > 1) {
                            "Estudo concluído! ${viewModel.totalSessoes} sessões de ${viewModel.focoMinutos} min."
                        } else {
                            "Sessão concluída! Foco de ${viewModel.focoMinutos} min completo."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// tela do timer rodando
@Composable
private fun TelaFocoAtiva(
    materia: String,
    textoRuido: String,
    tempoRestanteSegundos: Int,
    progresso: Float,
    pausado: Boolean,
    rotuloSessao: String?,
    onRetomar: () -> Unit,
    onEncerrar: () -> Unit,
    painelRuido: @Composable () -> Unit
) {
    //cores fixas: o modo foco é azul bem escuro com laranja (o tema escuro é só azul)
    val corFundo = Color(0xFF070E18)
    val corTexto = Color(0xFFF2E6D4)
    val corDestaque = Color(0xFFF0B866)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(corFundo)
            .verticalScroll(rememberScrollState())
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
            text = if (pausado) "FOCO PAUSADO" else "MODO FOCO ATIVO",
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 2.sp,
            color = corDestaque
        )

        if (rotuloSessao != null) {
            Text(
                text = rotuloSessao,
                style = MaterialTheme.typography.titleMedium,
                color = corTexto
            )
        }

        AnelTempo(
            progresso = progresso,
            corAnel = corDestaque,
            corTrilho = Color(0xFF3A2D1A)
        ) {
            Text(
                text = formatarTempo(tempoRestanteSegundos),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = corTexto
            )
        }

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
            Button(
                onClick = onRetomar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = corDestaque,
                    contentColor = Color(0xFF3A2400)
                )
            ) {
                Text("Retomar")
            }

            TextButton(
                onClick = onEncerrar,
                colors = ButtonDefaults.textButtonColors(contentColor = corTexto)
            ) {
                Text("Encerrar sessão")
            }

            // sem isso o título e o aviso do painel saem pretos no fundo escuro
            CompositionLocalProvider(LocalContentColor provides corTexto) {
                painelRuido()
            }
        }
    }
}

// segundos -> MM:SS (ou H:MM:SS quando passa de 1 hora)
private fun formatarTempo(totalSegundos: Int): String {
    val horas = totalSegundos / 3600
    val minutos = (totalSegundos % 3600) / 60
    val segundos = totalSegundos % 60

    return if (horas > 0) {
        "%d:%02d:%02d".format(horas, minutos, segundos)
    } else {
        "%02d:%02d".format(minutos, segundos)
    }
}
