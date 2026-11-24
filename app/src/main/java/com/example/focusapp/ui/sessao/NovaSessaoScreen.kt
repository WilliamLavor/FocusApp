package com.example.focusapp.ui.sessao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.data.Local
import com.example.focusapp.ui.focus.DESCANSO_MAX_MINUTOS
import com.example.focusapp.ui.focus.DESCANSO_MIN_MINUTOS
import com.example.focusapp.ui.focus.FOCO_MAX_MINUTOS
import com.example.focusapp.ui.focus.SESSOES_MAX
import com.example.focusapp.ui.focus.SESSOES_MIN
import com.example.focusapp.ui.focus.FOCO_MIN_MINUTOS
import com.example.focusapp.ui.ajustes.Preferencias
import com.example.focusapp.ui.focus.FocusViewModel
import com.example.focusapp.ui.focus.lerMinutos
import com.example.focusapp.ui.metricas.minutosDoDia
import com.example.focusapp.ui.metricas.sequenciaDeDias
import java.time.LocalDate

// matéria, quantas sessões e minutos de foco/descanso
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovaSessaoScreen(
    viewModel: FocusViewModel,
    locais: List<Local>,
    onIniciar: () -> Unit,
    onGerenciarLocais: () -> Unit
) {
    // campos do formulário
    //matéria e local sobrevivem à troca de aba
    var materia by rememberSaveable { mutableStateOf(viewModel.materia) }
    var sessoesTexto by remember { mutableStateOf(viewModel.totalSessoes.toString()) }
    var focoTexto by remember { mutableStateOf(viewModel.focoMinutos.toString()) }
    var descansoTexto by remember { mutableStateOf(viewModel.descansoMinutos.toString()) }
    var localId by rememberSaveable { mutableStateOf(viewModel.localId) }

    val sessoes = lerMinutos(sessoesTexto, SESSOES_MIN, SESSOES_MAX)
    val foco = lerMinutos(focoTexto, FOCO_MIN_MINUTOS, FOCO_MAX_MINUTOS)
    val descanso = lerMinutos(descansoTexto, DESCANSO_MIN_MINUTOS, DESCANSO_MAX_MINUTOS)

    val sessoesValido = sessoes != null
    val focoValido = foco != null
    val descansoValido = descanso != null

    //pro resumo do dia (meta e sequência)
    val context = LocalContext.current
    val fluxoSessoes = remember { AppDatabase.getDatabase(context).focusDao().observarTodas() }
    val todas by fluxoSessoes.collectAsState(initial = emptyList())
    val hoje = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova sessão") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            val meta = Preferencias.meta(context)
            val minutosHoje = minutosDoDia(todas, hoje)
            val sequencia = sequenciaDeDias(todas, hoje)

            if (meta > 0 || minutosHoje > 0 || sequencia > 0) {
                ResumoDoDia(minutosHoje, meta, sequencia)
            }

            OutlinedTextField(
                value = materia,
                onValueChange = { materia = it },
                label = { Text("O que você vai estudar?") },
                placeholder = { Text("Ex: Matemática Discreta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = sessoesTexto,
                onValueChange = { novo ->
                    if (novo.length <= 2 && novo.all { it.isDigit() }) sessoesTexto = novo
                },
                label = { Text("Quantas sessões você quer fazer?") },
                isError = !sessoesValido,
                supportingText = {
                    Text("De $SESSOES_MIN a $SESSOES_MAX. A avaliação vem só no fim de todas.")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = focoTexto,
                    onValueChange = { novo ->
                        if (novo.length <= 3 && novo.all { it.isDigit() }) focoTexto = novo
                    },
                    label = { Text("Foco (min)") },
                    isError = !focoValido,
                    supportingText = {
                        Text("De $FOCO_MIN_MINUTOS a $FOCO_MAX_MINUTOS")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = descansoTexto,
                    onValueChange = { novo ->
                        if (novo.length <= 3 && novo.all { it.isDigit() }) descansoTexto = novo
                    },
                    label = { Text("Descanso (min)") },
                    isError = !descansoValido,
                    supportingText = {
                        Text("De $DESCANSO_MIN_MINUTOS a $DESCANSO_MAX_MINUTOS (0 = sem descanso)")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (sessoes != null && foco != null && sessoesValido && focoValido && sessoes > 1) {
                Text(
                    text = "$sessoes sessões de $foco min = ${sessoes * foco} min de foco, " +
                        "com descanso de ${descanso ?: 0} min entre elas.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Local de estudo",
                style = MaterialTheme.typography.titleMedium
            )

            if (locais.isEmpty()) {
                Text(
                    text = "Nenhum local cadastrado ainda.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = localId == null,
                        onClick = { localId = null },
                        label = { Text("Sem local") }
                    )

                    for (local in locais) {
                        FilterChip(
                            selected = local.id == localId,
                            onClick = { localId = local.id },
                            label = { Text(local.nome) }
                        )
                    }
                }
            }

            TextButton(onClick = onGerenciarLocais) {
                Text("Gerenciar locais")
            }

            Button(
                onClick = {
                    viewModel.configurarSessao(
                        materia,
                        foco ?: 0,
                        descanso ?: 0,
                        sessoes ?: 1,
                        locais.firstOrNull { it.id == localId }
                    )
                    onIniciar()
                },
                enabled = sessoesValido && focoValido && descansoValido,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar sessão")
            }
        }
    }
}
