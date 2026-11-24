package com.example.focusapp.ui.metricas

import com.example.focusapp.data.FocusSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class DiaMinutos(val data: LocalDate, val minutos: Int)

data class MetricaMateria(
    val nome: String,
    val minutos: Int,
    val sessoes: Int,
    val mediaDb: Int,
    val qualidade: Double?
)

data class InsightRuido(
    val dbBoas: Int,        // média de dB das sessões bem avaliadas
    val dbDemais: Int,
    val qtdBoas: Int,
    val qtdDemais: Int
)

private fun diaDe(sessao: FocusSession, zona: ZoneId): LocalDate =
    Instant.ofEpochMilli(sessao.data).atZone(zona).toLocalDate()

fun minutosDoDia(
    sessoes: List<FocusSession>,
    dia: LocalDate,
    zona: ZoneId = ZoneId.systemDefault()
): Int = sessoes.filter { diaDe(it, zona) == dia }.sumOf { it.duracaoMinutos }

// do dia mais antigo pro de hoje, dia sem estudo vem com 0
fun minutosPorDia(
    sessoes: List<FocusSession>,
    dias: Int = 7,
    hoje: LocalDate = LocalDate.now(),
    zona: ZoneId = ZoneId.systemDefault()
): List<DiaMinutos> {
    val porDia = sessoes.groupBy { diaDe(it, zona) }

    return (dias - 1 downTo 0).map { atras ->
        val dia = hoje.minusDays(atras.toLong())
        DiaMinutos(dia, porDia[dia]?.sumOf { it.duracaoMinutos } ?: 0)
    }
}

// dias seguidos estudando; se hoje ainda não teve sessão, conta até ontem
fun sequenciaDeDias(
    sessoes: List<FocusSession>,
    hoje: LocalDate = LocalDate.now(),
    zona: ZoneId = ZoneId.systemDefault()
): Int {
    val dias = sessoes
        .filter { it.duracaoMinutos > 0 }
        .map { diaDe(it, zona) }
        .toSet()

    var dia = if (hoje in dias) hoje else hoje.minusDays(1)
    var total = 0

    while (dia in dias) {
        total++
        dia = dia.minusDays(1)
    }

    return total
}

// matéria "calculo" e "Cálculo " no mesmo grupo só se mudar maiúscula/espaço
fun metricasPorMateria(sessoes: List<FocusSession>): List<MetricaMateria> =
    sessoes
        .groupBy { it.materia.trim().lowercase() }
        .map { (_, grupo) ->
            val comRuido = grupo.filter { it.mediaDb > 0 }
            val avaliadas = grupo.filter { it.avaliacao > 0 }

            MetricaMateria(
                nome = grupo.first().materia.trim().ifBlank { "Estudo livre" },
                minutos = grupo.sumOf { it.duracaoMinutos },
                sessoes = grupo.size,
                mediaDb = if (comRuido.isEmpty()) 0 else comRuido.map { it.mediaDb }.average().roundToInt(),
                qualidade = if (avaliadas.isEmpty()) null else avaliadas.map { it.avaliacao }.average()
            )
        }
        .sortedByDescending { it.minutos }

// compara o ruído das sessões nota 4-5 com as outras (só as que têm nota e medição)
fun insightRuido(sessoes: List<FocusSession>): InsightRuido? {
    val validas = sessoes.filter { it.avaliacao > 0 && it.mediaDb > 0 }

    val boas = validas.filter { it.avaliacao >= 4 }
    val demais = validas.filter { it.avaliacao < 4 }

    if (boas.isEmpty() || demais.isEmpty()) return null

    return InsightRuido(
        dbBoas = boas.map { it.mediaDb }.average().roundToInt(),
        dbDemais = demais.map { it.mediaDb }.average().roundToInt(),
        qtdBoas = boas.size,
        qtdDemais = demais.size
    )
}
