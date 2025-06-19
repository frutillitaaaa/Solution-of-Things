package com.example.myapplication.mqtt

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.*

class MqttActivity : AppCompatActivity() {
    private var mqttClient: MqttClient? = null
    private lateinit var serverUri: EditText
    private lateinit var topic: EditText
    private lateinit var message: EditText
    private lateinit var connectButton: Button
    private lateinit var publishButton: Button
    private lateinit var subscribeButton: Button
    private lateinit var statusText: TextView
    private lateinit var receivedMessagesText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt)

        // Inicializar vistas
        serverUri = findViewById(R.id.serverUriInput)
        topic = findViewById(R.id.topicInput)
        message = findViewById(R.id.messageInput)
        connectButton = findViewById(R.id.connectButton)
        publishButton = findViewById(R.id.publishButton)
        subscribeButton = findViewById(R.id.subscribeButton)
        statusText = findViewById(R.id.statusText)
        receivedMessagesText = findViewById(R.id.receivedMessagesText)

        connectButton.setOnClickListener { connectToServer() }
        publishButton.setOnClickListener { publishMessage() }
        subscribeButton.setOnClickListener { subscribeTopic() }
    }

    private fun connectToServer() {
        try {
            val clientId = "AndroidClient-" + UUID.randomUUID().toString()
            mqttClient = MqttClient(
                serverUri.text.toString(),
                clientId,
                MemoryPersistence()
            )

            val options = MqttConnectOptions()
            options.isCleanSession = true

            statusText.text = "Conectando..."
            mqttClient?.connect(options)
            statusText.text = "Conectado"
            Toast.makeText(this, "Conectado al servidor MQTT", Toast.LENGTH_SHORT).show()

        } catch (e: MqttException) {
            e.printStackTrace()
            statusText.text = "Error: ${e.message}"
            Toast.makeText(this, "Error al conectar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun publishMessage() {
        try {
            mqttClient?.let { client ->
                if (client.isConnected) {
                    val mqttMessage = MqttMessage(message.text.toString().toByteArray())
                    client.publish(topic.text.toString(), mqttMessage)
                    Toast.makeText(this, "Mensaje publicado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No conectado al servidor", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al publicar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun subscribeTopic() {
        try {
            mqttClient?.let { client ->
                if (client.isConnected) {
                    client.subscribe(topic.text.toString()) { _, message ->
                        runOnUiThread {
                            val currentText = receivedMessagesText.text.toString()
                            val newMessage = String(message.payload)
                            receivedMessagesText.text = "$currentText\n$newMessage"
                        }
                    }
                    Toast.makeText(this, "Suscrito al tópico", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No conectado al servidor", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al suscribirse: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mqttClient?.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
} 