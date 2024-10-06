package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService

class ScanNearbyDevices {

    private var isScanning = false
    private var handler: Handler = Handler()
    private lateinit var runnable: Runnable

    companion object {
        private var instance: ScanNearbyDevices? = null

        // Singleton pattern to ensure only one instance exists
        fun getInstance(): ScanNearbyDevices {
            if (instance == null) {
                instance = ScanNearbyDevices()
            }
            return instance!!
        }

        val devicesList = mutableListOf<BluetoothDeviceInfo>()
    }

    // Function to start scanning for nearby Bluetooth devices
    fun startScanning(context: Context, callback: (List<BluetoothDeviceInfo>) -> Unit) {
        val bluetoothManager: BluetoothManager? =
            getSystemService(context, BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

        if (bluetoothAdapter?.isEnabled == true && !isScanning) {
            isScanning = true

            // Create a runnable to scan every second
            runnable = object : Runnable {
                @SuppressLint("MissingPermission")
                override fun run() {
                    Log.d("ScanNearbyDevices", "Scanning for nearby devices...")
                    println("Scanning nearby devices...")
                    devicesList.clear() // Clear previous results

                    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                    pairedDevices?.forEach { device ->
                        device.fetchUuidsWithSdp()
                        val deviceInfo = BluetoothDeviceInfo(
                            name = device.name ?: "Unknown Device",
                            address = device.address
                        )
                        devicesList.add(deviceInfo)
                    }

                    // Return the list of devices to the callback function
                    callback(devicesList)

                    // Schedule the next scan after 1 second
                    handler.postDelayed(this, 1000)
                }
            }

            // Start the periodic scanning
            handler.post(runnable)
        } else {
            Log.e("ScanNearbyDevices", "Bluetooth is disabled or already scanning.")
        }
    }

    // Function to stop scanning
    fun stopScanning() {
        if (isScanning) {
            isScanning = false
            handler.removeCallbacks(runnable) // Stop the periodic scanning
        }
    }

    fun resumeScanning() {
        if (!isScanning) {
            isScanning = true
            handler.post(runnable)
        }
    }
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)
