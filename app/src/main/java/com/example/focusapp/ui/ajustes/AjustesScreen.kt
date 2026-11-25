package com.example.focusapp.ui.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.ui.focus.DESCANSO_MAX_MINUTOS
import com.example.focusapp.ui.focus.DESCANSO_MIN_MINUTOS
import com.example.focusapp.ui.focus.FOCO_MAX_MINUTOS
import com.example.focusapp.ui.focus.FOCO_MIN_MINUTOS
import com.example.focusapp.ui.focus.FocusViewModel
import com.example.focusapp.ui.focus.lerMinutos
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    viewModel: FocusViewModel,
    escuro: Boolean,
    onAlternarTema: () -> Unit,
    onMeusLocais: () -> Unit,
    onHistorico: () -> Unit
) {
    val context = LocalContext.current

    var focoTexto by remember { mutableStateOf(Preferencias.foco(context).toString()) }
    var descansoTexto by remember { mutableStateOf(Preferencias.descanso(context).toString()) }
    var metaTexto by remember { mutableStateOf(Preferencias.meta(context).toString()) }
    var salvo by remember { mutableStateOf(false) }

    var confirmarApagar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val foco = lerMinutos(focoTexto, FOCO_MIN_MINUTOS, FOCO_MAX_MINUTOS)
    val descanso = lerMinutos(descansoTexto, DESCANSO_MIN_MINUTOS, DESCANSO_MAX_MINUTOS)
    val meta = lerMinutos(metaTexto, 0, Preferencias.META_MAX_MINUTOS)

    val focoValido = foco != null
    val descansoValido = descanso != null
    val metaValida = meta != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Aparência",
                style = MaterialTheme.typography.titleMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tema escuro")
                    Switch(checked = escuro, onCheckedChange = { onAlternarTema() })
                }
            }

            Text(
                text = "Tempos padrão",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = focoTexto,
                    onValueChange = { novo ->
                        if (novo.length <= 3 && novo.all { it.isDigit() }) {
                            focoTexto = novo
                            salvo = false
                        }
                    },
                    label = { Text("Foco (min)") },
                    isError = !focoValido,
                    supportingText = { Text("De $FOCO_MIN_MINUTOS a $FOCO_MAX_MINUTOS") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = descansoTexto,
                    onValueChange = { novo ->
                        if (novo.length <= 3 && novo.all { it.isDigit() }) {
                            descansoTexto = novo
                            salvo = false
                        }
                    },
                    label = { Text("Descanso (min)") },
                    isError = !descansoValido,
                    supportingText = { Text("De $DESCANSO_MIN_MINUTOS a $DESCANSO_MAX_MINUTOS") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = metaTexto,
                onValueChange = { novo ->
                    if (novo.length <= 3 && novo.all { it.isDigit() }) {
                        metaTexto = novo
                        salvo = false
                    }
                },
                label = { Text("Meta diária (min)") },
                isError = !metaValida,
                supportingText = { Text("De 0 a ${Preferencias.META_MAX_MINUTOS} (0 = sem meta)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    Preferencias.salvar(context, foco ?: 0, descanso ?: 0, meta ?: 0)
                    viewModel.aplicarPadroes(foco ?: 0, descanso ?: 0)
                    salvo = true
                },
                enabled = focoValido && descansoValido && metaValida,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (salvo) "Salvo!" else "Salvar")
            }

            Text(
                text = "Atalhos",
                style = MaterialTheme.typography.titleMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    LinhaAtalho("Meus locais", onMeusLocais)
                    LinhaAtalho("Histórico de sessões", onHistorico)
                }
            }

            Text(
                text = "Dados",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedButton(
                onClick = { confirmarApagar = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apagar histórico")
            }
        }
    }

    if (confirmarApagar) {
        AlertDialog(
            onDismissRequest = { confirmarApagar = false },
            title = { Text("Apagar todas as sessões?") },
            text = { Text("O histórico inteiro some e não dá pra desfazer. Seus locais continuam.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarApagar = false
                    scope.launch {
                        AppDatabase.getDatabase(context).focusDao().excluirTodas()
                    }
                }) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarApagar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun LinhaAtalho(texto: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(texto)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
