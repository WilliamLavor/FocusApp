package com.example.focusapp.ui.focus

// devolve o número se o texto for um inteiro dentro do limite, senão null
fun lerMinutos(texto: String, minimo: Int, maximo: Int): Int? {
    val numero = texto.trim().toIntOrNull() ?: return null
    return if (numero in minimo..maximo) numero else null
}
