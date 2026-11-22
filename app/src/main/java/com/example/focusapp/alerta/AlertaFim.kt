package com.example.focusapp.alerta

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// vibra e toca quando o foco/descanso acaba
class AlertaFim(context: Context) {

    private val contexto = context.applicationContext

    //guarda o toque pra ele não ser coletado no meio
    private var toque: Ringtone? = null

    fun tocar() {
        vibrar()
        tocarSom()
    }

    private fun vibrar() {
        try {
            val vibrador: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val gerenciador =
                    contexto.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                gerenciador.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                contexto.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (!vibrador.hasVibrator()) return

            // 3 vibradas curtas
            val padrao = longArrayOf(0, 400, 200, 400, 200, 400)
            vibrador.vibrate(VibrationEffect.createWaveform(padrao, -1))
        } catch (e: Exception) {
            //sem vibração, segue sem
        }
    }

    private fun tocarSom() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            toque?.stop()
            toque = RingtoneManager.getRingtone(contexto, uri)
            toque?.play()
        } catch (e: Exception) {
            // sem som, segue sem
        }
    }
}
