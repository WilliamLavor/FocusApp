package com.example.focusapp.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// onde a pessoa estuda (biblioteca, quarto...)
@Entity(tableName = "locais")
data class Local(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nome: String,

    val descricao: String = ""
)
