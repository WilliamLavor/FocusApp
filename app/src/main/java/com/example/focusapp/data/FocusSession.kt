package com.example.focusapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "focus_session")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materia: String,
    val duration: String,
    val date: String,
    val notas: String = "" // Campo opcional para anotações do estudo
)