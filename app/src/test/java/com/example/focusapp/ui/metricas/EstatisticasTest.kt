package com.example.focusapp.ui.metricas

import com.example.focusapp.data.FocusSession
import com.example.focusapp.data.StatusSessao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EstatisticasTest {

    private val zona = ZoneId.of("UTC")
    private val hoje = LocalDate.of(2026, 11, 20)

    private fun sessao(
        dia: LocalDate,
        minutos: Int = 25,
        materia: String = "Redes",
        db: Int = 0,
        nota: Int = 0
    ) = FocusSession(
        materia = materia,
        data = dia.atTime(12, 0).atZone(zona).toInstant().toEpochMilli(),
        duracaoMinutos = minutos,
        status = StatusSessao.CONCLUIDO.name,
        mediaDb = db,
        avaliacao = nota
    )

    @Test
    fun sequenciaContaDiasSeguidos() {
        val lista = listOf(sessao(hoje), sessao(hoje.minusDays(1)), sessao(hoje.minusDays(2)))

        assertEquals(3, sequenciaDeDias(lista, hoje, zona))
    }

    @Test
    fun sequenciaQuebraNoDiaSemEstudo() {
        val lista = listOf(sessao(hoje), sessao(hoje.minusDays(1)), sessao(hoje.minusDays(3)))

        assertEquals(2, sequenciaDeDias(lista, hoje, zona))
    }

    @Test
    fun sequenciaAindaValeSeHojeNaoTeveSessao() {
        val lista = listOf(sessao(hoje.minusDays(1)), sessao(hoje.minusDays(2)))

        assertEquals(2, sequenciaDeDias(lista, hoje, zona))
    }

    @Test
    fun semSessoesNaoTemSequencia() {
        assertEquals(0, sequenciaDeDias(emptyList(), hoje, zona))
    }

    @Test
    fun doisDiasSemEstudarZeramASequencia() {
        assertEquals(0, sequenciaDeDias(listOf(sessao(hoje.minusDays(2))), hoje, zona))
    }

    @Test
    fun minutosDoDiaSomaSoOQueEhDoDia() {
        val lista = listOf(sessao(hoje, 25), sessao(hoje, 10), sessao(hoje.minusDays(1), 50))

        assertEquals(35, minutosDoDia(lista, hoje, zona))
    }

    @Test
    fun minutosPorDiaTemSeteDiasComZeroNosVazios() {
        val lista = listOf(sessao(hoje, 30), sessao(hoje.minusDays(2), 20))

        val semana = minutosPorDia(lista, 7, hoje, zona)

        assertEquals(7, semana.size)
        assertEquals(hoje.minusDays(6), semana.first().data)
        assertEquals(hoje, semana.last().data)
        assertEquals(30, semana.last().minutos)
        assertEquals(0, semana[5].minutos)
        assertEquals(20, semana[4].minutos)
    }

    @Test
    fun materiaIgnoraMaiusculaEEspacoNoAgrupamento() {
        val lista = listOf(
            sessao(hoje, 25, materia = "Cálculo"),
            sessao(hoje, 25, materia = " cálculo "),
            sessao(hoje, 10, materia = "Redes")
        )

        val resultado = metricasPorMateria(lista)

        assertEquals(2, resultado.size)
        assertEquals(50, resultado[0].minutos)
        assertEquals(2, resultado[0].sessoes)
    }

    @Test
    fun materiaCalculaNotaEDbMedios() {
        val lista = listOf(
            sessao(hoje, db = 40, nota = 4),
            sessao(hoje, db = 60, nota = 2),
            sessao(hoje)
        )

        val materia = metricasPorMateria(lista)[0]

        assertEquals(50, materia.mediaDb)
        assertEquals(3.0, materia.qualidade!!, 0.001)
    }

    @Test
    fun insightComparaRuidoDasBoasComAsOutras() {
        val lista = listOf(
            sessao(hoje, db = 35, nota = 5),
            sessao(hoje, db = 45, nota = 4),
            sessao(hoje, db = 65, nota = 2)
        )

        val insight = insightRuido(lista)

        assertNotNull(insight)
        assertEquals(40, insight!!.dbBoas)
        assertEquals(65, insight.dbDemais)
        assertEquals(2, insight.qtdBoas)
        assertEquals(1, insight.qtdDemais)
    }

    @Test
    fun insightPrecisaDosDoisGrupos() {
        val soBoas = listOf(sessao(hoje, db = 40, nota = 5), sessao(hoje, db = 42, nota = 4))

        assertNull(insightRuido(soBoas))
        assertNull(insightRuido(emptyList()))
    }

    @Test
    fun insightIgnoraSessaoSemNotaOuSemMedicao() {
        val lista = listOf(
            sessao(hoje, db = 40, nota = 5),
            sessao(hoje, db = 70, nota = 0),
            sessao(hoje, db = 0, nota = 2)
        )

        assertNull(insightRuido(lista))
    }

    @Test
    fun ordenarMaterias_porTempoNotaERuido() {
        val dia = hoje
        val lista = listOf(
            sessao(dia, minutos = 10, materia = "Redes", db = 60, nota = 2),
            sessao(dia, minutos = 30, materia = "Banco", db = 50, nota = 0),
            sessao(dia, minutos = 20, materia = "Mobile", db = 0, nota = 5)
        )
        val materias = metricasPorMateria(lista)

        assertEquals(
            listOf("Banco", "Mobile", "Redes"),
            ordenarMaterias(materias, OrdemLocal.TEMPO).map { it.nome }
        )
        //sem nota vai pro fim
        assertEquals(
            listOf("Mobile", "Redes", "Banco"),
            ordenarMaterias(materias, OrdemLocal.NOTA).map { it.nome }
        )
        //sem medição vai pro fim
        assertEquals(
            listOf("Banco", "Redes", "Mobile"),
            ordenarMaterias(materias, OrdemLocal.RUIDO).map { it.nome }
        )
    }
}
