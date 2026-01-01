package com.example.battleship.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class OrientationSensor(context: Context, private val onOrientationChanged: (Float) -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Necesitamos dos sensores según Diapositiva 29 (Fusión básica) y 17
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    fun start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // Guardamos los datos brutos (Diapositiva 43)
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values
        }

        // Si tenemos ambos datos, calculamos la orientación
        if (gravity != null && geomagnetic != null) {
            val r = FloatArray(9)
            val i = FloatArray(9)

            // Implementación de la Diapositiva 17: getRotationMatrix
            // Transforma del Device Frame al ENU Frame
            val success = SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)

            if (success) {
                val orientation = FloatArray(3)
                // Implementación de la Diapositiva 41: getOrientation
                // Convierte la matriz a Ángulos de Euler (Azimuth, Pitch, Roll)
                SensorManager.getOrientation(r, orientation)

                // El Azimuth es el primer valor (índice 0) según Diapositiva 21
                // Viene en radianes, lo pasamos a grados
                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

                // Normalizamos para que sea 0-360 grados
                if (azimuth < 0) azimuth += 360

                onOrientationChanged(azimuth)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}