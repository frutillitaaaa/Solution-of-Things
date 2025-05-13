package com.example.myapplication.mqttConnection

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class MainActivity : AppCompatActivity() {
    private lateinit var btnConnect: Button
    private lateinit var btnSubscribe: Button
    private lateinit var btnPublish: Button
    private lateinit var etTopic: EditText
    private lateinit var etMessage: EditText
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_mqtt)

        // Inicializar vistas
        btnConnect = findViewById(R.id.btnConnect)
        btnSubscribe = findViewById(R.id.btnSubscribe)
        btnPublish = findViewById(R.id.btnPublish)
        etTopic = findViewById(R.id.etTopic)
        etMessage = findViewById(R.id.etMessage)
        tvLog = findViewById(R.id.tvLog)

        // Inicializar MQTT
        MqttHelper.init()

        // Botón conectar
        btnConnect.setOnClickListener {
            appendLog("Intentando conectar...")
            MqttHelper.connect()
        }

        // Botón suscribir
        btnSubscribe.setOnClickListener {
            val topic = etTopic.text.toString().trim()
            if (topic.isNotEmpty()) {
                appendLog("Suscribiéndose a: $topic")
                MqttHelper.subscribe(topic)
            }
        }

        // Botón publicar
        btnPublish.setOnClickListener {
            val topic = etTopic.text.toString().trim()
            val message = etMessage.text.toString().trim()
            if (topic.isNotEmpty() && message.isNotEmpty()) {
                appendLog("Publicando en $topic: $message")
                MqttHelper.publish(topic, message)
            }
        }

        // Configurar callback para actualizar la UI
        MqttHelper.client.setCallback(object : org.eclipse.paho.client.mqttv3.MqttCallback {
            override fun connectionLost(cause: Throwable) {
                appendLog("Conexión perdida: ${cause.message}")
            }

            override fun messageArrived(topic: String, message: org.eclipse.paho.client.mqttv3.MqttMessage) {
                val payload = message.payload.decodeToString()
                appendLog("Mensaje recibido de $topic: $payload")
            }

            override fun deliveryComplete(token: org.eclipse.paho.client.mqttv3.IMqttDeliveryToken) {
                // no-op
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        appendLog("Desconectando MQTT...")
        MqttHelper.disconnect()
    }

    private fun appendLog(text: String) {
        tvLog.append("$text\n")
    }
}