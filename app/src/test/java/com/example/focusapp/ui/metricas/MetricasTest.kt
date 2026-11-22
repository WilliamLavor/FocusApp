package com.example.focusapp.ui.metricas

import com.example.focusapp.data.FocusSession
import com.example.focusapp.data.Local
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetricasTest {

    private val biblioteca = Local(id = 1, nome = "Biblioteca")
    private val quarto = Local(id = 2, nome = "Quarto")

    private fun sessao(
        local: Int?,
        minutos: Int,
        db: Int = 0,
        nota: Int = 0
    ) = FocusSession(
        materia = "Teste",
        data = 0L,
        duracaoMinutos = minutos,
        status = "CONCLUIDO",
        mediaDb = db,
        avaliacao = nota,
        localId = local
    )

    @Test
    fun agrupaPorLocalSomandoMinutos() {
        val resultado = calcularMetricas(
            listOf(sessao(1, 25), sessao(1, 30), sessao(2, 10)),
            listOf(biblioteca, quarto)
        )

        assertEquals(2, resultado.size)
        assertEquals("Biblioteca", resultado[0].nome)
        assertEquals(55, resultado[0].minutos)
        assertEquals(2, resultado[0].sessoes)
        assertEquals("Quarto", resultado[1].nome)
    }

    @Test
    fun mediaIgnoraSessoesSemRuidoOuSemNota() {
        val resultado = calcularMetricas(
            listOf(
                sessao(1, 25, db = 40, nota = 4),
                sessao(1, 25, db = 50, nota = 5),
                sessao(1, 25, db = 0, nota = 0)
            ),
            listOf(biblioteca)
        )

        assertEquals(45, resultado[0].mediaDb)
        assertEquals(4.5, resultado[0].qualidade!!, 0.001)
    }

    @Test
    fun semNotaAQualidadeEhNula() {
        val resultado = calcularMetricas(listOf(sessao(1, 25)), listOf(biblioteca))

        assertNull(resultado[0].qualidade)
    }

    @Test
    fun localApagadoViraSemLocal() {
        val resultado = calcularMetricas(listOf(sessao(99, 25)), listOf(biblioteca))

        assertEquals("Sem local", resultado[0].nome)
        assertEquals(false, resultado[0].temLocal)
    }

    @Test
    fun melhorLocalEhODeMaiorNota() {
        val metricas = calcularMetricas(
            listOf(
                sessao(1, 100, nota = 3),
                sessao(2, 20, nota = 5)
            ),
            listOf(biblioteca, quarto)
        )

        assertEquals("Quarto", melhorLocal(metricas)?.nome)
    }

    @Test
    fun semNotasMelhorLocalEhOMaisUsado() {
        val metricas = calcularMetricas(
            listOf(sessao(1, 100), sessao(2, 20)),
            listOf(biblioteca, quarto)
        )

        assertEquals("Biblioteca", melhorLocal(metricas)?.nome)
    }

    @Test
    fun semSessoesNaoHaMelhorLocal() {
        assertNull(melhorLocal(calcularMetricas(emptyList(), listOf(biblioteca))))
    }

    @Test
    fun formataHoras() {
        assertEquals("25 min", formatarHoras(25))
        assertEquals("2h", formatarHoras(120))
        assertEquals("1h5min", formatarHoras(65))
    }
}
