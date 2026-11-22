package com.example.focusapp.ui.ajustes

import android.content.Context
import com.example.focusapp.ui.focus.DESCANSO_MAX_MINUTOS
import com.example.focusapp.ui.focus.DESCANSO_MIN_MINUTOS
import com.example.focusapp.ui.focus.FOCO_MAX_MINUTOS
import com.example.focusapp.ui.focus.FOCO_MIN_MINUTOS

// minutos padrão de foco e descanso, guardados no mesmo "config" do tema
object Preferencias {

    private const val ARQUIVO = "config"
    private const val FOCO = "foco_padrao"
    private const val DESCANSO = "descanso_padrao"

    private fun prefs(context: Context) =
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun foco(context: Context): Int =
        prefs(context).getInt(FOCO, 25).coerceIn(FOCO_MIN_MINUTOS, FOCO_MAX_MINUTOS)

    fun descanso(context: Context): Int =
        prefs(context).getInt(DESCANSO, 5).coerceIn(DESCANSO_MIN_MINUTOS, DESCANSO_MAX_MINUTOS)

    fun salvar(context: Context, foco: Int, descanso: Int) {
        prefs(context).edit()
            .putInt(FOCO, foco)
            .putInt(DESCANSO, descanso)
            .apply()
    }
}
