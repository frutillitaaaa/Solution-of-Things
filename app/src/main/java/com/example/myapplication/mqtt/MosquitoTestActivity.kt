package com.example.myapplication.mqtt

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMosquitoTestBinding
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MosquitoTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosquitoTestBinding
    private var mqttClient: Mqtt5AsyncClient? = null
    private var isConnected = false

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
            // Crear cliente MQTT
            mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .identifier(clientId)
                .serverHost(extractHost(brokerUrl))
                .serverPort(extractPort(brokerUrl))
                .buildAsync()

            // Configurar callbacks
            mqttClient?.toAsync()?.apply {
                // Callback de conexión
                setCallback(object : com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient.Mqtt5AsyncClientCallback {
                    override fun onConnected(client: com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient, connAck: Mqtt5ConnAck) {
                        runOnUiThread {
                            isConnected = true
                            updateStatus("Conectado a $brokerUrl")
                            Toast.makeText(this@MosquitoTestActivity, "Conectado exitosamente", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onDisconnected(client: com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient, throwable: Throwable?) {
                        runOnUiThread {
                            isConnected = false
                            updateStatus("Desconectado: ${throwable?.message ?: "Desconexión normal"}")
                            if (throwable != null) {
                                Toast.makeText(this@MosquitoTestActivity, "Conexión perdida: ${throwable.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onPublishReceived(client: com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient, publish: Mqtt5Publish) {
                        runOnUiThread {
                            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            val topic = publish.topic.toString()
                            val message = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                            val receivedMessage = "[$timestamp] $topic: $message"
                            addMessage(receivedMessage)
                        }
                    }
                })

                // Conectar al broker
                connectWith()
                    .cleanStart(true)
                    .keepAlive(60)
                    .send()
                    .whenComplete { connAck, throwable ->
                        if (throwable != null) {
                            runOnUiThread {
                                updateStatus("Error de conexión: ${throwable.message}")
                                Toast.makeText(this@MosquitoTestActivity, "Error al conectar: ${throwable.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun disconnectFromMqtt() {
        mqttClient?.let { client ->
            if (client.state == MqttClientState.CONNECTED) {
                client.disconnectWith()
                    .send()
                    .whenComplete { _, throwable ->
                        runOnUiThread {
                            isConnected = false
                            updateStatus("Desconectado")
                            if (throwable == null) {
                                Toast.makeText(this@MosquitoTestActivity, "Desconectado exitosamente", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MosquitoTestActivity, "Error al desconectar: ${throwable.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            }
        }
    }

    private fun publishMessage() {
        if (!isConnected || mqttClient?.state != MqttClientState.CONNECTED) {
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
            mqttClient?.publishWith()
                ?.topic(topic)
                ?.payload(message.toByteArray(StandardCharsets.UTF_8))
                ?.send()
                ?.whenComplete { _, throwable ->
                    runOnUiThread {
                        if (throwable == null) {
                            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            val sentMessage = "[$timestamp] Enviado a $topic: $message"
                            addMessage(sentMessage)
                            Toast.makeText(this@MosquitoTestActivity, "Mensaje enviado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MosquitoTestActivity, "Error al publicar: ${throwable.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al publicar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun subscribeToTopic() {
        if (!isConnected || mqttClient?.state != MqttClientState.CONNECTED) {
            Toast.makeText(this, "No estás conectado al broker MQTT", Toast.LENGTH_SHORT).show()
            return
        }

        val topic = binding.etTopic.text.toString()

        if (topic.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa un tópico", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mqttClient?.subscribeWith()
                ?.topicFilter(topic)
                ?.send()
                ?.whenComplete { subAck, throwable ->
                    runOnUiThread {
                        if (throwable == null) {
                            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            val subscribeMessage = "[$timestamp] Suscrito a: $topic"
                            addMessage(subscribeMessage)
                            Toast.makeText(this@MosquitoTestActivity, "Suscrito a $topic", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MosquitoTestActivity, "Error al suscribirse: ${throwable.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al suscribirse: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun extractHost(brokerUrl: String): String {
        return when {
            brokerUrl.startsWith("tcp://") -> brokerUrl.substring(6).split(":")[0]
            brokerUrl.startsWith("ssl://") -> brokerUrl.substring(6).split(":")[0]
            else -> brokerUrl.split(":")[0]
        }
    }

    private fun extractPort(brokerUrl: String): Int {
        return when {
            brokerUrl.startsWith("tcp://") -> brokerUrl.substring(6).split(":")[1].toIntOrNull() ?: 1883
            brokerUrl.startsWith("ssl://") -> brokerUrl.substring(6).split(":")[1].toIntOrNull() ?: 8883
            else -> brokerUrl.split(":")[1].toIntOrNull() ?: 1883
        }
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
        mqttClient?.disconnect()
        unbindService(serviceConnection)
    }
} 