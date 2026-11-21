package com.example.focusapp.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sessoes")
data class FocusSession(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val materia: String,

    val data: Long,

    val duracaoMinutos: Int,

    val status: String,

    val mediaDb: Int,

    val notas: String = "",

    // Avaliação de 1 a 5 estrelas (0 = ainda não avaliada)
    val avaliacao: Int = 0,

    // Fatores escolhidos na avaliação, separados por vírgula (ex.: "Silêncio,Conforto")
    val fatores: String = "",

    // Maior e menor nível de ruído medidos durante a sessão
    val picoDb: Int = 0,
    val minDb: Int = 0,

    // Local de estudo (id da tabela "locais"); null = sem local
    val localId: Int? = null
)