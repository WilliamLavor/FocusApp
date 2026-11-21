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
import androidx.lifecycle.lifecycleScope
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.data.FocusSession
import com.example.focusapp.sensor.MedidorRuido
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.example.focusapp.sensor.SensorVirado
import android.os.SystemClock


private const val DURACAO_SESSAO_SEGUNDOS = 25 * 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onNavigateToHistory: () -> Unit
) {
    var materia by remember { mutableStateOf("") }
    var focando by remember { mutableStateOf(false) }
    var appVisivel by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val activity = context as ComponentActivity

    // Acesso ao banco
    val banco = remember {
        AppDatabase.getDatabase(context)
    }

    val dao = remember {
        banco.focusDao()
    }

    // Dados da sessão atual
    var inicioSessao by remember { mutableLongStateOf(0L) }
    var somaRuido by remember { mutableIntStateOf(0) }
    var quantidadeMedicoes by remember { mutableIntStateOf(0) }

    var inicioCronometroMs by remember {
        mutableLongStateOf(0L)
    }

    var tempoRestanteSegundos by remember {
        mutableIntStateOf(DURACAO_SESSAO_SEGUNDOS)
    }

    var sessaoConcluida by remember {
        mutableStateOf(false)
    }



    // Inicia uma sessão apenas uma vez
    fun iniciarFoco() {
        if (!focando) {
            inicioSessao = System.currentTimeMillis()

            inicioCronometroMs = SystemClock.elapsedRealtime()

            tempoRestanteSegundos = DURACAO_SESSAO_SEGUNDOS

            sessaoConcluida = false

            somaRuido = 0
            quantidadeMedicoes = 0

            focando = true
        }
    }

    // Permissão do microfone
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

    // Medição do ruído durante o foco
    var ruidoDb by remember { mutableIntStateOf(0) }

    // Medição contínua do ruído durante o foco
    LaunchedEffect(temPermissao, appVisivel, focando) {

        if (temPermissao && appVisivel && focando) {

            val medidor = MedidorRuido()
            medidor.iniciar()

            try {
                while (true) {

                    val leitura = withContext(Dispatchers.IO) {
                        medidor.lerDb()
                    }

                    // Atualiza o valor exibido na tela.
                    ruidoDb = leitura

                    // Acumula as medições para calcular a média da sessão.
                    somaRuido += leitura
                    quantidadeMedicoes++

                    delay(500)
                }

            } finally {
                medidor.parar()
            }
        }
    }

    // Cronômetro de 25 minutos
    LaunchedEffect(focando) {

        if (focando && inicioCronometroMs > 0L) {

            val inicio = inicioCronometroMs
            val dataInicio = inicioSessao

            while (true) {

                val tempoDecorrido = (
                        (SystemClock.elapsedRealtime() - inicio) / 1000L
                        ).toInt()

                val restante = (
                        DURACAO_SESSAO_SEGUNDOS - tempoDecorrido
                        ).coerceAtLeast(0)

                tempoRestanteSegundos = restante

                // O tempo acabou!
                if (restante == 0) {

                    val media = if (quantidadeMedicoes > 0) {
                        somaRuido / quantidadeMedicoes
                    } else {
                        0
                    }

                    val sessao = FocusSession(
                        materia = materia.ifBlank { "Estudo livre" },
                        data = dataInicio,
                        duracaoMinutos = 25,
                        status = "CONCLUIDO",
                        mediaDb = media
                    )

                    // Impede que o ON_STOP salve a mesma sessão
                    // novamente como abandonada.
                    inicioSessao = 0L
                    inicioCronometroMs = 0L

                    sessaoConcluida = true
                    focando = false
                    inicioCronometroMs = 0L
                    tempoRestanteSegundos = DURACAO_SESSAO_SEGUNDOS

                    // Salva no Room.
                    activity.lifecycleScope.launch {
                        dao.inserir(sessao)
                    }

                    break
                }

                delay(1000)
            }
        }
    }

    // Sensores
    // Acelerômetro
    val sensorVirado = remember {
        SensorVirado(context)
    }


    // Ciclo de vida do aplicativo
    DisposableEffect(activity) {

        // Só inicia o foco quando o acelerômetro confirma a posição.
        sensorVirado.aoMudar = { virado ->
            if (virado) {
                iniciarFoco()
            }
        }

        val observador = LifecycleEventObserver { _, evento ->

            if (evento == Lifecycle.Event.ON_START) {
                appVisivel = true
                sensorVirado.iniciar()
            }

            if (evento == Lifecycle.Event.ON_STOP) {

                appVisivel = false
                sensorVirado.parar()

                // Se havia uma sessão ativa, salvamos como abandonada.
                if (inicioSessao > 0L) {

                    val agora = System.currentTimeMillis()

                    val duracao = (
                            (agora - inicioSessao) / 60000.0
                            ).roundToInt().coerceAtLeast(1)

                    val media = if (quantidadeMedicoes > 0) {
                        somaRuido / quantidadeMedicoes
                    } else {
                        0
                    }

                    val sessao = FocusSession(
                        materia = materia.ifBlank { "Estudo livre" },
                        data = inicioSessao,
                        duracaoMinutos = duracao,
                        status = "ABANDONADO",
                        mediaDb = media
                    )

                    activity.lifecycleScope.launch {
                        dao.inserir(sessao)
                    }

                    inicioSessao = 0L
                }

                focando = false
            }
        }

        activity.lifecycle.addObserver(observador)

        onDispose {
            activity.lifecycle.removeObserver(observador)
            sensorVirado.parar()
        }
    }
    // Impede o botão voltar durante o foco
    BackHandler(enabled = focando) { }

    // Mantém a tela ligada durante o foco
    val view = LocalView.current

    DisposableEffect(focando) {
        view.keepScreenOn = focando

        onDispose {
            view.keepScreenOn = false
        }
    }

    val descricaoRuido = when {
        ruidoDb < 40 -> "Silencioso"
        ruidoDb < 60 -> "Moderado"
        else -> "Barulhento"
    }

    val textoRuido = when {
        !temPermissao -> "Microfone sem permissão"
        !focando -> "O ruído será medido durante o foco"
        else -> "Ruído Atual: $ruidoDb dB ($descricaoRuido)"
    }

    // Exibe a tela adequada
    if (focando) {

        TelaFocoAtiva(
            materia = materia,
            textoRuido = textoRuido,
            tempoRestanteSegundos = tempoRestanteSegundos
        )

    } else {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("FocusApp") },
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

                OutlinedTextField(
                    value = materia,
                    onValueChange = { materia = it },
                    label = { Text("Matéria") },
                    placeholder = {
                        Text("Ex: Matemática Discreta")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                            text = "00 : 25 : 00",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "AGUARDANDO CELULAR",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Text("Vire a tela para baixo na mesa")

                        if (!sensorVirado.existe) {
                            Text(
                                text = "Este aparelho não possui sensor de proximidade",
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
                if (sessaoConcluida) {
                    Text(
                        text = "Sessão concluída! Parabéns pelos 25 minutos de foco.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = { materia = "" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Descartar Sessão")
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
    tempoRestanteSegundos: Int
) {
    val corFundo = MaterialTheme.colorScheme.onBackground
    val corTexto = MaterialTheme.colorScheme.background
    val horas = tempoRestanteSegundos / 3600
    val minutos = (tempoRestanteSegundos % 3600) / 60
    val segundos = tempoRestanteSegundos % 60

    val tempoFormatado = "%02d : %02d : %02d".format(
        horas,
        minutos,
        segundos
    )

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
            text = "ESTADO: FOCANDO",
            style = MaterialTheme.typography.titleMedium,
            color = corTexto
        )

        Text(
            text = "Foco ativo. Não saia do aplicativo!",
            color = corTexto
        )

        Text(
            text = textoRuido,
            color = corTexto
        )
    }
}