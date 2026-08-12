package api

import kotlin.random.Random

/**
 * Payload byte-pattern selectable per attack (analog of the signal types of the
 * PortaPack Mayhem "Jammer TX": noise, fixed pattern, sweep, chirp...).
 * At protocol level the exact shape rarely matters for the effect, but it is
 * useful to study and keeps the tool flexible.
 */
enum class PayloadPattern(val displayName: String) {
    RANDOM("Ruido aleatorio"),
    FIXED("Patrón fijo (A-Z)"),
    SAWTOOTH("Sierra (0-255)"),
    CHIRP("Ondulado (chirp)");

    /** Builds a [size]-byte payload buffer with this pattern. */
    fun buffer(size: Int): ByteArray = when (this) {
        RANDOM -> ByteArray(size) { Random.nextInt(256).toByte() }
        FIXED -> ByteArray(size) { ('A'.code + (it % 26)).toByte() }
        SAWTOOTH -> ByteArray(size) { (it % 256).toByte() }
        CHIRP -> ByteArray(size) { ((it * 13) % 256).toByte() }
    }
}
