package com.example.myapplication.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityWifiSetupBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.concurrent.TimeUnit
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.net.ConnectivityManager

class WifiSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWifiSetupBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiAdapter: WifiNetworkAdapter
    private var deviceAddress: String? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    private var selectedSsid: String? = null
    private var selectedPassword: String? = null

    private val wifiPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            scanWifiNetworks()
        } else {
            Toast.makeText(this, "Se requieren permisos de WiFi", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceAddress = intent.getStringExtra("device_address")
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        wifiAdapter = WifiNetworkAdapter { ssid ->
            showPasswordDialog(ssid)
        }

        binding.recyclerViewWifiNetworks.apply {
            layoutManager = LinearLayoutManager(this@WifiSetupActivity)
            adapter = wifiAdapter
        }

        binding.buttonConnect.setOnClickListener {
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            wifiPermissionLauncher.launch(permissionsToRequest)
        } else {
            scanWifiNetworks()
        }
    }

    private fun scanWifiNetworks() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            wifiManager.startScan()
            val results = wifiManager.scanResults
            wifiAdapter.submitList(results)
        }
    }

    private fun showPasswordDialog(ssid: String) {
        val dialog = WifiPasswordDialog(this) { password ->
            selectedSsid = ssid
            selectedPassword = password
            connectToEsp32Ap()
        }
        dialog.show()
    }

    private fun connectToEsp32Ap() {
        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid("PetFeeder_Setup_EA9E")
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectivityManager.bindProcessToNetwork(network)
                runOnUiThread {
                    Toast.makeText(this@WifiSetupActivity, "Conectado al AP del PetFeeder", Toast.LENGTH_SHORT).show()
                }
                sendWifiCredentialsToEsp32()
            }
            override fun onUnavailable() {
                super.onUnavailable()
                runOnUiThread {
                    Toast.makeText(this@WifiSetupActivity, "No se pudo conectar al AP del PetFeeder", Toast.LENGTH_LONG).show()
                }
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    private fun sendWifiCredentialsToEsp32() {
        val ssid = selectedSsid ?: return
        val password = selectedPassword ?: return

        val json = JSONObject().apply {
            put("ssid", ssid)
            put("password", password)
        }

        val request = Request.Builder()
            .url("http://192.168.4.1/wifi")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json.toString()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@WifiSetupActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@WifiSetupActivity, "Credenciales enviadas correctamente", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@WifiSetupActivity, "Error al enviar credenciales: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
} 