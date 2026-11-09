package com.example.focusapp.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// Usa o acelerômetro para saber se o celular está com a tela virada para baixo.
// Funciona mesmo com capinha alta, porque mede a gravidade e não a distância.
class SensorVirado(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Função que a tela define para ser avisada: true = tela virada para baixo
    var aoMudar: (Boolean) -> Unit = {}

    // false se o aparelho não tiver acelerômetro
    val existe: Boolean = sensor != null

    // Guarda o último estado para só avisar quando ele mudar
    private var viradoParaBaixo = false

    fun iniciar() {
        viradoParaBaixo = false
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun parar() {
        sensorManager.unregisterListener(this)
    }

    // O Android chama esta função toda vez que o acelerômetro mede algo novo
    override fun onSensorChanged(event: SensorEvent) {
        // Eixo Z: perto de +9,8 com a tela para cima e de -9,8 com a tela para baixo
        val z = event.values[2]
        val agoraVirado = z < -7

        if (agoraVirado != viradoParaBaixo) {
            viradoParaBaixo = agoraVirado
            aoMudar(agoraVirado)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}