package com.example.myapplication.mqtt

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMosquitoTestBinding
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat
import java.util.*

class MosquitoTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosquitoTestBinding
    private lateinit var mqttClient: MqttAndroidClient
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMosquitoTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener { connectToMqtt() }
        binding.btnDisconnect.setOnClickListener { disconnectFromMqtt() }
        binding.btnPublish.setOnClickListener { publishMessage() }
        binding.btnSubscribe.setOnClickListener { subscribeToTopic() }
        binding.btnGirarMotor.setOnClickListener { girarMotor() }
    }

    private fun connectToMqtt() {
        val serverUri = binding.etBroker.text.toString()
        val clientId = binding.etClientId.text.toString()

        if (serverUri.isEmpty() || clientId.isEmpty()) {
            Toast.makeText(this, "Por favor completa Broker y Cliente ID", Toast.LENGTH_SHORT).show()
            return
        }

        // La URI debe ser un WebSocket (ws:// o wss://) para Paho sobre WS
        val webSocketUri = convertToWebSocketUri(serverUri)

        mqttClient = MqttAndroidClient(applicationContext, webSocketUri, clientId)
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                isConnected = true
                runOnUiThread {
                    updateStatus("Conectado")
                    Toast.makeText(this@MosquitoTestActivity, "Conectado exitosamente a $serverURI", Toast.LENGTH_SHORT).show()
                }
            }

            override fun connectionLost(cause: Throwable?) {
                isConnected = false
                runOnUiThread {
                    updateStatus("Conexión perdida: ${cause?.message}")
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val receivedMessage = "[$timestamp] Mensaje de '$topic': ${message.toString()}"
                addMessage(receivedMessage)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // No es necesario para este ejemplo
            }
        })

        val options = MqttConnectOptions()
        options.isCleanSession = true // Iniciar sesión limpia
        options.connectionTimeout = 10
        options.keepAliveInterval = 60

        try {
            updateStatus("Conectando a $webSocketUri...")
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // La conexión se confirma en connectComplete
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    runOnUiThread {
                        updateStatus("Error al conectar: ${exception?.message}")
                        Toast.makeText(this@MosquitoTestActivity, "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            updateStatus("Error: ${e.message}")
        }
    }

    private fun disconnectFromMqtt() {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) {
            Toast.makeText(this, "Ya estás desconectado", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    isConnected = false
                    runOnUiThread {
                        updateStatus("Desconectado")
                        Toast.makeText(this@MosquitoTestActivity, "Desconectado exitosamente", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    runOnUiThread {
                        updateStatus("Error al desconectar: ${exception?.message}")
                    }
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun publishMessage() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }

        val topic = binding.etTopic.text.toString()
        val messageText = binding.etMessage.text.toString()

        if (topic.isEmpty() || messageText.isEmpty()) {
            Toast.makeText(this, "Completa el tópico y el mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val message = MqttMessage()
            message.payload = messageText.toByteArray()
            message.qos = 1 // Calidad de servicio 1
            message.isRetained = false

            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val sentMessage = "[$timestamp] Enviado a '$topic': $messageText"
                    addMessage(sentMessage)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("Error al enviar a '$topic'")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun subscribeToTopic() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }

        val topic = binding.etTopic.text.toString()

        if (topic.isEmpty()) {
            Toast.makeText(this, "Ingresa un tópico para suscribirte", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val subscribeMsg = "[$timestamp] Suscrito a: $topic"
                    addMessage(subscribeMsg)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("Error al suscribirse a '$topic'")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun girarMotor() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker", Toast.LENGTH_SHORT).show()
            return
        }
        val topic = "esp32/motor"
        val messageText = "GIRO"
        try {
            val message = MqttMessage()
            message.payload = messageText.toByteArray()
            message.qos = 1
            message.isRetained = false
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val sentMessage = "[$timestamp] Enviado a '$topic': $messageText"
                    addMessage(sentMessage)
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    addMessage("Error al enviar a '$topic'")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun convertToWebSocketUri(brokerUri: String): String {
        // Paho espera un formato como "ws://host:port"
        val hostWithPort = brokerUri.removePrefix("tcp://").removePrefix("ssl://")
        val host = hostWithPort.split(":")[0]
        
        return when {
            host == "test.mosquitto.org" -> "ws://$host:8080"
            brokerUri.startsWith("ssl://") -> "wss://$hostWithPort"
            else -> "ws://$hostWithPort"
        }
    }

    private fun updateStatus(status: String) {
        binding.tvStatus.text = status
        binding.tvStatus.setTextColor(if (isConnected) 0xFF4CAF50.toInt() else 0xFFFF0000.toInt())
    }

    private fun addMessage(message: String) {
        runOnUiThread {
            val currentText = binding.tvMessages.text.toString()
            val newText = if (currentText.isEmpty()) message else "$currentText\n$message"
            binding.tvMessages.text = newText
        }
    }

    override fun onDestroy() {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            mqttClient.disconnect()
        }
        super.onDestroy()
    }
} 