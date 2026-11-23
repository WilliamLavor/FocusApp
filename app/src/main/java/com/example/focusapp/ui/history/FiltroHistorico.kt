package com.example.focusapp.ui.history

import com.example.focusapp.data.FocusSession

private const val DIA_MS = 24L * 60 * 60 * 1000

// dias = 0 quer dizer sem limite de período
fun filtrarSessoes(
    sessoes: List<FocusSession>,
    busca: String,
    localId: Int?,
    dias: Int,
    agora: Long = System.currentTimeMillis()
): List<FocusSession> {
    val texto = busca.trim()
    val desde = if (dias > 0) agora - dias * DIA_MS else 0L

    return sessoes.filter { sessao ->
        sessao.materia.contains(texto, ignoreCase = true) &&
            (localId == null || sessao.localId == localId) &&
            sessao.data >= desde
    }
}
