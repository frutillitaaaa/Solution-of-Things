package com.example.myapplication.mqtt

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMosquitoTestBinding
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat
import java.util.*

class MosquitoTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosquitoTestBinding
    private var mqttClient: MqttAndroidClient? = null
    private var isConnected = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // Servicio MQTT conectado
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Servicio MQTT desconectado
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMosquitoTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        bindMqttService()
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

    private fun bindMqttService() {
        val serviceIntent = Intent(this, org.eclipse.paho.android.service.MqttService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun connectToMqtt() {
        val broker = binding.etBroker.text.toString()
        val clientId = binding.etClientId.text.toString()

        if (broker.isEmpty() || clientId.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mqttClient = MqttAndroidClient(this, broker, clientId)
            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    runOnUiThread {
                        isConnected = true
                        updateStatus("Conectado a $serverURI")
                        Toast.makeText(this@MosquitoTestActivity, "Conectado exitosamente", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    runOnUiThread {
                        isConnected = false
                        updateStatus("Conexión perdida: ${cause?.message}")
                        Toast.makeText(this@MosquitoTestActivity, "Conexión perdida", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    runOnUiThread {
                        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        val receivedMessage = "[$timestamp] $topic: ${message?.toString()}"
                        addMessage(receivedMessage)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    runOnUiThread {
                        Toast.makeText(this@MosquitoTestActivity, "Mensaje enviado", Toast.LENGTH_SHORT).show()
                    }
                }
            })

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
            }

            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    runOnUiThread {
                        updateStatus("Conectando...")
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    runOnUiThread {
                        updateStatus("Error de conexión: ${exception?.message}")
                        Toast.makeText(this@MosquitoTestActivity, "Error al conectar: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun disconnectFromMqtt() {
        mqttClient?.disconnect(null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                runOnUiThread {
                    isConnected = false
                    updateStatus("Desconectado")
                    Toast.makeText(this@MosquitoTestActivity, "Desconectado exitosamente", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                runOnUiThread {
                    updateStatus("Error al desconectar: ${exception?.message}")
                    Toast.makeText(this@MosquitoTestActivity, "Error al desconectar: ${exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
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
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    runOnUiThread {
                        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        val sentMessage = "[$timestamp] Enviado a $topic: $message"
                        addMessage(sentMessage)
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    runOnUiThread {
                        Toast.makeText(this@MosquitoTestActivity, "Error al publicar: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
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
            mqttClient?.subscribe(topic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    runOnUiThread {
                        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        val subscribeMessage = "[$timestamp] Suscrito a: $topic"
                        addMessage(subscribeMessage)
                        Toast.makeText(this@MosquitoTestActivity, "Suscrito a $topic", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    runOnUiThread {
                        Toast.makeText(this@MosquitoTestActivity, "Error al suscribirse: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error al suscribirse: ${e.message}", Toast.LENGTH_LONG).show()
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