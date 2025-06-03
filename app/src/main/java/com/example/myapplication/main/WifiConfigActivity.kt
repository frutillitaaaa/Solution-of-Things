package com.example.myapplication.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.auth.LoginActivity
import com.example.myapplication.databinding.ActivityWifiConfigBinding
import com.example.myapplication.utils.PrefManager
import com.example.myapplication.wifi.BluetoothScanActivity

class WifiConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWifiConfigBinding
    private lateinit var prefManager: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(this)
        setupUI()
    }

    private fun setupUI() {
        binding.textWelcome.text = "Bienvenido, ${prefManager.getUserEmail()}"

        binding.buttonWifiSetup.setOnClickListener {
            val intent = Intent(this, BluetoothScanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }

        binding.buttonLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        prefManager.clearSession()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
