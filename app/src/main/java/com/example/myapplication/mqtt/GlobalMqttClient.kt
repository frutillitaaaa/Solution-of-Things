package com.example.myapplication.mqtt

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions

object GlobalMqttClient {
    private var mqttClient: MqttAndroidClient? = null
    private var isConnected = false

    fun getClient(context: Context, serverUri: String, clientId: String): MqttAndroidClient {
        if (mqttClient == null) {
            mqttClient = MqttAndroidClient(context.applicationContext, serverUri, clientId)
        }
        return mqttClient!!
    }

    fun connect(context: Context, serverUri: String, clientId: String, callback: (() -> Unit)? = null) {
        if (mqttClient == null) {
            mqttClient = MqttAndroidClient(context.applicationContext, serverUri, clientId)
        }
        if (!isConnected) {
            val options = MqttConnectOptions()
            options.isCleanSession = true
            options.connectionTimeout = 10
            options.keepAliveInterval = 60
            mqttClient!!.connect(options, null, object : org.eclipse.paho.client.mqttv3.IMqttActionListener {
                override fun onSuccess(asyncActionToken: org.eclipse.paho.client.mqttv3.IMqttToken?) {
                    isConnected = true
                    callback?.invoke()
                }
                override fun onFailure(asyncActionToken: org.eclipse.paho.client.mqttv3.IMqttToken?, exception: Throwable?) {
                    isConnected = false
                }
            })
        } else {
            callback?.invoke()
        }
    }

    fun isConnected(): Boolean = isConnected

    fun publish(topic: String, message: String) {
        if (mqttClient != null && isConnected) {
            val mqttMessage = org.eclipse.paho.client.mqttv3.MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttMessage.qos = 1
            mqttMessage.isRetained = false
            mqttClient!!.publish(topic, mqttMessage)
        }
    }
} 