package com.mark.moodlogger.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Reads the phone's hardware step-counter sensor: a running total of steps since
 * the last reboot, and nothing else - no location, no route, no account. Needs
 * the ACTIVITY_RECOGNITION permission (API 29+).
 */
object StepCounter {

    fun isAvailable(context: Context): Boolean {
        val sm = context.getSystemService(SensorManager::class.java) ?: return false
        return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }

    /** One-shot read of the cumulative step count since boot; null if unavailable. */
    suspend fun readCumulative(context: Context): Long? {
        val sm = context.getSystemService(SensorManager::class.java) ?: return null
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        return suspendCancellableCoroutine { cont ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    sm.unregisterListener(this)
                    if (cont.isActive) cont.resume(event.values.firstOrNull()?.toLong())
                }
                override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
            }
            val registered = sm.registerListener(
                listener, sensor, SensorManager.SENSOR_DELAY_FASTEST,
            )
            if (!registered && cont.isActive) cont.resume(null)
            cont.invokeOnCancellation { sm.unregisterListener(listener) }
        }
    }
}
