// com/example/myapplication/mqtt/MosquitoTestActivity.kt
package com.example.myapplication.mqtt

import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMosquitoTestBinding
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat
import java.util.*

class MosquitoTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosquitoTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMosquitoTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener { connectToMqtt() }
        binding.btnDisconnect.setOnClickListener { MqttManager.disconnect() }
        binding.btnPublish.setOnClickListener { publishMessage() }
        binding.btnSubscribe.setOnClickListener { subscribeToTopic() }
        binding.btnGirarMotor.setOnClickListener { enviarGiroMotor() }

    }

    private fun connectToMqtt() {
        val serverUri = convertToWebSocketUri(binding.etBroker.text.toString())
        val clientId = binding.etClientId.text.toString()

        MqttManager.connect(this, serverUri, clientId, object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                runOnUiThread {
                    updateStatus("Conectado")
                    Toast.makeText(this@MosquitoTestActivity, "Conectado a $serverURI", Toast.LENGTH_SHORT).show()
                }
            }

            override fun connectionLost(cause: Throwable?) {
                runOnUiThread { updateStatus("Conexión perdida: ${cause?.message}") }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val msg = "[$timestamp] Mensaje de '$topic': ${message.toString()}"
                addMessage(msg)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
    }

    private fun publishMessage() {
        val topic = binding.etTopic.text.toString()
        val msg = binding.etMessage.text.toString()
        if (topic.isBlank() || msg.isBlank()) return

        MqttManager.publish(topic, msg,
            onSuccess = {
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                addMessage("[$ts] Enviado a '$topic': $msg")
            },
            onFailure = {
                addMessage("Error al enviar a '$topic'")
            }
        )
    }

    private fun subscribeToTopic() {
        val topic = binding.etTopic.text.toString()
        if (topic.isBlank()) return

        MqttManager.subscribe(topic,
            onSuccess = {
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                addMessage("[$ts] Suscrito a: $topic")
            },
            onFailure = {
                addMessage("Error al suscribirse a '$topic'")
            }
        )
    }

    private fun enviarGiroMotor() {
        MqttManager.publish("esp32/motor", "GIRO",
            onSuccess = {
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                addMessage("[$ts] Enviado a 'esp32/motor': GIRO")
            },
            onFailure = {
                addMessage("Error al enviar comando GIRO")
            }
        )
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

    private fun updateStatus(status: String) {
        binding.tvStatus.text = status
        binding.tvStatus.setTextColor(if (MqttManager.isConnected) 0xFF4CAF50.toInt() else 0xFFFF0000.toInt())
    }

    private fun addMessage(message: String) {
        val current = binding.tvMessages.text.toString()
        binding.tvMessages.text = if (current.isEmpty()) message else "$current\n$message"
    }

}