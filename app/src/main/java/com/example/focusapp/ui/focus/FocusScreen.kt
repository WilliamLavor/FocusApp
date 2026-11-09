package com.example.focusapp.ui.focus

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.focusapp.sensor.MedidorRuido
import com.example.focusapp.sensor.SensorProximidade
import com.example.focusapp.sensor.SensorVirado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onNavigateToHistory: () -> Unit
) {
    // Estados locais temporários para controlar a UI (State Hoisting virá com o ViewModel depois)
    var materia by remember { mutableStateOf("") }
    var focando by remember { mutableStateOf(false) }       // true = celular virado para baixo
    var appVisivel by remember { mutableStateOf(true) }     // false = app foi para segundo plano

    val context = LocalContext.current
    val activity = context as ComponentActivity

    // ---------- Microfone: permissão ----------
    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
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

    // ---------- Microfone: leitura do ruído ----------
    var ruidoDb by remember { mutableStateOf(0) }

    // Só mede quando tem permissão E o app está visível.
    // Se uma das duas coisas mudar, o loop antigo é cancelado e o "finally" libera o microfone.
    LaunchedEffect(temPermissao, appVisivel) {
        if (temPermissao && appVisivel) {
            val medidor = MedidorRuido()
            medidor.iniciar()
            try {
                while (true) {
                    ruidoDb = withContext(Dispatchers.IO) { medidor.lerDb() }
                    delay(500)
                }
            } finally {
                medidor.parar()
            }
        }
    }

    // ---------- Sensor de proximidade + ciclo de vida ----------
    val sensorProximidade = remember { SensorProximidade(context) }
    val sensorVirado = remember { SensorVirado(context) } // acelerômetro: funciona mesmo com capinha alta

    DisposableEffect(activity) {
        // O sensor só INICIA o foco: depois que começou, desvirar o celular não encerra
        sensorProximidade.aoMudar = { perto -> if (perto) focando = true }
        sensorVirado.aoMudar = { virado -> if (virado) focando = true }

        // Observa o ciclo de vida da Activity
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_START) {
                appVisivel = true
                sensorProximidade.iniciar()
                sensorVirado.iniciar()
            }
            if (evento == Lifecycle.Event.ON_STOP) {
                appVisivel = false
                sensorProximidade.parar()
                sensorVirado.parar()
                focando = false // saiu do app durante o foco: a sessão foi abandonada
            }
        }
        activity.lifecycle.addObserver(observador)

        onDispose {
            activity.lifecycle.removeObserver(observador)
            sensorProximidade.parar()
            sensorVirado.parar()
        }
    }

    // ---------- Regras durante o foco ----------
    // 1) O botão voltar não faz nada
    BackHandler(enabled = focando) { }

    // 2) A tela não apaga sozinha (se apagasse, o app sairia de cena)
    val view = LocalView.current
    DisposableEffect(focando) {
        view.keepScreenOn = focando
        onDispose { view.keepScreenOn = false }
    }

    // ---------- Textos ----------
    val descricaoRuido = when {
        ruidoDb < 40 -> "Silencioso"
        ruidoDb < 60 -> "Moderado"
        else -> "Barulhento"
    }
    val textoRuido = if (temPermissao) {
        "Ruído Atual: $ruidoDb dB ($descricaoRuido)"
    } else {
        "Microfone sem permissão"
    }

    // ---------- Qual tela mostrar ----------
    if (focando) {
        TelaFocoAtiva(materia = materia, textoRuido = textoRuido)
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

                // 1. Campo da Matéria
                OutlinedTextField(
                    value = materia,
                    onValueChange = { materia = it },
                    label = { Text("Matéria") },
                    placeholder = { Text("Ex: Matemática Discreta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 2. Card Central (estado de espera)
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
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

                        Text(
                            text = "Vire a tela para baixo na mesa",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (!sensorProximidade.existe && !sensorVirado.existe) {
                            Text(
                                text = "Este aparelho não tem sensores para detectar o celular virado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        HorizontalDivider()

                        Text(
                            text = textoRuido,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (!temPermissao) {
                            OutlinedButton(
                                onClick = { pedirPermissao.launch(Manifest.permission.RECORD_AUDIO) }
                            ) {
                                Text("Permitir microfone")
                            }
                        }
                    }
                }

                // 3. Controle manual
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

// Tela de foco: cores invertidas e nenhum botão (não há nada para tocar)
@Composable
private fun TelaFocoAtiva(
    materia: String,
    textoRuido: String
) {
    // Inversão: o fundo usa a cor que seria do texto, e o texto usa a cor que seria do fundo
    val corFundo = MaterialTheme.colorScheme.onBackground
    val corTexto = MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(corFundo)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
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
            style = MaterialTheme.typography.bodyMedium,
            color = corTexto
        )

        Text(
            text = textoRuido,
            style = MaterialTheme.typography.bodyMedium,
            color = corTexto
        )
    }
}