package com.example.battleship.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Helper class to compute the device's physical orientation (Azimuth/Compass heading).
 *
 * It implements a basic sensor fusion algorithm combining data from:
 * 1. Accelerometer (Gravity vector).
 * 2. Magnetometer (Geomagnetic field vector).
 *
 * @property context The application context to access system services.
 * @property onOrientationChanged Callback invoked with the new azimuth in degrees (0-360).
 */
class OrientationSensor(
    context: Context,
    private val onOrientationChanged: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // We need both sensors to determine orientation relative to the Earth's frame
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Temporary storage for raw sensor data
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    /**
     * Starts listening to sensor updates.
     * Uses [SensorManager.SENSOR_DELAY_UI] which provides a rate suitable for UI updates
     * without consuming excessive battery.
     */
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Stops listening to sensor updates to save battery.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Called when sensor values change.
     * This method performs the sensor fusion math to calculate the Azimuth.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // 1. Cache the raw values from each sensor
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values
        }

        // 2. Compute orientation only when both sensor datasets are available
        if (gravity != null && geomagnetic != null) {
            val r = FloatArray(9)
            val i = FloatArray(9)

            // Calculate the Rotation Matrix (Transforms from Device Frame to World/ENU Frame)
            val success = SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)

            if (success) {
                val orientation = FloatArray(3)
                // Convert the rotation matrix into Euler Angles (Azimuth, Pitch, Roll)
                SensorManager.getOrientation(r, orientation)

                // The Azimuth (compass bearing) is the first value (index 0)
                // The API returns radians, so we convert to degrees
                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

                // Normalize the result to ensure a range of [0, 360] degrees
                // (Math.toDegrees can return negative values for the western hemisphere)
                if (azimuth < 0) azimuth += 360

                // Send the result to the UI
                onOrientationChanged(azimuth)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op: We do not need to handle accuracy changes for this implementation
    }
}