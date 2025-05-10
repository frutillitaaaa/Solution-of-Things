package com.example.myapplication.mqttConnection

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient // <- Cambia a la ubicación correcta
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.MqttException

object MqttHelper {
    private const val TAG = "AndroidMqttClient"
    private const val SERVER_URI = "tcp://10.0.2.2:1883" // ⚠️ Usa "tcp://" en lugar de "mqtt://"
    private const val CLIENT_ID = "android_client" // ¡Mejor práctica!

    internal lateinit var client: MqttAndroidClient

    fun init(context: Context) {
        client = MqttAndroidClient(context, SERVER_URI, CLIENT_ID).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.d(TAG, "Connection lost", cause)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    Log.d(TAG, "Receive message from $topic: ${message.payload.decodeToString()}")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // No-op
                }
            })
        }
    }

    fun connect() {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }

        try {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connected!")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failed", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error connecting", e)
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            client.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Subscribe failed", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error subscribing", e)
        }
    }

    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        val msg = MqttMessage().apply {
            this.payload = payload.toByteArray()
            this.qos = qos
            isRetained = retained
        }

        try {
            client.publish(topic, msg, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Message published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Publish failed", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error publishing", e)
        }
    }

    fun disconnect() {
        try {
            client.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Disconnect failed", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }
}