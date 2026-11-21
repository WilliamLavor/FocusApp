
package com.example.focusapp.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class SensorVirado(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var aoMudar: (Boolean) -> Unit = {}

    val existe: Boolean = sensor != null

    private var registrado = false
    private var viradoParaBaixo = false

    // Momento em que o celular começou a permanecer na posição desejada.
    private var inicioPosicaoNanos: Long? = null

    fun iniciar() {
        if (sensor == null || registrado) return

        viradoParaBaixo = false
        inicioPosicaoNanos = null

        registrado = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun parar() {
        if (registrado) {
            sensorManager.unregisterListener(this)
            registrado = false
        }

        viradoParaBaixo = false
        inicioPosicaoNanos = null
    }

    override fun onSensorChanged(event: SensorEvent) {

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calcula a intensidade total da aceleração.
        val intensidade = sqrt(x * x + y * y + z * z)

        // Verifica se o aparelho está praticamente em repouso.
        val aceleracaoEstavel =
            abs(intensidade - SensorManager.GRAVITY_EARTH) < 1.0f

        // Para entrar: exige uma posição bem próxima da tela para baixo.
        if (!viradoParaBaixo) {

            val orientacaoValida = z <= -9.0f && aceleracaoEstavel

            if (orientacaoValida) {

                if (inicioPosicaoNanos == null) {
                    inicioPosicaoNanos = event.timestamp
                }

                val tempoDecorrido =
                    event.timestamp - inicioPosicaoNanos!!

                // Só ativa depois de 2 segundos na posição desejada.
                if (tempoDecorrido >= TEMPO_ESTAVEL_NANOS) {

                    viradoParaBaixo = true
                    inicioPosicaoNanos = null

                    aoMudar(true)
                }

            } else {
                // Se sair da posição antes dos 2 segundos, reinicia a contagem.
                inicioPosicaoNanos = null
            }

        } else {

            // Limite diferente para evitar oscilações na detecção.
            if (z > -8.5f) {

                viradoParaBaixo = false
                inicioPosicaoNanos = null

                aoMudar(false)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    companion object {
        private const val TEMPO_ESTAVEL_NANOS = 2_000_000_000L
    }
}
