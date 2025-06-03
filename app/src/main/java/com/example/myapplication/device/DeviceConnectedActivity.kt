package com.example.myapplication.device

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.main.MainActivity

class DeviceConnectedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_connected)

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            // Ir a la pantalla principal
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}