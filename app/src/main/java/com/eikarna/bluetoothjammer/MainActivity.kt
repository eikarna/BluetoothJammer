package com.eikarna.bluetoothjammer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import api.BluetoothDeviceInfo
import api.ScanNearbyDevices

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private lateinit var devices: List<BluetoothDeviceInfo>
    private val scanner = ScanNearbyDevices.getInstance()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.deviceListView)

        // Check and request necessary permissions
        checkBluetoothStatusAndPermissions()

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    private fun checkBluetoothStatusAndPermissions() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            // Bluetooth is either not supported or not enabled, show dialog
            showBluetoothDisabledDialog()
        } else {
            // Bluetooth is enabled, proceed with permission checks
            checkPermissionsAndStartScanning()
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            // Initialize the ListView and Adapter
            val action: String? = intent.action
            Log.d("MainActivity", "Action: $action")
            println("Action: $action")
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Discovery has found a device. Get the BluetoothDevice
                Log.d("MainActivity", "Device Found")
                println("Device Found")
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val deviceInfo = BluetoothDeviceInfo(
                    name = device?.name ?: "Unknown Device",
                    address = device?.address ?: "00:00:00:00"
                )

                // Add the device to the list and notify the adapter
                ScanNearbyDevices.devicesList.add(deviceInfo)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showBluetoothDisabledDialog() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, 1)
    }

    private fun checkPermissionsAndStartScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and higher, request specific Bluetooth permissions
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            if (!hasPermissions(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
            } else {
                // Permissions already granted, start scanning
                startScanningForDevices()
            }
        } else {
            // For older Android versions, request Bluetooth and location permissions
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            if (!hasPermissions(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
            } else {
                // Permissions already granted, start scanning
                startScanningForDevices()
            }
        }
    }

    // Handle the permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions were granted
                startScanningForDevices()
            } else {
                // Permission denied, show a message
                Toast.makeText(
                    this,
                    "Permissions are required to scan for Bluetooth devices",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startScanningForDevices() {
        // Start scanning for nearby Bluetooth devices
        scanner.startScanning(this) { discoveredDevices ->
            devices = discoveredDevices
            val deviceNames = devices.map { "${it.name} (${it.address})" }

            // Set up ArrayAdapter to show the list of devices
            deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
            listView.adapter = deviceListAdapter

            // Handle item click events
            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedDevice = devices[position]
                showDeviceInfo(selectedDevice)
            }
        }
    }

    // Show device details in a dialog
    private fun showDeviceInfo(device: BluetoothDeviceInfo) {
        val message =
            "Name: ${device.name}\nAddress: ${device.address}"

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Device Info")
            .setMessage(message)
            .setPositiveButton("Attack") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, AttackActivity::class.java).apply {
                    putExtra("DEVICE_NAME", device.name)
                    putExtra("ADDRESS", device.address)
                    putExtra("THREADS", 8)
                }

                // Start AttackActivity
                startActivity(intent)
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Copy Info") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = android.content.ClipData.newPlainText("Device Info", message)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Device info copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        dialogBuilder.create().show()
    }

    // Stop scanning when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        scanner.stopScanning()
    }

    // Stop scanning when change to another intent
    override fun onPause() {
        super.onPause()
        scanner.stopScanning()
    }

    // Resume scanning
    override fun onResume() {
        super.onResume()
        scanner.resumeScanning()
    }
}
