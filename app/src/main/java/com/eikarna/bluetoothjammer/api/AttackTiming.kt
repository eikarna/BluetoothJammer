package api

import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Rate limiting helper: pauses the coroutine for [baseMs] plus a pseudo-random
 * jitter (0..jitterMs) to avoid a perfectly periodic, easy-to-profile signature.
 * With baseMs == 0 the attack runs at maximum speed.
 */
internal suspend fun jitterDelay(baseMs: Int, jitterMs: Int = 100) {
    if (baseMs <= 0) return
    delay((baseMs + Random.nextInt(0, jitterMs + 1)).toLong())
}
