package com.eikarna.bluetoothjammer

import android.content.Context
import api.AttackManager
import api.BluetoothAttack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttackManagerTest {

    @After
    fun tearDown() {
        AttackManager.stopAll()
    }

    @Test
    fun track_registersTarget() {
        AttackManager.track("AA:BB:CC:DD:EE:FF", FakeAttack())
        assertTrue(AttackManager.isAttacking)
        assertEquals(setOf("aa:bb:cc:dd:ee:ff"), AttackManager.activeTargets())
    }

    @Test
    fun track_multipleTargets() {
        AttackManager.track("00:11:22:33:44:55", FakeAttack())
        AttackManager.track("66:77:88:99:AA:BB", FakeAttack())
        assertEquals(2, AttackManager.activeTargets().size)
    }

    @Test
    fun stopAll_clearsAndStopsAttacks() {
        val fake = FakeAttack()
        AttackManager.track("00:11:22:33:44:55", fake)
        AttackManager.stopAll()
        assertFalse(AttackManager.isAttacking)
        assertTrue(AttackManager.activeTargets().isEmpty())
        assertTrue(fake.stopped)
    }

    @Test
    fun stopAll_whenEmpty_isSafe() {
        AttackManager.stopAll()
        assertFalse(AttackManager.isAttacking)
        assertTrue(AttackManager.activeTargets().isEmpty())
    }
}

private class FakeAttack : BluetoothAttack {
    var stopped = false
    override val displayName = "Fake"
    override val description = "Fake"
    override fun start(context: Context, onLog: (String) -> Unit) {}
    override fun stop() {
        stopped = true
    }

    override fun isRunning() = !stopped
}
