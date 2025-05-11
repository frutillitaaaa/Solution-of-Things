package com.example.myapplication.wifi

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityBluetoothScanBinding
import java.util.*

class BluetoothScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBluetoothScanBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: BluetoothDeviceAdapter
    private var bluetoothGatt: BluetoothGatt? = null

    private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    private inner class GattCallback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@BluetoothScanActivity, "Conectado", Toast.LENGTH_SHORT).show()
                }
                gatt?.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                
                if (characteristic != null) {
                    val command = "start_ap"
                    characteristic.value = command.toByteArray()
                    gatt?.writeCharacteristic(characteristic)
                    
                    runOnUiThread {
                        Toast.makeText(this@BluetoothScanActivity, "Comando enviado, iniciando configuración WiFi...", Toast.LENGTH_LONG).show()
                        // Esperar un momento para que el ESP32 inicie el AP
                        Thread.sleep(2000)
                        val intent = Intent(this@BluetoothScanActivity, WifiSetupActivity::class.java).apply {
                            putExtra("device_address", gatt?.device?.address)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startBluetoothScan()
        } else {
            Toast.makeText(this, "Se requieren permisos de Bluetooth", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    val name = device.name ?: ""
                    if (name.startsWith("PetFeeder") && !bluetoothDevices.any { it.address == device.address }) {
                        bluetoothDevices.add(device)
                        deviceAdapter.notifyItemInserted(bluetoothDevices.size - 1)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setupUI()
        checkPermissions()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
        bluetoothGatt?.close()
    }

    private fun setupUI() {
        deviceAdapter = BluetoothDeviceAdapter(bluetoothDevices) { device ->
            connectToDevice(device)
        }

        binding.recyclerViewDevices.apply {
            layoutManager = LinearLayoutManager(this@BluetoothScanActivity)
            adapter = deviceAdapter
        }

        binding.buttonScan.setOnClickListener {
            checkPermissions()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Se requieren permisos de Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Conectando a ${device.name}...", Toast.LENGTH_SHORT).show()
        bluetoothGatt = device.connectGatt(this, false, GattCallback())
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            bluetoothPermissionLauncher.launch(permissionsToRequest)
        } else {
            startBluetoothScan()
        }
    }

    private fun startBluetoothScan() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            return
        }

        bluetoothDevices.clear()
        deviceAdapter.notifyDataSetChanged()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.startDiscovery()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startBluetoothScan()
            } else {
                Toast.makeText(this, "Bluetooth debe estar activado", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }
} 