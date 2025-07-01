package com.example.myapplication.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.myapplication.auth.LoginActivity
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.utils.PrefManager
import com.google.android.material.navigation.NavigationView
import com.example.myapplication.R
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.myapplication.utils.SharedPreferencesManager
import android.app.TimePickerDialog
import android.os.Handler
import android.widget.Button
import android.widget.Toast
import com.example.myapplication.mqtt.GlobalMqttClient


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefManager: PrefManager
    private lateinit var sharedPrefManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(this)
        sharedPrefManager = SharedPreferencesManager(this)

        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        val logoutButton = binding.navigationView.findViewById<TextView>(R.id.nav_logout_footer)
        logoutButton.setOnClickListener {
            prefManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val btnAlimentarMascota = findViewById<Button>(R.id.btnAlimentarMascota)
        val btnProgramarAlimentacion = findViewById<Button>(R.id.btnProgramarAlimentacion)

        btnAlimentarMascota.setOnClickListener {
            if (GlobalMqttClient.isConnected()) {
                GlobalMqttClient.publish("esp32/motor", "GIRO")
                Toast.makeText(this, "Comando enviado: Alimentar Mascota", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No hay conexión MQTT activa", Toast.LENGTH_SHORT).show()
            }
        }

        btnProgramarAlimentacion.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = calendar.get(java.util.Calendar.MINUTE)
            val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val horaSeleccionada = String.format("%02d:%02d", selectedHour, selectedMinute)
                val calendarAhora = java.util.Calendar.getInstance()
                val calendarProgramada = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                    set(java.util.Calendar.MINUTE, selectedMinute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    if (before(calendarAhora)) {
                        add(java.util.Calendar.DAY_OF_MONTH, 1)
                    }
                }
                val delayMillis = calendarProgramada.timeInMillis - calendarAhora.timeInMillis
                Toast.makeText(this, "Alimentación programada para las $horaSeleccionada", Toast.LENGTH_LONG).show()
                Handler(mainLooper).postDelayed({
                    for (i in 1..3) {
                        if (GlobalMqttClient.isConnected()) {
                            GlobalMqttClient.publish("esp32/motor", "GIRO")
                        }
                    }
                    Toast.makeText(this, "Comando de giro enviado 3 veces", Toast.LENGTH_SHORT).show()
                }, delayMillis)
            }, hour, minute, true)
            timePicker.show()
        }
    }

    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            R.id.nav_alimentaciones -> {
                val userId = sharedPrefManager.getCurrentUserId()
                val intent = Intent(this, com.example.myapplication.alimentacion.AlimentacionActivity::class.java)
                intent.putExtra("userId", userId)
                startActivity(intent)
            }
            R.id.nav_pair_device -> {
                startActivity(Intent(this, com.example.myapplication.wifi.BluetoothScanActivity::class.java))
            }
            R.id.nav_mosquito_test -> {
                startActivity(Intent(this, com.example.myapplication.mqtt.MosquitoTestActivity::class.java))
            }

        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
