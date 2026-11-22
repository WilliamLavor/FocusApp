package com.example.focusapp.ui.locais

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.Local

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocaisScreen(
    locais: List<Local>,
    onSalvar: (Local) -> Unit,
    onExcluir: (Local) -> Unit,
    onNavigateBack: () -> Unit
) {
    // local no diálogo (id 0 = novo) e local pra excluir
    var emEdicao by remember { mutableStateOf<Local?>(null) }
    var paraExcluir by remember { mutableStateOf<Local?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus locais") },
                navigationIcon = {
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { emEdicao = Local(nome = "") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Adicionar local") }
            )
        }
    ) { paddingValues ->

        if (locais.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum local cadastrado. Toque em \"Adicionar local\".")
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = locais, key = { it.id }) { local ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { emEdicao = local }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = local.nome,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                if (local.descricao.isNotBlank()) {
                                    Text(
                                        text = local.descricao,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            IconButton(onClick = { paraExcluir = local }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Excluir"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    emEdicao?.let { atual ->
        DialogoLocal(
            local = atual,
            onConfirmar = { nome, descricao ->
                onSalvar(atual.copy(nome = nome.trim(), descricao = descricao.trim()))
                emEdicao = null
            },
            onCancelar = { emEdicao = null }
        )
    }

    paraExcluir?.let { local ->
        AlertDialog(
            onDismissRequest = { paraExcluir = null },
            title = { Text("Excluir local?") },
            text = {
                Text("As sessões que usaram \"${local.nome}\" continuam no histórico, só ficam sem local.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onExcluir(local)
                        paraExcluir = null
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { paraExcluir = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DialogoLocal(
    local: Local,
    onConfirmar: (String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var nome by remember(local) { mutableStateOf(local.nome) }
    var descricao by remember(local) { mutableStateOf(local.descricao) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (local.id == 0) "Novo local" else "Editar local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    placeholder = { Text("Ex: Biblioteca") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição (opcional)") },
                    placeholder = { Text("Ex: Campus, bloco 2") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(nome, descricao) },
                enabled = nome.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
