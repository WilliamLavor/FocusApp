package com.example.focusapp.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.data.FocusSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    // Busca a sessão pelo ID
    LaunchedEffect(sessionId) {

        val encontrada = dao.buscarPorId(sessionId)

        sessao = encontrada

        materia = encontrada?.materia ?: ""
        notas = encontrada?.notas ?: ""

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
                                    notas = notas
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
                    onClick = {
                        scope.launch {

                            dao.excluir(atual)

                            onNavigateBack()
                        }
                    },

                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Excluir sessão")
                }
            }
        }
    }
}