package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*

class BluetoothServer(private val bluetoothAdapter: BluetoothAdapter) {

    private val TAG = "BluetoothServer"

    @SuppressLint("MissingPermission")
    inner class AcceptThread : Thread() {
        val uuid = UUID.randomUUID()
        private val mmServerSocket: BluetoothServerSocket? by lazy {
            bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(uuid.toString().split('-')[0], uuid)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close() // Close the server socket after accepting a connection
                    shouldLoop = false
                }
            }
        }

        private fun manageMyConnectedSocket(socket: BluetoothSocket) {
            // Handle the connected socket (e.g., start a thread for data transfer)
            socket.connect()
            val dataSize = 4096 // Example data size
            val sendBuffer = ByteArray(dataSize) { ((it % 40) + 'A'.toByte()).toByte() }
            socket.outputStream.write(sendBuffer)
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}
