package com.example.focusapp.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
fun HistoryScreen(
    onNavigateToDetails: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {

    val context = LocalContext.current

    val dao = remember {
        AppDatabase.getDatabase(context).focusDao()
    }

    // Observa as alterações no banco
    val sessoesFlow = remember {
        dao.observarTodas()
    }

    val sessoes by sessoesFlow.collectAsState(
        initial = emptyList()
    )

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico") },

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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),

            contentPadding = PaddingValues(16.dp),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Caso não existam sessões
            if (sessoes.isEmpty()) {

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),

                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhuma sessão registrada.")
                    }
                }

            } else {

                // Exibe cada sessão do banco
                items(
                    items = sessoes,
                    key = { it.id }
                ) { sessao ->

                    SessionCard(
                        sessao = sessao,

                        onClick = {
                            onNavigateToDetails(sessao.id)
                        },

                        onDelete = {
                            scope.launch {
                                dao.excluir(sessao)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    sessao: FocusSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    val dataFormatada = SimpleDateFormat(
        "dd/MM/yyyy HH:mm",
        Locale.getDefault()
    ).format(Date(sessao.data))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = sessao.materia,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = sessao.status
                )

                Text(
                    text = dataFormatada
                )

                Text(
                    text = "${sessao.duracaoMinutos} min • Média: ${sessao.mediaDb} dB"
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir"
                )
            }
        }
    }
}