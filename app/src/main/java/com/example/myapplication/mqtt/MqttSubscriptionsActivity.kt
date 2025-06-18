package com.example.myapplication.mqtt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMqttSubscriptionsBinding
import com.example.myapplication.R
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*

class MqttSubscriptionsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMqttSubscriptionsBinding
    private lateinit var mqttClient: MqttAndroidClient
    private lateinit var subscriptionsAdapter: MqttSubscriptionsAdapter
    private val subscriptions = mutableListOf<MqttSubscription>()
    
    companion object {
        private const val TAG = "MqttSubscriptions"
        private const val SERVER_URI = "tcp://broker.hivemq.com:1883" // Broker público de ejemplo
        private const val CLIENT_ID = "AndroidClient_${System.currentTimeMillis()}"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMqttSubscriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupMqttClient()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.mqtt_subscriptions)
        }
    }
    
    private fun setupRecyclerView() {
        subscriptionsAdapter = MqttSubscriptionsAdapter(subscriptions) { subscription ->
            showSubscriptionDetails(subscription)
        }
        
        binding.recyclerViewSubscriptions.apply {
            layoutManager = LinearLayoutManager(this@MqttSubscriptionsActivity)
            adapter = subscriptionsAdapter
        }
    }
    
    private fun setupMqttClient() {
        mqttClient = MqttAndroidClient(this, SERVER_URI, CLIENT_ID)
        
        try {
            val token = mqttClient.connect()
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Conexión MQTT exitosa")
                    Toast.makeText(this@MqttSubscriptionsActivity, getString(R.string.mqtt_connected), Toast.LENGTH_SHORT).show()
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Error de conexión MQTT: ${exception?.message}")
                    Toast.makeText(this@MqttSubscriptionsActivity, "${getString(R.string.mqtt_connection_error)}: ${exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al conectar: ${e.message}")
            Toast.makeText(this, "${getString(R.string.mqtt_connection_error)}: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        // Configurar callback para mensajes recibidos
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d(TAG, "Conexión completada, reconnect: $reconnect")
            }
            
            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Conexión perdida: ${cause?.message}")
                Toast.makeText(this@MqttSubscriptionsActivity, getString(R.string.mqtt_connection_lost), Toast.LENGTH_SHORT).show()
            }
            
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Mensaje recibido en $topic: ${message?.toString()}")
                runOnUiThread {
                    val subscription = subscriptions.find { it.topic == topic }
                    subscription?.let {
                        it.lastMessage = message?.toString() ?: ""
                        it.timestamp = Date()
                        subscriptionsAdapter.notifyDataSetChanged()
                    }
                }
            }
            
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "Entrega completada")
            }
        })
    }
    
    private fun setupClickListeners() {
        binding.fabAddSubscription.setOnClickListener {
            showAddSubscriptionDialog()
        }
    }
    
    private fun showAddSubscriptionDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_add_subscription, null)
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.mqtt_add_subscription))
            .setView(dialogBinding)
            .setPositiveButton(getString(R.string.mqtt_subscribe)) { _, _ ->
                val topic = dialogBinding.findViewById<android.widget.EditText>(R.id.etTopic).text.toString()
                val qos = dialogBinding.findViewById<android.widget.Spinner>(R.id.spinnerQos).selectedItemPosition
                
                if (topic.isNotEmpty()) {
                    subscribeToTopic(topic, qos)
                } else {
                    Toast.makeText(this, getString(R.string.mqtt_please_enter_topic), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.mqtt_cancel), null)
            .show()
    }
    
    private fun subscribeToTopic(topic: String, qos: Int) {
        try {
            val token = mqttClient.subscribe(topic, qos)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Suscripción exitosa a $topic")
                    runOnUiThread {
                        val subscription = MqttSubscription(topic, qos)
                        subscriptions.add(subscription)
                        subscriptionsAdapter.notifyItemInserted(subscriptions.size - 1)
                        Toast.makeText(this@MqttSubscriptionsActivity, getString(R.string.mqtt_subscription_success, topic), Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Error en suscripción: ${exception?.message}")
                    runOnUiThread {
                        Toast.makeText(this@MqttSubscriptionsActivity, "Error al suscribirse: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al suscribirse: ${e.message}")
            Toast.makeText(this, "Error al suscribirse: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showSubscriptionDetails(subscription: MqttSubscription) {
        val message = """
            Tópico: ${subscription.topic}
            QoS: ${subscription.qos}
            Último mensaje: ${subscription.lastMessage ?: "Ninguno"}
            Timestamp: ${subscription.timestamp ?: "N/A"}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.mqtt_subscription_details))
            .setMessage(message)
            .setPositiveButton(getString(R.string.mqtt_unsubscribe)) { _, _ ->
                unsubscribeFromTopic(subscription)
            }
            .setNegativeButton(getString(R.string.mqtt_close), null)
            .show()
    }
    
    private fun unsubscribeFromTopic(subscription: MqttSubscription) {
        try {
            val token = mqttClient.unsubscribe(subscription.topic)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Desuscripción exitosa de ${subscription.topic}")
                    runOnUiThread {
                        val index = subscriptions.indexOf(subscription)
                        if (index != -1) {
                            subscriptions.removeAt(index)
                            subscriptionsAdapter.notifyItemRemoved(index)
                        }
                        Toast.makeText(this@MqttSubscriptionsActivity, getString(R.string.mqtt_unsubscription_success, subscription.topic), Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Error en desuscripción: ${exception?.message}")
                    runOnUiThread {
                        Toast.makeText(this@MqttSubscriptionsActivity, "Error al desuscribirse: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desuscribirse: ${e.message}")
            Toast.makeText(this, "Error al desuscribirse: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            if (mqttClient.isConnected) {
                mqttClient.disconnect()
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        }
    }
} 