package com.example.focusapp.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// Lê o sensor de proximidade (fica na frente do celular, perto do alto-falante).
// Quando o celular está virado para baixo na mesa, o sensor fica "coberto".
class SensorProximidade(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    // Função que a tela define para ser avisada: true = tem algo perto do sensor
    var aoMudar: (Boolean) -> Unit = {}

    // false se o aparelho não tiver sensor de proximidade
    val existe: Boolean = sensor != null

    fun iniciar() {
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun parar() {
        sensorManager.unregisterListener(this)
    }

    // O Android chama esta função toda vez que o valor do sensor muda
    override fun onSensorChanged(event: SensorEvent) {
        val distancia = event.values[0]
        // Perto = distância menor que o alcance máximo do sensor
        val perto = distancia < (sensor?.maximumRange ?: 0f)
        aoMudar(perto)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}