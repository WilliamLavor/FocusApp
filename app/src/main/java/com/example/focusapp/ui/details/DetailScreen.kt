package com.example.focusapp.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    sessionId: Int,
    onNavigateBack: () -> Unit
) {
    var materia by remember { mutableStateOf("Matemática Discreta") }
    var notas by remember { mutableStateOf("Revisão de indução matemática e listas") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Sessão") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sessão ID: #$sessionId", style = MaterialTheme.typography.titleMedium)
                    Text("Data: 07/11/2026 às 10:30")
                    Text("Status: CONCLUÍDO")
                    Text("Média acústica: 38 dB")
                }
            }

            OutlinedTextField(
                value = materia,
                onValueChange = { materia = it },
                label = { Text("Nome da Matéria") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text("Anotações do Estudo") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            // Por enquanto os botões só voltam para a tela anterior
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Alterações")
            }

            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Eliminar Registo")
            }
        }
    }
}