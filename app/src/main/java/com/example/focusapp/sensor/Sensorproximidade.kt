package com.example.focusapp.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class SensorProximidade(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val sensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    var aoMudar: (Boolean) -> Unit = {}

    val existe: Boolean = sensor != null

    private var registrado = false

    fun iniciar() {

        if (sensor == null) {
            Log.e(TAG, "Sensor de proximidade não encontrado")
            return
        }

        if (registrado) return

        Log.d(TAG, "Nome: ${sensor.name}")
        Log.d(TAG, "Fabricante: ${sensor.vendor}")
        Log.d(TAG, "Alcance: ${sensor.maximumRange} cm")
        Log.d(TAG, "Resolução: ${sensor.resolution} cm")
        Log.d(TAG, "Delay mínimo: ${sensor.minDelay}")

        val sensores = sensorManager.getSensorList(
            Sensor.TYPE_PROXIMITY
        )

        Log.d(
            TAG,
            "Sensores encontrados: ${sensores.joinToString { it.name }}"
        )

        registrado = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        Log.d(TAG, "Listener registrado: $registrado")
    }

    fun parar() {

        if (registrado) {
            sensorManager.unregisterListener(this)
            registrado = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {

        val distancia = event.values.firstOrNull() ?: return
        val alcance = event.sensor.maximumRange

        val perto = distancia < alcance

        Log.d(
            TAG,
            "EVENTO RECEBIDO | " +
                    "Distância: $distancia cm | " +
                    "Alcance: $alcance cm | " +
                    "Perto: $perto"
        )

        aoMudar(perto)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Precisão alterada: $accuracy")
    }

    companion object {
        private const val TAG = "SensorProximidade"
    }
}