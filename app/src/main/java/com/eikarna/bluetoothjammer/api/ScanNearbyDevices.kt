package api

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.HashSet
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService

enum class DeviceSource { PAIRED, CLASSIC, BLE }

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int? = null,
    val deviceClass: Int? = null,
    val bleAppearance: Int? = null,
    val source: DeviceSource = DeviceSource.CLASSIC,
    val isSpeaker: Boolean = false,
    val speakerConfidence: SpeakerClassifier.Confidence = SpeakerClassifier.Confidence.LOW,
    val speakerReason: String? = null,
    val vendor: String? = null,
    val serviceUuids: List<String>? = null,
) {
    /** Human-readable label derived from the classic device class, e.g. "Altavoz". */
    val deviceTypeLabel: String? get() = SpeakerClassifier.describeDeviceClass(deviceClass)
}

/**
 * Discovers nearby Bluetooth devices combining three sources:
 *  - already paired devices (always shown, anchor),
 *  - classic discovery (ACTION_FOUND), and
 *  - BLE scan results.
 *
 * Results are deduplicated by MAC address and delivered event-driven via the callback.
 */
class ScanNearbyDevices {

    companion object {
        private const val TAG = "ScanNearbyDevices"
        private const val UPDATE_DEBOUNCE_MS = 300L

        @Volatile
        private var instance: ScanNearbyDevices? = null
        fun getInstance(): ScanNearbyDevices =
            instance ?: synchronized(this) {
                instance ?: ScanNearbyDevices().also { instance = it }
            }
    }

    private var appContext: Context? = null
    private var isScanning = false
    private val devices = LinkedHashMap<String, BluetoothDeviceInfo>()
    private var callback: ((List<BluetoothDeviceInfo>) -> Unit)? = null

    private val uiHandler = Handler(Looper.getMainLooper())
    private var updatePending = false

    private var classicReceiver: BroadcastReceiver? = null
    private var bleScanCallback: ScanCallback? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val serviceProbes = HashSet<String>()

    @SuppressLint("MissingPermission")
    fun startScanning(context: Context, cb: (List<BluetoothDeviceInfo>) -> Unit) {
        appContext = context.applicationContext
        callback = cb
        if (isScanning) return
        isScanning = true
        devices.clear()

        seedPairedDevices()
        startClassicDiscovery()
        startBleScan()
        scheduleUpdate()
    }

    fun stopScanning() {
        if (!isScanning) return
        isScanning = false
        val adapter = bluetoothAdapter()
        try {
            adapter?.cancelDiscovery()
        } catch (e: SecurityException) {
            Log.w(TAG, "cancelDiscovery blocked", e)
        }
        unregisterClassicReceiver()
        stopBleScan()
        uiHandler.removeCallbacksAndMessages(null)
        updatePending = false
        callback = null
    }

    private fun bluetoothAdapter(): BluetoothAdapter? {
        val ctx = appContext ?: return null
        val bm = getSystemService(ctx, BluetoothManager::class.java)
        return bm?.adapter
    }

    // ---------- Paired devices (anchor) ----------

    @SuppressLint("MissingPermission")
    private fun seedPairedDevices() {
        val adapter = bluetoothAdapter() ?: return
        val paired = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            Log.w(TAG, "bondedDevices blocked", e)
            null
        } ?: return
        paired.forEach { device ->
            val name = try {
                device.name ?: "Desconocido"
            } catch (e: SecurityException) {
                "Desconocido"
            }
            upsert(
                name, device.address, rssi = null,
                deviceClass = device.bluetoothClass?.deviceClass, bleAppearance = null,
                source = DeviceSource.PAIRED
            )
            probeServicesAsync(device.address)
        }
    }

    // ---------- Classic discovery ----------

    @SuppressLint("MissingPermission")
    private fun startClassicDiscovery() {
        val adapter = bluetoothAdapter() ?: return
        if (!hasPermission(
                if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_SCAN
                else Manifest.permission.BLUETOOTH_ADMIN
            )
        ) return
        registerClassicReceiver()
        val started = try {
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            Log.w(TAG, "startDiscovery blocked", e)
            false
        }
        if (!started) Log.w(TAG, "startDiscovery() returned false (another app may be discovering)")
    }

    private fun registerClassicReceiver() {
        if (classicReceiver != null) return
        val ctx = appContext ?: return
        classicReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= 33) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        val deviceClass: android.bluetooth.BluetoothClass? =
                            if (Build.VERSION.SDK_INT >= 33) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS, android.bluetooth.BluetoothClass::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS)
                            }
                        val rawRssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        if (device != null) {
                            val name = try {
                                device.name ?: "Desconocido"
                            } catch (e: SecurityException) {
                                "Desconocido"
                            }
                            upsert(
                                name, device.address,
                                rssi = if (rawRssi == Short.MIN_VALUE.toInt()) null else rawRssi,
                                deviceClass = deviceClass?.deviceClass, bleAppearance = null,
                                source = DeviceSource.CLASSIC
                            )
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> Log.d(TAG, "Classic discovery finished")
                }
            }
        }
        try {
            ContextCompat.registerReceiver(
                ctx, classicReceiver,
                IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "registerReceiver blocked", e)
        }
    }

    private fun unregisterClassicReceiver() {
        val receiver = classicReceiver ?: return
        try {
            appContext?.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
        classicReceiver = null
    }

    // ---------- BLE scan ----------

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        val adapter = bluetoothAdapter() ?: return
        if (Build.VERSION.SDK_INT >= 31) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        } else if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return
        val scanner = adapter.bluetoothLeScanner ?: return
        if (bleScanCallback == null) {
            bleScanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val record = result.scanRecord
                    val appearance = record?.appearance()
                    val name = try {
                        record?.deviceName ?: result.device.name ?: "Desconocido"
                    } catch (e: SecurityException) {
                        "Desconocido"
                    }
                    upsert(
                        name, result.device.address,
                        rssi = result.rssi,
                        deviceClass = result.device.bluetoothClass?.deviceClass,
                        bleAppearance = appearance,
                        source = DeviceSource.BLE,
                        serviceUuids = record?.serviceUuids?.map {
                            ProfileNames.profileName(it.uuid.toString()) ?: ProfileNames.shortUuid(it.uuid.toString())
                        }
                    )
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "BLE scan failed: $errorCode")
                }
            }
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            scanner.startScan(null, settings, bleScanCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "BLE scan blocked", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        val adapter = bluetoothAdapter() ?: return
        val scanner = adapter.bluetoothLeScanner ?: return
        val cb = bleScanCallback ?: return
        try {
            scanner.stopScan(cb)
        } catch (e: SecurityException) {
            Log.w(TAG, "stopScan blocked", e)
        }
    }

    /** Parse the BLE GAP appearance field (AD type 0x19, 2 bytes little-endian) from a scan record. */
    private fun ScanRecord.appearance(): Int? {
        val bytes = getBytes() ?: return null
        var i = 0
        while (i < bytes.size) {
            val length = bytes[i].toInt() and 0xFF
            if (length == 0) break
            if (i + 1 >= bytes.size) break
            val type = bytes[i + 1].toInt() and 0xFF
            if (type == 0x19 && length >= 3 && i + 3 < bytes.size) {
                return (bytes[i + 2].toInt() and 0xFF) or ((bytes[i + 3].toInt() and 0xFF) shl 8)
            }
            i += length + 1
        }
        return null
    }

    // ---------- Merge + delivery ----------

    /**
     * One-shot SDP probe: fetches the service records of a remote device once
     * (classic BR/EDR) and stores the readable profile names. BLE devices get
     * their services directly from the scan record instead.
     */
    @SuppressLint("MissingPermission")
    private fun probeServicesAsync(address: String) {
        val key = address.lowercase()
        synchronized(serviceProbes) {
            if (serviceProbes.contains(key)) return
            serviceProbes.add(key)
        }
        serviceScope.launch {
            delay(1500)
            val device = runCatching { bluetoothAdapter()?.getRemoteDevice(address) }.getOrNull() ?: return@launch
            runCatching { device.fetchUuidsWithSdp() }
            delay(3000)
            val uuids = runCatching { device.uuids }.getOrNull()
            val names = uuids?.map {
                ProfileNames.profileName(it.toString()) ?: ProfileNames.shortUuid(it.toString())
            }
            if (!names.isNullOrEmpty()) {
                val k = address.lowercase()
                val existing = devices[k] ?: return@launch
                devices[k] = existing.copy(serviceUuids = names)
                scheduleUpdate()
            }
        }
    }

    private fun upsert(
        name: String,
        address: String,
        rssi: Int?,
        deviceClass: Int?,
        bleAppearance: Int?,
        source: DeviceSource,
        serviceUuids: List<String>? = null
    ) {
        val key = address.lowercase()
        val existing = devices[key]
        val merged: BluetoothDeviceInfo = if (existing == null) {
            BluetoothDeviceInfo(
                name, address, rssi, deviceClass, bleAppearance, source,
                vendor = OuiVendor.vendorFor(address),
                serviceUuids = serviceUuids
            )
        } else {
            existing.copy(
                name = if (existing.name == "Desconocido") name else existing.name,
                rssi = rssi ?: existing.rssi,
                deviceClass = existing.deviceClass ?: deviceClass,
                bleAppearance = existing.bleAppearance ?: bleAppearance,
                source = if (existing.source == DeviceSource.PAIRED) DeviceSource.PAIRED else source,
                vendor = existing.vendor ?: OuiVendor.vendorFor(address),
                serviceUuids = existing.serviceUuids ?: serviceUuids,
            )
        }
        val classification = SpeakerClassifier.classify(merged.name, merged.deviceClass, merged.bleAppearance)
        devices[key] = merged.copy(
            isSpeaker = classification.isSpeaker,
            speakerConfidence = classification.confidence,
            speakerReason = classification.reason,
        )
        if (existing == null && source != DeviceSource.BLE) {
            probeServicesAsync(address)
        }
        scheduleUpdate()
    }

    private fun scheduleUpdate() {
        if (updatePending) return
        updatePending = true
        uiHandler.postDelayed({
            updatePending = false
            if (!isScanning) return@postDelayed
            val sorted = devices.values.sortedWith(
                compareByDescending<BluetoothDeviceInfo> { it.isSpeaker }
                    .thenByDescending { it.rssi ?: Int.MIN_VALUE }
            )
            callback?.invoke(sorted)
        }, UPDATE_DEBOUNCE_MS)
    }

    private fun hasPermission(permission: String): Boolean {
        val ctx = appContext ?: return false
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
    }
}
