package com.eikarna.bluetoothjammer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.widget.doAfterTextChanged
import api.AttackManager
import api.AttackParams
import api.AttackType
import api.BluetoothAttack
import api.PayloadPattern
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import util.Logger

class AttackActivity : AppCompatActivity() {

    private lateinit var viewDeviceName: MaterialTextView
    private lateinit var viewDeviceAddress: MaterialTextView
    private lateinit var viewThreads: TextInputEditText
    private lateinit var viewDelay: TextInputEditText
    private lateinit var viewTxSeconds: TextInputEditText
    private lateinit var viewSleepSeconds: TextInputEditText
    private lateinit var spinnerPayloadPattern: Spinner
    private lateinit var viewPayloadSize: TextInputEditText
    private lateinit var switchBombard: MaterialSwitch
    private lateinit var spinnerAttackType: Spinner
    private lateinit var buttonStartStop: MaterialButton
    private lateinit var logAttack: MaterialTextView
    private lateinit var switchLog: MaterialSwitch

    private var deviceName: String = "Unknown Device"
    private var address: String = "Unknown Address"
    private val targets: MutableList<Pair<String, String>> = mutableListOf()
    private var threads: Int = 8
    private var delayMs: Int = 0
    private var txSeconds: Int = 0
    private var sleepSeconds: Int = 0
    private var payloadSize: Int = 0
    private var bombard: Boolean = false

    private val startedAttacks = mutableListOf<BluetoothAttack>()
    private var startTimeMs = 0L
    private var connEvents = 0
    private var dataEvents = 0
    private var retryEvents = 0

    companion object {
        var FrameworkVersion = 1.4
        var loggingStatus = true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.attack_layout)

        // Targets: either a single device (legacy extras) or a list (multi-target)
        val targetsExtra = intent.getStringArrayListExtra("EXTRA_TARGETS")
        if (targetsExtra != null) {
            targetsExtra.forEach { line ->
                val parts = line.split("|", limit = 2)
                if (parts.size == 2) targets.add(parts[0] to parts[1])
            }
        }
        if (targets.isEmpty()) {
            deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Unknown Device"
            address = intent.getStringExtra("ADDRESS") ?: "Unknown Address"
            targets.add(deviceName to address)
        } else {
            deviceName = if (targets.size == 1) targets[0].first else "${targets.size} objetivos"
            address = targets.joinToString(", ") { it.second }
        }
        threads = intent.getIntExtra("THREADS", 8)

        // Get Element ID
        viewDeviceName = findViewById(R.id.textViewDeviceName)
        viewDeviceAddress = findViewById(R.id.textViewAddress)
        viewThreads = findViewById(R.id.editTextThreads)
        viewDelay = findViewById(R.id.editTextDelay)
        viewTxSeconds = findViewById(R.id.editTextTxSeconds)
        viewSleepSeconds = findViewById(R.id.editTextSleepSeconds)
        spinnerPayloadPattern = findViewById(R.id.spinnerPayloadPattern)
        viewPayloadSize = findViewById(R.id.editTextPayloadSize)
        switchBombard = findViewById(R.id.switchBombard)
        spinnerAttackType = findViewById(R.id.spinnerAttackType)
        buttonStartStop = findViewById(R.id.buttonStartStop)
        logAttack = findViewById(R.id.logTextView)
        switchLog = findViewById(R.id.switchLogView)

        // Set text views
        viewDeviceName.text = "Device Name: $deviceName"
        viewDeviceAddress.text = "Address: $address"
        viewThreads.setText("$threads")
        logAttack.justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
        Logger.appendLog(logAttack, "Bluetooth Jammer Framework Version: $FrameworkVersion")

        // Payload pattern selector
        val patternAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            PayloadPattern.values().map { it.displayName }
        )
        spinnerPayloadPattern.adapter = patternAdapter

        // Attack type selector
        val attackTypes = AttackType.values()
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            attackTypes.map { it.displayName }
        )
        spinnerAttackType.adapter = typeAdapter
        spinnerAttackType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val type = attackTypes[position]
                if (!AttackManager.isAttacking) {
                    logAttack.append("\n> ${type.displayName}: ${type.description}")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set button listener
        buttonStartStop.setOnClickListener {
            if (AttackManager.isAttacking) stopAttack() else startAttack()
        }

        // Threading Input listener
        viewThreads.doAfterTextChanged { str ->
            if (str != null && str.toString().isNotEmpty() && str.isDigitsOnly()) {
                threads = str.toString().toInt()
            }
        }

        // Delay input (ms entre ráfagas; 0 = máxima velocidad)
        viewDelay.doAfterTextChanged { str ->
            if (str != null && str.toString().isNotEmpty() && str.isDigitsOnly()) {
                delayMs = str.toString().toInt()
            }
        }

        // TX / Sleep duty cycle (PortaPack-style burst/pause)
        viewTxSeconds.doAfterTextChanged { str ->
            if (str != null && str.toString().isNotEmpty() && str.isDigitsOnly()) {
                txSeconds = str.toString().toInt()
            }
        }

        viewSleepSeconds.doAfterTextChanged { str ->
            if (str != null && str.toString().isNotEmpty() && str.isDigitsOnly()) {
                sleepSeconds = str.toString().toInt()
            }
        }

        // Payload size (bytes; 0 = automático)
        viewPayloadSize.doAfterTextChanged { str ->
            if (str != null && str.toString().isNotEmpty() && str.isDigitsOnly()) {
                payloadSize = str.toString().toInt()
            }
        }

        // Bombard mode (conectar → ráfaga → cerrar, en ciclo rápido)
        switchBombard.setOnCheckedChangeListener { _, checked ->
            bombard = checked
            if (checked) {
                logAttack.append("\n> Modo bombardeo: ciclos rápidos conectar/enviar/cerrar")
            }
        }

        // Logging Switch listener
        switchLog.setOnCheckedChangeListener { _, isChecked ->
            loggingStatus = isChecked
            Toast.makeText(
                this@AttackActivity,
                if (isChecked) "Logging habilitado" else "Logging deshabilitado",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAttack() {
        if (AttackManager.isAttacking) return
        buttonStartStop.text = "Stop"
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        val selectedType = AttackType.values()[spinnerAttackType.selectedItemPosition]
        val params = AttackParams(
            threads = threads,
            rateDelayMs = delayMs,
            txSeconds = txSeconds,
            sleepSeconds = sleepSeconds,
            payloadPattern = PayloadPattern.values()[spinnerPayloadPattern.selectedItemPosition],
            bombard = bombard,
            payloadSize = payloadSize
        )

        startTimeMs = System.currentTimeMillis()
        connEvents = 0
        dataEvents = 0
        retryEvents = 0

        startedAttacks.clear()
        targets.forEach { (name, addr) ->
            val attack = selectedType.create(addr, params)
            startedAttacks.add(attack)
            AttackManager.track(addr, attack)
            attack.start(this) { message ->
                runOnUiThread {
                    if (AttackManager.isAttacking && loggingStatus) {
                        when {
                            message.contains("[CONN]") -> connEvents++
                            message.contains("[DATA]") -> dataEvents++
                            message.contains("[RETRY]") -> retryEvents++
                        }
                        Logger.appendLog(logAttack, "[$name] $message")
                    }
                }
            }
        }
        Logger.appendLog(
            logAttack,
            "Ataque iniciado: ${selectedType.displayName} sobre ${targets.size} objetivo(s) | " +
                "Intensidad: $threads | Delay: ${delayMs}ms | TX: ${txSeconds}s | Sleep: ${sleepSeconds}s | " +
                "Patrón: ${params.payloadPattern.displayName} | Payload: ${payloadSize}B | Bombardeo: $bombard"
        )
        Toast.makeText(
            this,
            "Usa esto SOLO con dispositivos de tu propiedad. Stop para detener.",
            Toast.LENGTH_LONG
        ).show()
    }

    @SuppressLint("MissingPermission")
    private fun stopAttack() {
        buttonStartStop.text = "Start"
        val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000
        Logger.appendLog(
            logAttack,
            "[RESUMEN] ${elapsed}s · CONN $connEvents · DATA $dataEvents · RETRY $retryEvents · objetivos ${targets.size}"
        )
        Logger.appendLog(logAttack, "Ataque detenido (${AttackManager.activeTargets().size} objetivo(s) activo(s)).")
        AttackManager.stopAll()
        startedAttacks.clear()
        BluetoothAdapter.getDefaultAdapter().startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (AttackManager.isAttacking) stopAttack()
    }

    override fun onPause() {
        super.onPause()
        if (AttackManager.isAttacking) stopAttack()
    }
}
