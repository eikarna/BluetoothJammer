package api

import java.util.concurrent.ConcurrentHashMap

/**
 * Central coordinator for attack sessions (pattern ported from the
 * PIXELQUADRO07 fork). Keeps running attacks registered per target address so:
 *  - a global stop cancels everything, even if the triggering Activity died;
 *  - several targets can be attacked simultaneously (multi-target).
 *
 * The attack classes own their internal logic; the manager only tracks their
 * lifecycle and exposes a global stop.
 */
object AttackManager {

    private val sessions = ConcurrentHashMap<String, MutableList<BluetoothAttack>>()

    @Volatile
    var isAttacking = false
        private set

    /** Registers an already-started attack targeting [address]. */
    fun track(address: String, attack: BluetoothAttack) {
        isAttacking = true
        sessions.getOrPut(address.lowercase()) { mutableListOf() }.add(attack)
    }

    /** Addresses with at least one active attack. */
    fun activeTargets(): Set<String> = sessions.keys.toSet()

    /** Stops and forgets every registered attack. */
    fun stopAll() {
        isAttacking = false
        sessions.values.forEach { attacks -> attacks.forEach { runCatching { it.stop() } } }
        sessions.clear()
    }
}
