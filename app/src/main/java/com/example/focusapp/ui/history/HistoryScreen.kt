package com.example.focusapp.ui.history // Ajuste para o seu pacote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// 1. Classe de dados temporária (Será substituída pela Entidade do Room no futuro)
data class FocusSessionMock(
    val id: Int,
    val materia: String,
    val duration: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetails: (sessionId: Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    // 2. Lista simulada de sessões concluídas
    val mockSessions = listOf(
        FocusSessionMock(id = 1, materia = "Matemática Discreta", duration = "25:00", date = "10 Nov 2026"),
        FocusSessionMock(id = 2, materia = "Sistemas Distribuídos", duration = "50:00", date = "09 Nov 2026"),
        FocusSessionMock(id = 3, materia = "Computação Gráfica", duration = "15:30", date = "08 Nov 2026")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Foco") },
                navigationIcon = {
                    // Botão de voltar na barra superior
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

        // 3. LazyColumn para renderizar a lista de forma eficiente
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // A função 'items' cria um Card para cada elemento da nossa lista
            items(mockSessions) { session ->
                SessionCard(
                    session = session,
                    // Ao clicar no Card, disparamos a navegação passando o ID da sessão
                    onClick = { onNavigateToDetails(session.id) }
                )
            }
        }
    }
}

// 4. Componente visual isolado para cada item da lista (Deixa o código mais limpo)
@Composable
fun SessionCard(
    session: FocusSessionMock,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Torna o card clicável
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.materia,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = session.date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = session.duration,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}