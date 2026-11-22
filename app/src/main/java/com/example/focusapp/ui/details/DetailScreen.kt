package com.example.focusapp.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.data.FocusSession
import com.example.focusapp.ui.avaliacao.FATORES
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    sessionId: Int,
    onNavigateBack: () -> Unit
) {

    val context = LocalContext.current

    val dao = remember {
        AppDatabase.getDatabase(context).focusDao()
    }

    val scope = rememberCoroutineScope()

    // Sessão encontrada no banco
    var sessao by remember {
        mutableStateOf<FocusSession?>(null)
    }

    var nomeLocal by remember {
        mutableStateOf<String?>(null)
    }

    var carregando by remember {
        mutableStateOf(true)
    }

    // Campos editáveis
    var materia by remember {
        mutableStateOf("")
    }

    var notas by remember {
        mutableStateOf("")
    }

    var estrelas by remember { mutableIntStateOf(0) }

    val fatoresEscolhidos = remember { mutableStateListOf<String>() }

    var confirmarExclusao by remember { mutableStateOf(false) }

    // Busca a sessão pelo ID
    LaunchedEffect(sessionId) {

        val encontrada = dao.buscarPorId(sessionId)

        sessao = encontrada

        nomeLocal = encontrada?.localId?.let { id ->
            AppDatabase.getDatabase(context).localDao().buscarPorId(id)?.nome
        }

        materia = encontrada?.materia ?: ""
        notas = encontrada?.notas ?: ""
        estrelas = encontrada?.avaliacao ?: 0

        fatoresEscolhidos.clear()
        encontrada?.fatores?.split(",")?.filter { it.isNotBlank() }?.let { fatoresEscolhidos.addAll(it) }

        carregando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Detalhes da Sessão")
                },

                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
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

        if (carregando) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),

                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (sessao == null) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),

                contentAlignment = Alignment.Center
            ) {
                Text("Sessão não encontrada.")
            }

        } else {

            val atual = sessao!!

            val dataFormatada = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).format(Date(atual.data))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),

                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Informações da sessão
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            "Sessão ID: #${atual.id}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Data: $dataFormatada")

                        Text("Status: ${atual.status}")

                        Text("Duração: ${atual.duracaoMinutos} minutos")

                        Text("Média acústica: ${atual.mediaDb} dB")

                        nomeLocal?.let { Text("Local: $it") }

                        if (atual.picoDb > 0) {
                            Text("Pico: ${atual.picoDb} dB • Mínimo: ${atual.minDb} dB")
                        }

                        if (atual.avaliacao > 0) {
                            Text("Avaliação: ${atual.avaliacao}/5")
                        }

                        if (atual.fatores.isNotBlank()) {
                            Text("Influenciou: ${atual.fatores.replace(",", ", ")}")
                        }
                    }
                }

                // Editar matéria
                OutlinedTextField(
                    value = materia,
                    onValueChange = { materia = it },
                    label = {
                        Text("Nome da Matéria")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Editar avaliação
                Text(
                    text = "Avaliação",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
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

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (fator in FATORES) {
                        FilterChip(
                            selected = fator in fatoresEscolhidos,
                            onClick = {
                                if (fator in fatoresEscolhidos) fatoresEscolhidos.remove(fator)
                                else fatoresEscolhidos.add(fator)
                            },
                            label = { Text(fator) }
                        )
                    }
                }

                // Editar anotações
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = {
                        Text("Anotações do Estudo")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                // Salvar alterações
                Button(
                    onClick = {
                        scope.launch {

                            dao.atualizar(
                                atual.copy(
                                    materia = materia.trim(),
                                    notas = notas,
                                    avaliacao = estrelas,
                                    fatores = fatoresEscolhidos.joinToString(",")
                                )
                            )

                            onNavigateBack()
                        }
                    },

                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar alterações")
                }

                // Excluir sessão
                TextButton(
                    onClick = { confirmarExclusao = true },

                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Excluir sessão")
                }
            }

            if (confirmarExclusao) {
                AlertDialog(
                    onDismissRequest = { confirmarExclusao = false },
                    title = { Text("Excluir sessão?") },
                    text = { Text("Não dá pra desfazer depois.") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmarExclusao = false
                            scope.launch {
                                dao.excluir(atual)
                                onNavigateBack()
                            }
                        }) { Text("Excluir") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") }
                    }
                )
            }
        }
    }
}