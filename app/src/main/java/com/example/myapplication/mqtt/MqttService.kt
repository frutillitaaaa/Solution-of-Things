package com.example.myapplication.mqtt

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MqttService : Service() {
    private val binder = LocalBinder()
    private lateinit var mqttClient: MqttAndroidClient
    private var isConnected = false
    private var serverUri: String = ""
    private var clientId: String = ""
    private var options: MqttConnectOptions? = null
    private var reconnecting = false

    companion object {
        const val ACTION_MQTT_MESSAGE = "com.example.myapplication.mqtt.ACTION_MQTT_MESSAGE"
        const val EXTRA_TOPIC = "topic"
        const val EXTRA_MESSAGE = "message"
    }

    inner class LocalBinder : Binder() {
        fun getService(): MqttService = this@MqttService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        // Escuchar cambios de red
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!isConnected && ::mqttClient.isInitialized) {
                    reconnect()
                }
            }
        })
    }

    fun connect(serverUri: String, clientId: String, callback: (Boolean, String?) -> Unit) {
        this.serverUri = serverUri
        this.clientId = clientId
        val webSocketUri = convertToWebSocketUri(serverUri)
        mqttClient = MqttAndroidClient(applicationContext, webSocketUri, clientId)
        setupMqttCallback()
        options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 60
        }
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // Se maneja en connectComplete
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    callback(false, exception?.message)
                }
            })
        } catch (e: MqttException) {
            callback(false, e.message)
        }
    }

    fun disconnect() {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            mqttClient.disconnect()
            isConnected = false
        }
    }

    fun publish(topic: String, messageText: String, callback: (Boolean) -> Unit) {
        if (!isConnected) {
            callback(false)
            return
        }
        try {
            val message = MqttMessage()
            message.payload = messageText.toByteArray()
            message.qos = 1
            message.isRetained = false
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) { callback(true) }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) { callback(false) }
            })
        } catch (e: MqttException) {
            callback(false)
        }
    }

    fun subscribe(topic: String, callback: (Boolean) -> Unit) {
        if (!isConnected) {
            callback(false)
            return
        }
        try {
            mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) { callback(true) }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) { callback(false) }
            })
        } catch (e: MqttException) {
            callback(false)
        }
    }

    private fun reconnect() {
        if (reconnecting || isConnected || !::mqttClient.isInitialized) return
        reconnecting = true
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    isConnected = true
                    reconnecting = false
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    reconnecting = false
                    // Intentar de nuevo después de un tiempo
                    Timer().schedule(object : TimerTask() {
                        override fun run() { reconnect() }
                    }, 5000)
                }
            })
        } catch (e: MqttException) {
            reconnecting = false
        }
    }

    private fun convertToWebSocketUri(brokerUri: String): String {
        val hostWithPort = brokerUri.removePrefix("tcp://").removePrefix("ssl://")
        val host = hostWithPort.split(":")[0]
        return when {
            host == "test.mosquitto.org" -> "tcp://$host:1883"
            brokerUri.startsWith("ssl://") -> "wss://$hostWithPort"
            else -> "ws://$hostWithPort"
        }
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    fun setMessageCallback(callback: ((String, String) -> Unit)?) {
        // Dummy para compatibilidad, no se usa con broadcast
    }

    fun sendMessageToUI(topic: String?, message: String?) {
        val intent = Intent(ACTION_MQTT_MESSAGE)
        intent.putExtra(EXTRA_TOPIC, topic)
        intent.putExtra(EXTRA_MESSAGE, message)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendConnectionLost() {
        val intent = Intent("MQTT_CONNECTION_LOST")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendConnectionRestored() {
        val intent = Intent("MQTT_CONNECTION_RESTORED")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun setupMqttCallback() {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                isConnected = true
                sendConnectionRestored()
            }
            override fun connectionLost(cause: Throwable?) {
                isConnected = false
                sendConnectionLost()
                reconnect()
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                sendMessageToUI(topic, message?.toString())
            }
            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
    }
} 