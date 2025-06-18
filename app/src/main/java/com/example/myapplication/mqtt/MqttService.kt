package com.example.myapplication.mqtt

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*

class MqttService : Service() {
    
    private val binder = MqttBinder()
    private lateinit var mqttClient: MqttAndroidClient
    private val subscriptions = mutableMapOf<String, MqttSubscription>()
    private var isConnected = false
    
    companion object {
        private const val TAG = "MqttService"
        private const val SERVER_URI = "tcp://broker.hivemq.com:1883"
        private const val CLIENT_ID = "AndroidService_${System.currentTimeMillis()}"
    }
    
    inner class MqttBinder : Binder() {
        fun getService(): MqttService = this@MqttService
    }
    
    override fun onCreate() {
        super.onCreate()
        setupMqttClient()
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    private fun setupMqttClient() {
        mqttClient = MqttAndroidClient(this, SERVER_URI, CLIENT_ID)
        
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d(TAG, "Conexión completada, reconnect: $reconnect")
                isConnected = reconnect || true
            }
            
            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Conexión perdida: ${cause?.message}")
                isConnected = false
            }
            
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Mensaje recibido en $topic: ${message?.toString()}")
                topic?.let { topicName ->
                    subscriptions[topicName]?.let { subscription ->
                        subscription.lastMessage = message?.toString() ?: ""
                        subscription.timestamp = Date()
                    }
                }
            }
            
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "Entrega completada")
            }
        })
    }
    
    fun connect(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        try {
            val token = mqttClient.connect()
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Conexión MQTT exitosa")
                    isConnected = true
                    onSuccess()
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Error de conexión MQTT: ${exception?.message}")
                    isConnected = false
                    onFailure(exception?.message ?: "Error desconocido")
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al conectar: ${e.message}")
            isConnected = false
            onFailure(e.message ?: "Error desconocido")
        }
    }
    
    fun subscribe(topic: String, qos: Int, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (!isConnected) {
            onFailure("No conectado al broker")
            return
        }
        
        try {
            val token = mqttClient.subscribe(topic, qos)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Suscripción exitosa a $topic")
                    subscriptions[topic] = MqttSubscription(topic, qos)
                    onSuccess()
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Error en suscripción: ${exception?.message}")
                    onFailure(exception?.message ?: "Error desconocido")
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al suscribirse: ${e.message}")
            onFailure(e.message ?: "Error desconocido")
        }
    }
    
    fun unsubscribe(topic: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (!isConnected) {
            onFailure("No conectado al broker")
            return
        }
        
        try {
            val token = mqttClient.unsubscribe(topic)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Desuscripción exitosa de $topic")
                    subscriptions.remove(topic)
                    onSuccess()
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Error en desuscripción: ${exception?.message}")
                    onFailure(exception?.message ?: "Error desconocido")
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desuscribirse: ${e.message}")
            onFailure(e.message ?: "Error desconocido")
        }
    }
    
    fun publish(topic: String, message: String, qos: Int = 0, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (!isConnected) {
            onFailure("No conectado al broker")
            return
        }
        
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttMessage.qos = qos
            val token = mqttClient.publish(topic, mqttMessage)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Mensaje publicado exitosamente en $topic")
                    onSuccess()
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Error al publicar: ${exception?.message}")
                    onFailure(exception?.message ?: "Error desconocido")
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al publicar: ${e.message}")
            onFailure(e.message ?: "Error desconocido")
        }
    }
    
    fun getSubscriptions(): List<MqttSubscription> {
        return subscriptions.values.toList()
    }
    
    fun isConnected(): Boolean {
        return isConnected
    }
    
    fun disconnect() {
        try {
            if (isConnected && mqttClient.isConnected) {
                mqttClient.disconnect()
                isConnected = false
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
} 