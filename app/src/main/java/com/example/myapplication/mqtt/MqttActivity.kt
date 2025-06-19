package com.example.myapplication.mqtt

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MqttActivity : AppCompatActivity() {

    private lateinit var mqttClient: MqttAndroidClient
    private lateinit var serverUri: EditText
    private lateinit var topic: EditText
    private lateinit var message: EditText
    private lateinit var connectButton: Button
    private lateinit var publishButton: Button
    private lateinit var subscribeButton: Button
    private lateinit var statusText: TextView
    private lateinit var receivedMessagesText: TextView

    private val TAG = "MqttActivity"

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
        val serverURIString = serverUri.text.toString()
        if (serverURIString.isBlank()) {
            Toast.makeText(this, "Por favor, ingrese la URI del servidor", Toast.LENGTH_SHORT).show()
            return
        }

        val clientId = MqttClient.generateClientId()
        mqttClient = MqttAndroidClient(this.applicationContext, serverURIString, clientId)

        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                statusText.text = "Conectado"
                Log.i(TAG, "connectComplete")
            }

            override fun connectionLost(cause: Throwable?) {
                statusText.text = "Conexión perdida"
                Log.w(TAG, "connectionLost", cause)
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                message?.let {
                    val msg = "Tópico: $topic\nMensaje: ${it.toString()}"
                    Log.i(TAG, msg)
                    val currentText = receivedMessagesText.text.toString()
                    receivedMessagesText.text = if (currentText.isEmpty()) it.toString() else "$currentText\n${it.toString()}"
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.i(TAG, "deliveryComplete")
            }
        })

        val options = MqttConnectOptions()
        options.isCleanSession = true

        try {
            statusText.text = "Conectando..."
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    statusText.text = "Conectado"
                    Toast.makeText(this@MqttActivity, "Conectado al servidor MQTT", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    statusText.text = "Error de conexión: ${exception?.message}"
                    Toast.makeText(this@MqttActivity, "Error al conectar: ${exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "connect onFailure", exception)
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            Log.e(TAG, "connect error", e)
        }
    }

    private fun publishMessage() {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) {
            Toast.makeText(this, "No conectado al servidor", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mqttMessage = MqttMessage(message.text.toString().toByteArray())
            mqttClient.publish(topic.text.toString(), mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Toast.makeText(this@MqttActivity, "Mensaje publicado", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Toast.makeText(this@MqttActivity, "Error al publicar: ${exception?.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun subscribeTopic() {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) {
            Toast.makeText(this, "No conectado al servidor", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mqttClient.subscribe(topic.text.toString(), 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Toast.makeText(this@MqttActivity, "Suscrito al tópico", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Toast.makeText(this@MqttActivity, "Error al suscribirse: ${exception?.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            try {
                mqttClient.disconnect()
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }
} 