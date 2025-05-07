package com.example.myapplication.mqttConnection

import com.example.myapplication.R

class MainActivity : AppCompatActivity() {
    private lateinit var mqttClient: MqttAndroidClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_mqtt)

        // Configuración del cliente MQTT
        val serverUri = "tcp://test.mosquitto.org:1883" // Broker público para pruebas
        val clientId = "AndroidClient_${System.currentTimeMillis()}"

        mqttClient = MqttAndroidClient(this, serverUri, clientId).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "Conexión perdida", cause)
                    runOnUiThread {
                        txtStatus.text = "Desconectado"
                        txtStatus.setTextColor(Color.RED)
                    }
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    Log.d("MQTT", "Mensaje recibido: ${message.toString()} en $topic")
                    runOnUiThread {
                        txtLog.append("\n${message.toString()}")
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MQTT", "Mensaje entregado")
                }
            })
        }

        btnConnect.setOnClickListener {
            connectToMqtt()
        }
    }

    private fun connectToMqtt() {
        try {
            val options = MqttConnectOptions().apply {
                cleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
            }

            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Conectado con éxito")
                    runOnUiThread {
                        txtStatus.text = "Conectado"
                        txtStatus.setTextColor(Color.GREEN)
                    }
                    subscribeToTopic("test/topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Error de conexión", exception)
                    runOnUiThread {
                        txtStatus.text = "Error de conexión"
                        txtStatus.setTextColor(Color.RED)
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun subscribeToTopic(topic: String) {
        try {
            mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Suscrito a $topic")
                    runOnUiThread {
                        txtLog.append("\nSuscrito a $topic")
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Error al suscribirse", exception)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        mqttClient.disconnect()
        super.onDestroy()
    }
}