package com.example.myapplication.mqtt

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

object MqttManager {
    lateinit var mqttClient: MqttAndroidClient
    var isConnected = false

    fun connect(context: Context, serverUri: String, clientId: String, callback: MqttCallbackExtended) {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            Log.d("MQTT", "Ya está conectado")
            return
        }

        mqttClient = MqttAndroidClient(context.applicationContext, serverUri, clientId)
        mqttClient.setCallback(callback)

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 60
        }

        Log.d("MQTT", "Intentando conectar a $serverUri con clientId=$clientId")

        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                isConnected = true
                Log.d("MQTT", "Conexión exitosa a $serverUri")
                callback.connectComplete(false, serverUri) // 🔧 llama manualmente
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                isConnected = false
                Log.e("MQTT", "Error al conectar: ${exception?.message}")
                exception?.printStackTrace()
            }
        })
    }

    fun publish(topic: String, messageText: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        if (!isConnected || !::mqttClient.isInitialized) return onFailure()

        val message = MqttMessage().apply {
            payload = messageText.toByteArray()
            qos = 1
            isRetained = false
        }

        mqttClient.publish(topic, message, null, object : IMqttActionListener {
            override fun onSuccess(token: IMqttToken?) = onSuccess()
            override fun onFailure(token: IMqttToken?, exception: Throwable?) = onFailure()
        })
    }

    fun subscribe(topic: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        if (!isConnected || !::mqttClient.isInitialized) return onFailure()

        mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
            override fun onSuccess(token: IMqttToken?) = onSuccess()
            override fun onFailure(token: IMqttToken?, exception: Throwable?) = onFailure()
        })
    }

    fun disconnect() {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            mqttClient.disconnect()
            isConnected = false
        }
    }
}