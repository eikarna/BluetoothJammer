package com.eikarna.bluetoothjammer

import api.OuiVendor
import api.ProfileNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceMetadataTest {

    @Test
    fun appleOui_isDetected() {
        assertEquals("Apple", OuiVendor.vendorFor("00:1B:63:12:34:56"))
    }

    @Test
    fun mediatekOui_isDetected() {
        assertEquals("MediaTek", OuiVendor.vendorFor("00:0e:8f:ab:cd:ef"))
    }

    @Test
    fun lowercaseAndDashes_areNormalized() {
        assertEquals("Samsung", OuiVendor.vendorFor("2c-fd-a1-00-00-01"))
    }

    @Test
    fun unknownOui_returnsNull() {
        assertNull(OuiVendor.vendorFor("AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun a2dpProfileName_isMapped() {
        assertEquals(
            "A2DP Source",
            ProfileNames.profileName("0000110A-0000-1000-8000-00805F9B34FB")
        )
    }

    @Test
    fun unknownProfile_returnsNull() {
        assertNull(ProfileNames.profileName("00000000-0000-1000-8000-00805F9B34FB"))
    }

    @Test
    fun shortUuid_showsLastHex() {
        assertEquals("34FB", ProfileNames.shortUuid("0000110A-0000-1000-8000-00805F9B34FB"))
    }
}
