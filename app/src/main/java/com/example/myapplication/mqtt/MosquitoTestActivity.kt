package com.example.myapplication.mqtt

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMosquitoTestBinding
import okhttp3.*
import okio.ByteString
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MosquitoTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosquitoTestBinding
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            runOnUiThread {
                isConnected = true
                updateStatus("Conectado")
                Toast.makeText(this@MosquitoTestActivity, "Conectado exitosamente", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runOnUiThread {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val receivedMessage = "[$timestamp] Mensaje recibido: $text"
                addMessage(receivedMessage)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            runOnUiThread {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val receivedMessage = "[$timestamp] Mensaje recibido (bytes): ${bytes.utf8()}"
                addMessage(receivedMessage)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            runOnUiThread {
                isConnected = false
                updateStatus("Desconectado: $reason")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            runOnUiThread {
                isConnected = false
                updateStatus("Error: ${t.message}")
                Toast.makeText(this@MosquitoTestActivity, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMosquitoTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener {
            connectToMqtt()
        }

        binding.btnDisconnect.setOnClickListener {
            disconnectFromMqtt()
        }

        binding.btnPublish.setOnClickListener {
            publishMessage()
        }

        binding.btnSubscribe.setOnClickListener {
            subscribeToTopic()
        }
    }

    private fun connectToMqtt() {
        val brokerUrl = binding.etBroker.text.toString()
        val clientId = binding.etClientId.text.toString()

        if (brokerUrl.isEmpty() || clientId.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Convertir URL MQTT a WebSocket
            val wsUrl = convertMqttToWebSocket(brokerUrl)
            
            val request = Request.Builder()
                .url(wsUrl)
                .build()

            webSocket = client.newWebSocket(request, webSocketListener)
            updateStatus("Conectando...")

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun disconnectFromMqtt() {
        webSocket?.let { ws ->
            ws.close(1000, "Desconexión normal")
            webSocket = null
            isConnected = false
            updateStatus("Desconectado")
            Toast.makeText(this, "Desconectado exitosamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun publishMessage() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker MQTT", Toast.LENGTH_SHORT).show()
            return
        }

        val topic = binding.etTopic.text.toString()
        val message = binding.etMessage.text.toString()

        if (topic.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Por favor completa el tópico y el mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Crear mensaje MQTT simple
            val mqttMessage = createMqttPublishMessage(topic, message)
            webSocket?.send(mqttMessage)
            
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val sentMessage = "[$timestamp] Enviado a $topic: $message"
            addMessage(sentMessage)
            Toast.makeText(this, "Mensaje enviado", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al publicar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun subscribeToTopic() {
        if (!isConnected) {
            Toast.makeText(this, "No estás conectado al broker MQTT", Toast.LENGTH_SHORT).show()
            return
        }

        val topic = binding.etTopic.text.toString()

        if (topic.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa un tópico", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Crear mensaje de suscripción MQTT simple
            val subscribeMessage = createMqttSubscribeMessage(topic)
            webSocket?.send(subscribeMessage)
            
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val subscribeMsg = "[$timestamp] Suscrito a: $topic"
            addMessage(subscribeMsg)
            Toast.makeText(this, "Suscrito a $topic", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al suscribirse: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertMqttToWebSocket(mqttUrl: String): String {
        return when {
            mqttUrl.startsWith("tcp://") -> {
                val host = mqttUrl.substring(6).split(":")[0]
                val port = mqttUrl.substring(6).split(":")[1].toIntOrNull() ?: 1883
                "ws://$host:${port + 1000}" // Puerto WebSocket típico
            }
            mqttUrl.startsWith("ssl://") -> {
                val host = mqttUrl.substring(6).split(":")[0]
                val port = mqttUrl.substring(6).split(":")[1].toIntOrNull() ?: 8883
                "wss://$host:${port + 1000}" // Puerto WebSocket seguro típico
            }
            else -> {
                val host = mqttUrl.split(":")[0]
                val port = mqttUrl.split(":")[1].toIntOrNull() ?: 1883
                "ws://$host:${port + 1000}"
            }
        }
    }

    private fun createMqttPublishMessage(topic: String, message: String): String {
        // Implementación simple de mensaje MQTT PUBLISH
        return "PUBLISH\nTopic: $topic\nMessage: $message\n"
    }

    private fun createMqttSubscribeMessage(topic: String): String {
        // Implementación simple de mensaje MQTT SUBSCRIBE
        return "SUBSCRIBE\nTopic: $topic\n"
    }

    private fun updateStatus(status: String) {
        binding.tvStatus.text = status
        binding.tvStatus.setTextColor(if (isConnected) 0xFF4CAF50.toInt() else 0xFFFF0000.toInt())
    }

    private fun addMessage(message: String) {
        val currentText = binding.tvMessages.text.toString()
        val newText = if (currentText.isEmpty()) message else "$currentText\n$message"
        binding.tvMessages.text = newText
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromMqtt()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
} 