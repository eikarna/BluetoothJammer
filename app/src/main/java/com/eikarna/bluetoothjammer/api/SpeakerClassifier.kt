package api

/**
 * Classifies whether a nearby Bluetooth device is likely an audio speaker.
 *
 * Signals, strongest first:
 *  1. Classic Bluetooth device class (major = Audio/Video + speaker minor).
 *  2. BLE GAP appearance field (Generic Speaker = 0x0017).
 *  3. Device name heuristics (fallback when the device publishes no metadata).
 *
 * Note: constants below mirror android.bluetooth.BluetoothClass.Device.* values
 * as plain literals so this class stays unit-testable on the JVM.
 */
object SpeakerClassifier {

    enum class Confidence { HIGH, MEDIUM, LOW }

    data class Result(val isSpeaker: Boolean, val confidence: Confidence, val reason: String?)

    // --- Classic device class (raw layout: bits 8-12 major, bits 2-7 minor) ---
    // BluetoothClass.Device.Major.AUDIO_VIDEO = 0x400; shifted value used by majorDeviceClass is 0x04
    private const val MAJOR_AUDIO_VIDEO = 0x04

    // Minor values of Audio/Video devices (BluetoothClass.Device.AUDIO_VIDEO_* >> 2)
    private const val MINOR_LOUDSPEAKER = 0x05          // AUDIO_VIDEO_LOUDSPEAKER (0x414)
    private const val MINOR_HIFI = 0x0A                 // AUDIO_VIDEO_HIFI_AUDIO (0x428)
    private const val MINOR_DISPLAY_AND_SPEAKER = 0x10  // AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER (0x440)
    private const val MINOR_PORTABLE_AUDIO = 0x07       // AUDIO_VIDEO_PORTABLE_AUDIO (0x41C)
    private const val MINOR_CAR_AUDIO = 0x08            // AUDIO_VIDEO_CAR_AUDIO (0x420)

    // --- BLE GAP appearance values (Bluetooth SIG Assigned Numbers) ---
    private const val APPEARANCE_GENERIC_SPEAKER = 0x0017

    // --- Name heuristics (fallback; lowercase substring match) ---
    private val SPEAKER_NAME_KEYWORDS = listOf(
        "speaker", "altavoz", "soundbar", "boombox", "boom box", "homepod",
        "sonos", "marshall", "harman", "ultimate ears", "ue boom", "ue megaboom",
        "echo dot", "echo show", "echo pop", "jbl", "alexa", "bose",
        "bluetooth speaker", "bt speaker", "mini speaker",
    )

    fun classify(name: String?, deviceClass: Int?, bleAppearance: Int?): Result {
        // 1. Classic device class (strongest)
        if (deviceClass != null) {
            val major = (deviceClass shr 8) and 0x1F
            val minor = (deviceClass and 0xFF) shr 2
            if (major == MAJOR_AUDIO_VIDEO) {
                when (minor) {
                    MINOR_LOUDSPEAKER, MINOR_HIFI, MINOR_DISPLAY_AND_SPEAKER ->
                        return Result(true, Confidence.HIGH, "Clase BT")
                    MINOR_PORTABLE_AUDIO, MINOR_CAR_AUDIO ->
                        return Result(true, Confidence.MEDIUM, "Clase BT")
                }
            }
        }

        // 2. BLE appearance
        if (bleAppearance == APPEARANCE_GENERIC_SPEAKER) {
            return Result(true, Confidence.HIGH, "BLE")
        }

        // 3. Name heuristics
        val lower = name?.lowercase() ?: ""
        if (lower.isNotEmpty()) {
            val hit = SPEAKER_NAME_KEYWORDS.firstOrNull { lower.contains(it) }
            if (hit != null) return Result(true, Confidence.MEDIUM, "Nombre: $hit")
        }

        return Result(false, Confidence.LOW, null)
    }

    /**
     * Human-readable label for a classic device class, or null when unknown.
     */
    fun describeDeviceClass(deviceClass: Int?): String? {
        if (deviceClass == null) return null
        val major = (deviceClass shr 8) and 0x1F
        val minor = (deviceClass and 0xFF) shr 2
        return when (major) {
            0x01 -> "Ordenador"
            0x02 -> "Teléfono"
            0x04 -> when (minor) {
                MINOR_LOUDSPEAKER -> "Altavoz"
                MINOR_HIFI -> "Hi-Fi"
                MINOR_DISPLAY_AND_SPEAKER -> "Pantalla con altavoz"
                0x06 -> "Auriculares"            // AUDIO_VIDEO_HEADPHONES
                0x01 -> "Auriculares con mic"    // AUDIO_VIDEO_WEARABLE_HEADSET
                0x02 -> "Manos libres"           // AUDIO_VIDEO_HANDSFREE
                MINOR_PORTABLE_AUDIO -> "Audio portátil"
                MINOR_CAR_AUDIO -> "Audio de coche"
                else -> "Audio/Video"
            }
            0x05 -> "Periférico"
            0x07 -> "Wearable"
            0x09 -> "Salud"
            else -> null
        }
    }
}
