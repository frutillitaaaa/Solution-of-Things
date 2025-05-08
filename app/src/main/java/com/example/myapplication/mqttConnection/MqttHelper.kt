package com.example.myapplication.mqttConnection

import android.content.Context
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttHelper(context: Context) {
    private var mqttClient: MqttClient? = null
    private val serverUri = "tcp://tu.broker.mqtt:1883" // Reemplaza con tu broker
    private val clientId = "AndroidClient_" + System.currentTimeMillis()

    fun connect() {
        try {
            mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
            mqttClient?.connect(getMqttConnectOptions())

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    // Manejar reconexión
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Manejar mensajes recibidos
                    println("Mensaje recibido: ${message?.toString()} en $topic")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Confirmación de entrega
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