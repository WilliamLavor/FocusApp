package com.example.focusapp.ui.metricas

import com.example.focusapp.data.FocusSession
import com.example.focusapp.data.Local
import kotlin.math.roundToInt

// Resumo das sessões de um local (ou do grupo "Sem local")
data class MetricaLocal(
    val nome: String,
    val minutos: Int,
    val mediaDb: Int,           // 0 = sem medição
    val qualidade: Double?,     // média das estrelas (null = nenhuma avaliação)
    val sessoes: Int,
    val temLocal: Boolean
)

// Agrupa as sessões por local. Local que não existe mais conta como "Sem local".
fun calcularMetricas(sessoes: List<FocusSession>, locais: List<Local>): List<MetricaLocal> {
    val nomes = locais.associate { it.id to it.nome }

    return sessoes
        .groupBy { sessao -> sessao.localId?.takeIf { it in nomes } }
        .map { (localId, grupo) ->
            val comRuido = grupo.filter { it.mediaDb > 0 }
            val avaliadas = grupo.filter { it.avaliacao > 0 }

            MetricaLocal(
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
                temLocal = localId != null
            )
        }
        .sortedByDescending { it.minutos }
}

// Melhor nota média; se ninguém avaliou, o local mais usado
fun melhorLocal(metricas: List<MetricaLocal>): MetricaLocal? {
    val comLocal = metricas.filter { it.temLocal }

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
