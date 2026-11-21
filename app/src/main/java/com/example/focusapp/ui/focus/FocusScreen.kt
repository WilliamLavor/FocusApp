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
import com.example.focusapp.sensor.SensorProximidade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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

    // Inicia uma sessão apenas uma vez
    fun iniciarFoco() {
        if (!focando) {
            inicioSessao = System.currentTimeMillis()
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

    LaunchedEffect(temPermissao, appVisivel, focando) {

        if (temPermissao && appVisivel && focando) {

            val medidor = MedidorRuido()
            medidor.iniciar()

            try {
                while (true) {

                    val leitura = withContext(Dispatchers.IO) {
                        medidor.lerDb()
                    }

                    ruidoDb = leitura
                    somaRuido += leitura
                    quantidadeMedicoes++

                    delay(500)
                }

            } finally {
                medidor.parar()
            }
        }
    }

    // Sensores
    val sensorProximidade = remember {
        SensorProximidade(context)
    }


    // Ciclo de vida do aplicativo
    DisposableEffect(activity) {

        sensorProximidade.aoMudar = { perto ->
            if (perto) iniciarFoco()
        }



        val observador = LifecycleEventObserver { _, evento ->

            if (evento == Lifecycle.Event.ON_START) {
                appVisivel = true

                sensorProximidade.iniciar()

            }

            if (evento == Lifecycle.Event.ON_STOP) {

                appVisivel = false

                sensorProximidade.parar()


                // Se havia uma sessão ativa, salvamos como abandonada
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

                    // Impede que a mesma sessão seja salva novamente
                    inicioSessao = 0L
                }

                focando = false
            }
        }

        activity.lifecycle.addObserver(observador)

        onDispose {
            activity.lifecycle.removeObserver(observador)

            sensorProximidade.parar()

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
            textoRuido = textoRuido
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

                        if (!sensorProximidade.existe) {
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
    textoRuido: String
) {
    val corFundo = MaterialTheme.colorScheme.onBackground
    val corTexto = MaterialTheme.colorScheme.background

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
            text = "00 : 25 : 00",
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