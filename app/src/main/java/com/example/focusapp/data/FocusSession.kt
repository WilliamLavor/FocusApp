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

    val notas: String = ""
)