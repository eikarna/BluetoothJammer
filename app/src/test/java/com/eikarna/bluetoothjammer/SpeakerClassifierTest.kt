package com.eikarna.bluetoothjammer

import api.SpeakerClassifier
import api.SpeakerClassifier.Confidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerClassifierTest {

    @Test
    fun classicLoudspeaker_isSpeakerHigh() {
        // deviceClass = 0x414 (BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER)
        val r = SpeakerClassifier.classify("Mi Altavoz", 0x414, null)
        assertTrue(r.isSpeaker)
        assertEquals(Confidence.HIGH, r.confidence)
    }

    @Test
    fun classicHifi_isSpeakerHigh() {
        // deviceClass = 0x428 (AUDIO_VIDEO_HIFI_AUDIO)
        val r = SpeakerClassifier.classify("Amplificador", 0x428, null)
        assertTrue(r.isSpeaker)
        assertEquals(Confidence.HIGH, r.confidence)
    }

    @Test
    fun classicHeadphones_notSpeaker() {
        // deviceClass = 0x418 (AUDIO_VIDEO_HEADPHONES)
        val r = SpeakerClassifier.classify("Sony WH-1000", 0x418, null)
        assertFalse(r.isSpeaker)
    }

    @Test
    fun classicPortableAudio_isSpeakerMedium() {
        // deviceClass = 0x41C (AUDIO_VIDEO_PORTABLE_AUDIO)
        val r = SpeakerClassifier.classify("Radio FM", 0x41C, null)
        assertTrue(r.isSpeaker)
        assertEquals(Confidence.MEDIUM, r.confidence)
    }

    @Test
    fun bleGenericSpeakerAppearance_isSpeakerHigh() {
        // BLE GAP appearance = 0x0017 (Generic Speaker)
        val r = SpeakerClassifier.classify("JBL-X", null, 0x0017)
        assertTrue(r.isSpeaker)
        assertEquals(Confidence.HIGH, r.confidence)
    }

    @Test
    fun nameKeywordFallback_speakerMedium() {
        val r = SpeakerClassifier.classify("JBL Flip 6", null, null)
        assertTrue(r.isSpeaker)
        assertEquals(Confidence.MEDIUM, r.confidence)
    }

    @Test
    fun unknownDevice_notSpeaker() {
        val r = SpeakerClassifier.classify("Reloj inteligente", null, null)
        assertFalse(r.isSpeaker)
    }

    @Test
    fun nullNameAndNoMetadata_notSpeaker() {
        val r = SpeakerClassifier.classify(null, null, null)
        assertFalse(r.isSpeaker)
    }
}
