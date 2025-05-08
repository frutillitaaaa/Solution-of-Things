package com.example.myapplication.mqttConnection

import android.content.Context
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttHprivate lateinit var mqttClient: MqttAndroidClient
// TAG
    companion object {
    const val TAG = "AndroidMqttClient"
}elper(context: Context) {


    fun connect(context: Context) {
        val serverURI = "tcp://LocalHost.mqtt:1883"
        mqttClient = MqttAndroidClient(context, serverURI, "kotlin_client")
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        val options = MqttConnectOptions()
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Connection failure")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }

    private fun getMqttConnectOptions(): MqttConnectOptions {
        val options = MqttConnectOptions()
        options.isCleanSession = true
        options.connectionTimeout = 10
        options.keepAliveInterval = 20
        // Si tu broker requiere autenticación:
        // options.userName = "usuario"
        // options.password = "contraseña".toCharArray()
        return options
    }

    fun subscribe(topic: String, qos: Int = 1) {
        mqttClient?.subscribe(topic, qos)
    }

    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        val mqttMessage = MqttMessage(message.toByteArray())
        mqttMessage.qos = qos
        mqttMessage.isRetained = retained
        mqttClient?.publish(topic, mqttMessage)
    }

    fun disconnect() {
        mqttClient?.disconnect()
    }
}