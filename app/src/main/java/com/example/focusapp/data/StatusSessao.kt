package com.example.focusapp.data

// no banco continua salvo como texto (name), assim nao muda o schema
enum class StatusSessao(val rotulo: String) {
    CONCLUIDO("Concluído"),
    ABANDONADO("Abandonado");

    companion object {
        // texto vindo do banco -> rotulo bonito (se vier algo estranho mostra cru)
        fun rotuloDe(status: String): String =
            entries.firstOrNull { it.name == status }?.rotulo ?: status
    }
}
