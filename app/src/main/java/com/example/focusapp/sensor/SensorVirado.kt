
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

    //desde quando ta virado
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

        //força total da aceleração
        val intensidade = sqrt(x * x + y * y + z * z)

        //parado mesmo?
        val aceleracaoEstavel =
            abs(intensidade - SensorManager.GRAVITY_EARTH) < 1.0f

        //pra entrar tem que estar bem de cabeça pra baixo
        if (!viradoParaBaixo) {

            val orientacaoValida = z <= -9.0f && aceleracaoEstavel

            if (orientacaoValida) {

                if (inicioPosicaoNanos == null) {
                    inicioPosicaoNanos = event.timestamp
                }

                val tempoDecorrido =
                    event.timestamp - inicioPosicaoNanos!!

                //só vale depois de 2s assim
                if (tempoDecorrido >= TEMPO_ESTAVEL_NANOS) {

                    viradoParaBaixo = true
                    inicioPosicaoNanos = null

                    aoMudar(true)
                }

            } else {
                inicioPosicaoNanos = null
            }

        } else {

            //limite mais folgado pra sair, senão fica oscilando
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
