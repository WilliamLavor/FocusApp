package com.example.focusapp.ui.history

import com.example.focusapp.data.FocusSession
import com.example.focusapp.data.StatusSessao
import org.junit.Assert.assertEquals
import org.junit.Test

class FiltroHistoricoTest {

    private val dia = 24L * 60 * 60 * 1000
    private val agora = 100 * dia

    private fun sessao(materia: String, local: Int?, diasAtras: Int) = FocusSession(
        materia = materia,
        data = agora - diasAtras * dia,
        duracaoMinutos = 25,
        status = StatusSessao.CONCLUIDO.name,
        mediaDb = 0,
        localId = local
    )

    private val lista = listOf(
        sessao("Cálculo", 1, 1),
        sessao("Redes", 2, 10),
        sessao("calculo numerico", 1, 40)
    )

    @Test
    fun buscaIgnoraMaiusculas() {
        assertEquals(1, filtrarSessoes(lista, "CÁLC", null, 0, agora).size)
    }

    @Test
    fun filtraPorLocal() {
        assertEquals(1, filtrarSessoes(lista, "", 2, 0, agora).size)
    }

    @Test
    fun filtraPorPeriodo() {
        assertEquals(1, filtrarSessoes(lista, "", null, 7, agora).size)
        assertEquals(2, filtrarSessoes(lista, "", null, 30, agora).size)
        assertEquals(3, filtrarSessoes(lista, "", null, 0, agora).size)
    }
}
