package api

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Wraps another attack in a TX/Sleep duty cycle (concept ported from the
 * PortaPack Mayhem "Jammer TX" app): the inner attack runs for [txSeconds],
 * pauses for [sleepSeconds], and repeats. Both timers get pseudo-random
 * jitter so the cycle is never perfectly periodic.
 */
class DutyCycleAttack(
    private val inner: BluetoothAttack,
    private val txSeconds: Int,
    private val sleepSeconds: Int
) : BluetoothAttack {

    override val displayName = inner.displayName
    override val description = inner.description

    private var scope: CoroutineScope? = null
    @Volatile
    private var running = false

    override fun isRunning() = running

    override fun start(context: Context, onLog: (String) -> Unit) {
        if (running) return
        running = true
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope!!.launch {
            onLog("[DUTY] Ciclo TX ${txSeconds}s / Sleep ${sleepSeconds}s (con jitter)")
            while (isActive && running) {
                inner.start(context, onLog)
                onLog("[DUTY] TX activo (${txSeconds}s)")
                jitterDelay(txSeconds * 1000, jitterMs = (txSeconds * 1000) / 2)
                inner.stop()
                onLog("[DUTY] Pausa (${sleepSeconds}s)")
                jitterDelay(sleepSeconds * 1000, jitterMs = (sleepSeconds * 1000) / 2)
            }
        }
    }

    override fun stop() {
        running = false
        scope?.cancel()
        scope = null
        inner.stop()
    }
}
