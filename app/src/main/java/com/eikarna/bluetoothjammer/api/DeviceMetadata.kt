package api

/**
 * Local device metadata helpers (no network needed).
 *
 * The OUI table maps the first 3 bytes of a MAC address to a vendor name.
 * It is a PARTIAL, curated list of well-known IEEE OUI assignments — some
 * devices may be missing or mislabeled (many vendors use random addresses).
 */
object OuiVendor {

    private val TABLE: Map<String, String> = mapOf(
        // Apple
        "00:03:93" to "Apple", "00:0A:95" to "Apple", "00:1B:63" to "Apple", "00:1E:52" to "Apple",
        "00:23:DF" to "Apple", "00:25:00" to "Apple", "04:0C:CE" to "Apple", "04:26:65" to "Apple",
        "08:66:98" to "Apple", "0C:30:21" to "Apple", "0C:74:C2" to "Apple", "14:7D:DA" to "Apple",
        "1C:36:BB" to "Apple", "20:C9:D0" to "Apple", "28:37:37" to "Apple", "2C:BE:EB" to "Apple",
        "34:12:98" to "Apple", "34:36:3B" to "Apple", "3C:07:54" to "Apple", "3C:AB:8E" to "Apple",
        "40:36:5F" to "Apple", "44:00:10" to "Apple", "48:43:5E" to "Apple", "4C:57:CA" to "Apple",
        "5C:F5:DA" to "Apple", "60:F8:1D" to "Apple", "64:20:0C" to "Apple", "68:09:27" to "Apple",
        "6C:2E:85" to "Apple", "70:77:34" to "Apple", "74:E1:B6" to "Apple", "78:27:4B" to "Apple",
        "7C:11:BE" to "Apple", "80:E6:50" to "Apple", "84:38:35" to "Apple", "88:C6:63" to "Apple",
        "8C:7B:9D" to "Apple", "90:0C:C3" to "Apple", "90:B0:ED" to "Apple", "94:28:6E" to "Apple",
        "98:8D:46" to "Apple", "9C:20:7B" to "Apple", "A0:2C:36" to "Apple", "A4:83:E7" to "Apple",
        "A8:5C:2C" to "Apple", "AC:BC:32" to "Apple", "B0:E5:ED" to "Apple", "B4:8B:19" to "Apple",
        "B8:E8:56" to "Apple", "BC:52:B7" to "Apple", "C0:2B:B8" to "Apple", "C4:B3:01" to "Apple",
        "C8:3A:35" to "Apple", "CC:08:E0" to "Apple", "D0:23:DB" to "Apple", "D4:61:9D" to "Apple",
        "D8:30:62" to "Apple", "DC:2B:2A" to "Apple", "E0:AC:CB" to "Apple", "E4:CE:8F" to "Apple",
        "E8:07:BF" to "Apple", "EC:AD:8B" to "Apple", "F0:18:98" to "Apple", "F4:0F:24" to "Apple",
        "F8:1E:DF" to "Apple", "FC:A8:9A" to "Apple", "FC:E9:98" to "Apple",
        // Samsung
        "00:12:FB" to "Samsung", "00:15:99" to "Samsung", "00:16:6C" to "Samsung", "00:1B:6D" to "Samsung",
        "00:1E:1F" to "Samsung", "00:23:D4" to "Samsung", "00:25:9E" to "Samsung", "04:3F:72" to "Samsung",
        "08:00:46" to "Samsung", "08:00:28" to "Samsung", "0C:70:80" to "Samsung", "10:2A:B3" to "Samsung",
        "14:31:DB" to "Samsung", "18:5E:0F" to "Samsung", "1C:1B:0D" to "Samsung", "1C:71:86" to "Samsung",
        "20:02:AF" to "Samsung", "24:65:11" to "Samsung", "28:64:EF" to "Samsung", "2C:10:C1" to "Samsung",
        "2C:FD:A1" to "Samsung", "30:71:B2" to "Samsung", "34:12:4C" to "Samsung", "38:BA:F8" to "Samsung",
        "3C:A0:67" to "Samsung", "44:5C:E9" to "Samsung", "48:D8:55" to "Samsung", "4C:77:66" to "Samsung",
        "50:28:4A" to "Samsung", "54:EE:75" to "Samsung", "58:A0:23" to "Samsung", "5C:F9:38" to "Samsung",
        "60:A4:D0" to "Samsung", "64:CB:DB" to "Samsung", "68:17:D4" to "Samsung", "6C:AD:EF" to "Samsung",
        "70:3A:CB" to "Samsung", "74:C3:DF" to "Samsung", "78:DF:4C" to "Samsung", "7C:EC:79" to "Samsung",
        "80:87:14" to "Samsung", "84:16:F9" to "Samsung", "88:36:6C" to "Samsung", "8C:F6:C6" to "Samsung",
        "90:17:AC" to "Samsung", "94:B8:6D" to "Samsung", "98:E8:FA" to "Samsung", "9C:93:4E" to "Samsung",
        "A0:E9:DB" to "Samsung", "A4:9B:13" to "Samsung", "A8:6D:AA" to "Samsung", "AC:5A:14" to "Samsung",
        "B0:71:2F" to "Samsung", "B4:70:9B" to "Samsung", "B8:08:D7" to "Samsung", "BC:C6:DB" to "Samsung",
        "C0:7B:BC" to "Samsung", "C4:7D:4F" to "Samsung", "C8:50:E9" to "Samsung", "CC:29:F5" to "Samsung",
        "D0:47:04" to "Samsung", "D4:46:45" to "Samsung", "D8:96:95" to "Samsung", "DC:0B:1A" to "Samsung",
        "E0:8D:4D" to "Samsung", "E4:9A:79" to "Samsung", "E8:9D:87" to "Samsung", "EC:1C:05" to "Samsung",
        "F0:43:47" to "Samsung", "F4:CE:46" to "Samsung", "F8:31:3E" to "Samsung", "FC:9D:64" to "Samsung",
        // Qualcomm / Atheros
        "00:03:7F" to "Qualcomm", "00:0B:6C" to "Qualcomm", "28:CF:E9" to "Qualcomm",
        "9C:AD:97" to "Qualcomm", "AC:37:43" to "Qualcomm",
        // Broadcom / Cypress
        "00:10:18" to "Broadcom", "00:17:F2" to "Broadcom", "04:21:73" to "Broadcom", "10:AE:60" to "Broadcom",
        "18:1D:EA" to "Broadcom", "20:1E:88" to "Broadcom", "2C:AB:00" to "Broadcom", "40:CB:C0" to "Broadcom",
        "50:3E:AA" to "Broadcom", "58:6D:8F" to "Broadcom", "68:9B:7A" to "Broadcom", "6C:9B:02" to "Broadcom",
        "8C:DE:F9" to "Broadcom", "A0:CE:C8" to "Broadcom", "B0:5A:DA" to "Broadcom", "C8:16:BD" to "Broadcom",
        "D8:6C:63" to "Broadcom", "E0:94:67" to "Broadcom", "F8:3D:FF" to "Broadcom",
        // MediaTek
        "00:0E:8F" to "MediaTek", "04:AB:18" to "MediaTek", "10:7C:61" to "MediaTek", "14:F6:5A" to "MediaTek",
        "18:FE:34" to "MediaTek", "1C:3B:F0" to "MediaTek", "2C:21:72" to "MediaTek", "38:38:7C" to "MediaTek",
        "3C:0E:23" to "MediaTek", "3C:20:30" to "MediaTek", "48:37:7A" to "MediaTek", "54:08:D6" to "MediaTek",
        "58:52:52" to "MediaTek", "5C:94:EF" to "MediaTek", "64:09:80" to "MediaTek", "64:B4:73" to "MediaTek",
        "68:27:57" to "MediaTek", "6C:5F:1E" to "MediaTek", "70:5A:0F" to "MediaTek", "74:15:F5" to "MediaTek",
        "78:9E:D0" to "MediaTek", "80:4B:20" to "MediaTek", "84:37:D6" to "MediaTek", "88:96:4E" to "MediaTek",
        "8C:3C:4A" to "MediaTek", "94:60:2F" to "MediaTek", "9C:8E:99" to "MediaTek", "AC:84:C6" to "MediaTek",
        "B4:9B:5A" to "MediaTek", "BC:32:5F" to "MediaTek", "C0:7C:D1" to "MediaTek", "C8:51:95" to "MediaTek",
        "CC:14:A6" to "MediaTek", "D4:6A:6A" to "MediaTek", "DC:44:6D" to "MediaTek", "E0:E4:03" to "MediaTek",
        "E8:50:8B" to "MediaTek", "EC:5F:23" to "MediaTek", "F0:9E:63" to "MediaTek", "F8:A4:5F" to "MediaTek",
        // Intel
        "00:02:B3" to "Intel", "00:0C:E7" to "Intel", "00:1B:21" to "Intel", "00:21:6A" to "Intel",
        "00:23:02" to "Intel", "04:CE:14" to "Intel", "0C:8B:FD" to "Intel", "10:0B:A9" to "Intel",
        "14:91:82" to "Intel", "1C:65:9D" to "Intel", "20:1A:06" to "Intel", "24:77:03" to "Intel",
        "28:D2:44" to "Intel", "2C:27:D7" to "Intel", "34:23:87" to "Intel", "3C:46:D8" to "Intel",
        "44:85:00" to "Intel", "48:F8:B3" to "Intel", "4C:ED:DE" to "Intel", "50:3C:6F" to "Intel",
        "54:B2:03" to "Intel", "58:FB:84" to "Intel", "5C:E0:C5" to "Intel", "64:80:99" to "Intel",
        "68:17:29" to "Intel", "6C:29:95" to "Intel", "70:F1:A1" to "Intel", "74:2F:68" to "Intel",
        "78:31:C1" to "Intel", "7C:B0:3E" to "Intel", "80:32:53" to "Intel", "84:7B:61" to "Intel",
        "88:C9:B3" to "Intel", "8C:FD:18" to "Intel", "90:9A:4A" to "Intel", "94:65:2D" to "Intel",
        "98:3B:8F" to "Intel", "9C:B6:54" to "Intel", "A0:88:69" to "Intel", "A4:1F:72" to "Intel",
        "A8:7C:01" to "Intel", "AC:7A:4D" to "Intel", "B0:48:7A" to "Intel", "B4:6D:83" to "Intel",
        "B8:CB:29" to "Intel", "BC:77:37" to "Intel", "C0:CB:38" to "Intel", "C4:71:FE" to "Intel",
        "C8:1B:B3" to "Intel", "CC:C1:E0" to "Intel", "D0:AB:D5" to "Intel", "D4:EE:07" to "Intel",
        "D8:BB:C1" to "Intel", "DC:FE:07" to "Intel", "E0:63:DA" to "Intel", "E4:02:9B" to "Intel",
        "E8:39:35" to "Intel", "EC:1A:59" to "Intel", "F0:7B:CB" to "Intel", "F4:6D:04" to "Intel",
        "F8:75:A4" to "Intel", "FC:F8:AE" to "Intel",
        // Otros fabricantes conocidos
        "00:1A:11" to "Google", "F4:F5:D8" to "Google",
        "00:04:9F" to "Huawei", "00:0C:E6" to "Huawei", "20:08:ED" to "Huawei", "38:BC:1A" to "Huawei",
        "44:23:7C" to "Xiaomi", "78:C0:94" to "Xiaomi", "9C:F5:04" to "Xiaomi",
        "00:1F:90" to "Sony",
        "00:1B:3D" to "LG",
    )

    /**
     * Returns the vendor for a MAC address like "AA:BB:CC:DD:EE:FF", or null when unknown.
     * Handles lowercase and dash/dot separators.
     */
    fun vendorFor(address: String): String? {
        val normalized = address.trim().uppercase().replace("-", ":").replace(".", ":")
        return if (normalized.length >= 8) TABLE[normalized.take(8)] else null
    }
}

/**
 * Maps Bluetooth UUIDs (classic SDP + BLE GATT) to human-readable profile names.
 */
object ProfileNames {

    private val PROFILES = mapOf(
        "00001101" to "SPP",
        "00001105" to "OPP",
        "0000110A" to "A2DP Source",
        "0000110B" to "A2DP Sink",
        "0000110C" to "AVRCP",
        "0000110E" to "AVRCP Controller",
        "0000110F" to "AVRCP Target",
        "0000111E" to "HFP",
        "0000111F" to "HFP AG",
        "00001124" to "HID",
        "0000112D" to "PAN",
        "00001115" to "PANU",
        "00001130" to "PBAP",
        "00001112" to "PBAP Server",
        "00001800" to "GAP (BLE)",
        "00001801" to "GATT (BLE)",
        "0000180A" to "Device Info (BLE)",
        "0000180D" to "Heart Rate (BLE)",
        "0000180F" to "Battery (BLE)",
    )

    /** Returns a readable profile name for a full UUID, or null when unknown. */
    fun profileName(uuid: String): String? {
        val key = uuid.trim().uppercase().replace("-", "").take(8)
        return PROFILES[key]
    }

    /** Short 4-hex display form of a UUID, e.g. "0A3B". */
    fun shortUuid(uuid: String): String {
        return uuid.trim().uppercase().replace("-", "").takeLast(4)
    }
}
