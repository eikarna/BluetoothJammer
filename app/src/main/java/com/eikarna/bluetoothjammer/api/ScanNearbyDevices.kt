package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

class ScanNearbyDevices {

    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    private var scanRunnable: Runnable? = null
    private var listener: ((List<BluetoothDeviceInfo>) -> Unit)? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    // Devices are keyed by MAC address so every device is listed exactly once.
    private val devicesMap = ConcurrentHashMap<String, BluetoothDeviceInfo>()

    companion object {
        private const val TAG = "ScanNearbyDevices"
        private const val REFRESH_INTERVAL_MS = 1000L

        private var instance: ScanNearbyDevices? = null

        // Singleton pattern to ensure only one instance exists
        @Synchronized
        fun getInstance(): ScanNearbyDevices {
            return instance ?: ScanNearbyDevices().also { instance = it }
        }
    }

    /** Current snapshot of known (bonded + discovered) devices. */
    val devices: List<BluetoothDeviceInfo>
        get() = devicesMap.values.sortedBy { it.name.lowercase() }

    // Function to start scanning for nearby Bluetooth devices
    @SuppressLint("MissingPermission")
    fun startScanning(context: Context, callback: (List<BluetoothDeviceInfo>) -> Unit) {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        bluetoothAdapter = adapter
        listener = callback

        if (adapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth is disabled, cannot scan.")
            return
        }

        // Always hand the current snapshot to a (re)attached listener first.
        callback(devices)

        if (isScanning) return
        isScanning = true

        // Create a runnable that keeps bonded + discovered devices in sync
        scanRunnable = object : Runnable {
            @SuppressLint("MissingPermission")
            override fun run() {
                Log.d(TAG, "Scanning for nearby devices...")
                collectBondedDevices(adapter)
                // Classic discovery stops on its own after ~12s; keep it alive
                // while the device list is visible.
                try {
                    if (!adapter.isDiscovering) adapter.startDiscovery()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Discovery not permitted", e)
                }
                listener?.invoke(devices)
                handler.postDelayed(this, REFRESH_INTERVAL_MS)
            }
        }
        handler.post(scanRunnable!!)
    }

    @SuppressLint("MissingPermission")
    private fun collectBondedDevices(adapter: BluetoothAdapter) {
        try {
            adapter.bondedDevices?.forEach { device ->
                devicesMap[device.address] = BluetoothDeviceInfo(
                    name = device.name ?: "Unknown Device",
                    address = device.address
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot list bonded devices", e)
        }
    }

    /**
     * Registers a device found through a BluetoothDevice.ACTION_FOUND broadcast.
     * Returns true when the device was not in the list before.
     */
    @SuppressLint("MissingPermission")
    fun addDevice(device: BluetoothDevice): Boolean {
        val info = try {
            BluetoothDeviceInfo(
                name = device.name ?: "Unknown Device",
                address = device.address
            )
        } catch (e: SecurityException) {
            return false
        }

        val previous = devicesMap.put(device.address, info)
        if (previous != info) listener?.invoke(devices)
        return previous == null
    }

    fun clearDevices() {
        devicesMap.clear()
        listener?.invoke(devices)
    }

    // Function to stop scanning
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return
        isScanning = false
        scanRunnable?.let { handler.removeCallbacks(it) }
        scanRunnable = null
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot cancel discovery", e)
        }
    }
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)
