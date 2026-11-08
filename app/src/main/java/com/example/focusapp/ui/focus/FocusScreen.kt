package com.example.focusapp.ui.focus

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.focusapp.sensor.MedidorRuido
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
    var isFocando by remember { mutableStateOf(false) }

    // ---------- Microfone: permissão ----------
    val context = LocalContext.current

    // Começa com o resultado da checagem: já temos a permissão ou não?
    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // Abre a janelinha do Android pedindo a permissão e guarda a resposta
    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        temPermissao = concedida
    }

    // Ao abrir a tela, pede a permissão se ainda não tiver
    LaunchedEffect(Unit) {
        if (!temPermissao) {
            pedirPermissao.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ---------- Microfone: leitura do ruído ----------
    var ruidoDb by remember { mutableStateOf(0) }

    // Roda quando a tela abre (e de novo se a permissão mudar).
    // Quando a tela sai da composição, o loop é cancelado e o "finally" libera o microfone.
    LaunchedEffect(temPermissao) {
        if (temPermissao) {
            val medidor = MedidorRuido()
            medidor.iniciar()
            try {
                while (true) {
                    // Leitura em outra thread (IO) para não travar a tela
                    ruidoDb = withContext(Dispatchers.IO) { medidor.lerDb() }
                    delay(500) // atualiza duas vezes por segundo
                }
            } finally {
                medidor.parar()
            }
        }
    }

    // Texto que descreve o nível de ruído
    val descricaoRuido = when {
        ruidoDb < 40 -> "Silencioso"
        ruidoDb < 60 -> "Moderado"
        else -> "Barulhento"
    }

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

            // 2. Card Central de Foco
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
                    // Temporizador
                    Text(
                        text = "00 : 25 : 00",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Estado do Sensor de Proximidade
                    Text(
                        text = if (isFocando) "ESTADO: FOCANDO" else "AGUARDANDO CELULAR",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isFocando) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )

                    Text(
                        text = if (isFocando) "Foco ativo. Não saia do aplicativo!" else "Vire a tela para baixo na mesa",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider()

                    // Leitura do Microfone
                    if (temPermissao) {
                        Text(
                            text = "Ruído Atual: $ruidoDb dB ($descricaoRuido)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Microfone sem permissão",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = { pedirPermissao.launch(Manifest.permission.RECORD_AUDIO) }
                        ) {
                            Text("Permitir microfone")
                        }
                    }
                }
            }

            // 3. Controlos Manuais
            Button(
                onClick = { isFocando = !isFocando }, // Alterna o estado visual para você testar
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isFocando) "Parar Foco" else "Iniciar Manualmente")
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