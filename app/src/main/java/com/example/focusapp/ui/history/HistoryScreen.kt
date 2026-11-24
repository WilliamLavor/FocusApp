package com.example.focusapp.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.data.FocusSession
import com.example.focusapp.ui.componentes.EstadoVazio
import com.example.focusapp.data.StatusSessao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    onNavigateToDetails: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {

    val context = LocalContext.current

    val dao = remember {
        AppDatabase.getDatabase(context).focusDao()
    }

    val sessoesFlow = remember {
        dao.observarTodas()
    }

    val sessoes by sessoesFlow.collectAsState(
        initial = emptyList()
    )

    //nomes dos locais pro card
    val locaisFlow = remember {
        AppDatabase.getDatabase(context).localDao().observarTodos()
    }

    val locais by locaisFlow.collectAsState(initial = emptyList())

    val nomesLocais = locais.associate { it.id to it.nome }

    val scope = rememberCoroutineScope()

    //filtros (dias 0 = tudo)
    var busca by remember { mutableStateOf("") }
    var filtroLocal by remember { mutableStateOf<Int?>(null) }
    var filtroDias by remember { mutableIntStateOf(0) }

    val filtradas = remember(sessoes, busca, filtroLocal, filtroDias) {
        filtrarSessoes(sessoes, busca, filtroLocal, filtroDias)
    }

    //sessao que ta esperando confirmacao pra excluir
    var paraExcluir by remember { mutableStateOf<FocusSession?>(null) }

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

            //lista vazia
            if (sessoes.isEmpty()) {

                item {
                    EstadoVazio(
                        icone = Icons.Default.History,
                        titulo = "Nenhuma sessão ainda",
                        dica = "Faça uma sessão na aba Timer e ela aparece aqui."
                    )
                }

            } else {

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        OutlinedTextField(
                            value = busca,
                            onValueChange = { busca = it },
                            label = { Text("Buscar matéria") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for ((dias, nome) in listOf(0 to "Tudo", 7 to "7 dias", 30 to "30 dias")) {
                                FilterChip(
                                    selected = filtroDias == dias,
                                    onClick = { filtroDias = dias },
                                    label = { Text(nome) }
                                )
                            }
                        }

                        if (locais.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = filtroLocal == null,
                                    onClick = { filtroLocal = null },
                                    label = { Text("Todos os locais") }
                                )

                                for (local in locais) {
                                    FilterChip(
                                        selected = filtroLocal == local.id,
                                        onClick = { filtroLocal = local.id },
                                        label = { Text(local.nome) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (filtradas.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhuma sessão com esses filtros.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                items(
                    items = filtradas,
                    key = { it.id }
                ) { sessao ->

                    SessionCard(
                        sessao = sessao,

                        nomeLocal = sessao.localId?.let { nomesLocais[it] },

                        onClick = {
                            onNavigateToDetails(sessao.id)
                        },

                        onDelete = { paraExcluir = sessao }
                    )
                }
            }
        }
    }

    paraExcluir?.let { alvo ->
        AlertDialog(
            onDismissRequest = { paraExcluir = null },
            title = { Text("Excluir sessão?") },
            text = { Text("A sessão de ${alvo.materia} será apagada e não dá pra desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.excluir(alvo) }
                    paraExcluir = null
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { paraExcluir = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SessionCard(
    sessao: FocusSession,
    nomeLocal: String?,
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
                    text = StatusSessao.rotuloDe(sessao.status)
                )

                Text(
                    text = dataFormatada
                )

                Text(
                    text = "${sessao.duracaoMinutos} min • Média: ${sessao.mediaDb} dB"
                )

                if (nomeLocal != null) {
                    Text(
                        text = "Local: $nomeLocal"
                    )
                }

                if (sessao.avaliacao > 0) {
                    Text(
                        text = "Avaliação: ${sessao.avaliacao}/5"
                    )
                }
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