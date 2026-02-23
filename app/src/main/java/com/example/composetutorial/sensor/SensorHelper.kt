package com.example.composetutorial.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Helper class to manage sensor readings using Kotlin Flows. Provides reactive sensor data for
 * Compose UI.
 */
class SensorHelper(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** Data class representing accelerometer readings */
    data class AccelerometerData(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f)

    /** Get accelerometer sensor readings as a Flow */
    fun getAccelerometerFlow(): Flow<AccelerometerData> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener =
                object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        trySend(
                                AccelerometerData(
                                        x = event.values[0],
                                        y = event.values[1],
                                        z = event.values[2]
                                )
                        )
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /** Get light sensor readings as a Flow */
    fun getLightSensorFlow(): Flow<Float> = callbackFlow {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        val listener =
                object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        trySend(event.values[0])
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

        lightSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /** Check if a specific sensor is available on the device */
    fun isSensorAvailable(sensorType: Int): Boolean {
        return sensorManager.getDefaultSensor(sensorType) != null
    }

    /** Get list of all available sensors on the device */
    fun getAllSensors(): List<Sensor> {
        return sensorManager.getSensorList(Sensor.TYPE_ALL)
    }
}
