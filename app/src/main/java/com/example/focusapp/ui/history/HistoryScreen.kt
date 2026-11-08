package com.example.focusapp.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetails: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
            item {
                SessionCard(
                    materia = "Matemática Discreta",
                    status = "SUCESSO",
                    info = "07/11/2026 - 45 min • Média: 38 dB",
                    onClick = { onNavigateToDetails(12) }
                )
            }
            item {
                SessionCard(
                    materia = "Estruturas de Dados",
                    status = "ABANDONADO",
                    info = "06/11/2026 - 12 min • Média: 55 dB",
                    onClick = { onNavigateToDetails(11) }
                )
            }
        }
    }
}

@Composable
private fun SessionCard(
    materia: String,
    status: String,
    info: String,
    onClick: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(materia, style = MaterialTheme.typography.titleMedium)
                Text(status)
                Text(info)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir")
            }
        }
    }
}