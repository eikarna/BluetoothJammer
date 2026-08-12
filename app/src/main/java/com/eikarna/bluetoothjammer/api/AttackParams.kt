package api

/**
 * Aggregated parameters for launching an attack. Mirrors the PortaPack Mayhem
 * "Jammer TX" controls (TX/Sleep duty cycle + jitter, signal types), plus the
 * RFCOMM channel-flood options (payload size, bombardment mode).
 */
data class AttackParams(
    val threads: Int = 8,
    val rateDelayMs: Int = 0,
    val txSeconds: Int = 0,
    val sleepSeconds: Int = 0,
    val payloadPattern: PayloadPattern = PayloadPattern.FIXED,
    val bombard: Boolean = false,
    val payloadSize: Int = 0
)
