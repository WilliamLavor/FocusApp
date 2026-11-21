package com.example.focusapp.sensor

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.log10
import kotlin.math.sqrt

// Mede o ruído do ambiente usando o microfone.
// Uso: iniciar() -> lerDb() quantas vezes quiser -> parar()
class MedidorRuido {

    private val taxaAmostragem = 44100

    // Tamanho mínimo de buffer que o Android aceita para esta configuração
    private val tamanhoBuffer = AudioRecord.getMinBufferSize(
        taxaAmostragem,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private var gravador: AudioRecord? = null

    // A permissão RECORD_AUDIO é pedida na tela, antes de chamar este método
    @SuppressLint("MissingPermission")
    fun iniciar() {
        val novoGravador = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            taxaAmostragem,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            tamanhoBuffer
        )

        if (novoGravador.state == AudioRecord.STATE_INITIALIZED) {
            novoGravador.startRecording()
            gravador = novoGravador
        } else {
            novoGravador.release()
        }
    }

    // Lê um pedaço do áudio e devolve o nível de ruído em dB (valor relativo)
    fun lerDb(): Int {
        val amostras = ShortArray(tamanhoBuffer)
        val quantidade = gravador?.read(amostras, 0, amostras.size) ?: 0
        if (quantidade <= 0) return 0

        // Raiz da média dos quadrados (RMS): mede a "força" do som
        var soma = 0.0
        for (i in 0 until quantidade) {
            soma += amostras[i] * amostras[i].toDouble()
        }
        val rms = sqrt(soma / quantidade)
        if (rms < 1) return 0

        // Converte para decibéis
        return (20 * log10(rms)).toInt()
    }

    fun parar() {
        gravador?.stop()
        gravador?.release()
        gravador = null
    }
}