package com.example.focusapp.ui.metricas

import com.example.focusapp.data.FocusSession
import com.example.focusapp.data.Local
import kotlin.math.roundToInt

// resumo de um local (ou do grupo Sem local)
data class MetricaLocal(
    val localId: Int?,          // null = grupo Sem local
    val nome: String,
    val minutos: Int,
    val mediaDb: Int,           // 0 = sem medição
    val qualidade: Double?,     // média das estrelas (null = sem nota)
    val sessoes: Int,
    val temLocal: Boolean,
    val ultimaSessao: Long?     // null = nunca estudou lá
)

// agrupa por local, local apagado vira Sem local
// todo local cadastrado entra, mesmo sem sessão
fun calcularMetricas(sessoes: List<FocusSession>, locais: List<Local>): List<MetricaLocal> {
    val nomes = locais.associate { it.id to it.nome }

    val grupos = sessoes.groupBy { sessao -> sessao.localId?.takeIf { it in nomes } }
    val todos = locais.associate { it.id as Int? to emptyList<FocusSession>() } + grupos

    return todos
        .map { (localId, grupo) ->
            val comRuido = grupo.filter { it.mediaDb > 0 }
            val avaliadas = grupo.filter { it.avaliacao > 0 }

            MetricaLocal(
                localId = localId,
                nome = localId?.let { nomes[it] } ?: "Sem local",
                minutos = grupo.sumOf { it.duracaoMinutos },
                mediaDb = if (comRuido.isEmpty()) {
                    0
                } else {
                    comRuido.map { it.mediaDb }.average().roundToInt()
                },
                qualidade = if (avaliadas.isEmpty()) {
                    null
                } else {
                    avaliadas.map { it.avaliacao }.average()
                },
                sessoes = grupo.size,
                temLocal = localId != null,
                ultimaSessao = grupo.maxOfOrNull { it.data }
            )
        }
        .sortedByDescending { it.minutos }
}

//melhor nota média, ou o mais usado se ninguém avaliou
fun melhorLocal(metricas: List<MetricaLocal>): MetricaLocal? {
    val comLocal = metricas.filter { it.temLocal && it.sessoes > 0 }

    return comLocal
        .filter { it.qualidade != null }
        .maxWithOrNull(compareBy<MetricaLocal>({ it.qualidade ?: 0.0 }, { it.minutos }))
        ?: comLocal.maxByOrNull { it.minutos }
}

fun formatarHoras(minutos: Int): String {
    val horas = minutos / 60
    val resto = minutos % 60

    return when {
        horas == 0 -> "$resto min"
        resto == 0 -> "${horas}h"
        else -> "${horas}h${resto}min"
    }
}
