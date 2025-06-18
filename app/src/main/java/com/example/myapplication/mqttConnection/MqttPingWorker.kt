// src/main/java/com/example/myapplication/mqttConnection/MqttPingWorker.kt
package com.example.myapplication.mqttConnection

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttException

class MqttPingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            // Usa reflexión para invocar el método interno que envía el ping
            val commsField = MqttHelper.client.javaClass
                .getDeclaredField("comms")
                .apply { isAccessible = true }
            val comms = commsField.get(MqttHelper.client)

            val method = comms.javaClass
                .getDeclaredMethod("checkForActivity", IMqttActionListener::class.java)
                .apply { isAccessible = true }

            method.invoke(comms, object : IMqttActionListener {
                override fun onSuccess(asyncToken: IMqttToken?) {
                    Log.d("MqttPingWorker", "PINGREQ sent successfully")
                }
                override fun onFailure(asyncToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MqttPingWorker", "PINGREQ failed", exception)
                }
            })

            // Reprograma el siguiente ping
            MqttHelper.run {
                javaClass
                    .getDeclaredMethod("scheduleManualPing")
                    .apply { isAccessible = true }
                    .invoke(this)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("MqttPingWorker", "Error during ping", e)
            Result.retry()
        }
    }
}
