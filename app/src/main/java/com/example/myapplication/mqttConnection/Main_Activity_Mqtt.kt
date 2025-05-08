package com.example.myapplication.mqttConnection
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainMqttBinding





// 2. Modifica tu Activity:
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainMqttBinding // Cambia por el nombre real de tu binding
    private lateinit var mqttHelper: MqttHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMqttBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mqttHelper = MqttHelper(this)
        mqttHelper.connect()

        binding.buttonPublish.setOnClickListener {
            mqttHelper.publish("test/topic", "Hola desde Android!")
        }
    }
}