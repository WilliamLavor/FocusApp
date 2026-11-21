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

    // estrelas de 1 a 5 (0 = sem avaliação)
    val avaliacao: Int = 0,

    // fatores separados por vírgula
    val fatores: String = "",

    // maior e menor dB da sessão
    val picoDb: Int = 0,
    val minDb: Int = 0,

    // id do local (null = sem local)
    val localId: Int? = null
)