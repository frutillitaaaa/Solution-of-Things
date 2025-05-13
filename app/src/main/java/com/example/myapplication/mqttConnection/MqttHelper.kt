package com.example.myapplication.mqttConnection

import android.util.Log
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets

object MqttHelper {
    private const val TAG = "MqttHelper"
    private const val SERVER_URI = "tcp://10.0.2.2:1883"
    private const val CLIENT_ID = "android_client"

     lateinit var client: MqttAsyncClient

    @Synchronized
    fun init() {
        if (::client.isInitialized) return

        client = MqttAsyncClient(SERVER_URI, CLIENT_ID, MemoryPersistence()).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.d(TAG, "Connection lost", cause)
                    // reintentar o notificar
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    val payload = message.payload.toString(StandardCharsets.UTF_8)
                    Log.d(TAG, "Msg arrived → topic='$topic' payload='$payload'")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken) {
                    Log.d(TAG, "Delivery complete: ${token.message}")
                }
            })
        }
    }

    fun connect() {
        init()
        val opts = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            // El keepAlive aquí funciona con un scheduler interno, NO usa AlarmManager
            keepAliveInterval = 20
        }

        try {
            client.connect(opts, null, object : IMqttActionListener {
                override fun onSuccess(asyncToken: IMqttToken?) {
                    Log.d(TAG, "Connected to $SERVER_URI")
                }

                override fun onFailure(asyncToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failed", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error on connect()", e)
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            client.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Subscribe failed", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error on subscribe()", e)
        }
    }

    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val msg = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8)).apply {
                this.qos = qos
                isRetained = retained
            }
            client.publish(topic, msg, null, object : IMqttActionListener {
                override fun onSuccess(asyncToken: IMqttToken?) {
                    Log.d(TAG, "Published to $topic")
                }

                override fun onFailure(asyncToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Publish failed", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error on publish()", e)
        }
    }

    fun disconnect() {
        try {
            client.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected cleanly")
                }

                override fun onFailure(asyncToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Disconnect failed", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error on disconnect()", e)
        }
    }
}
