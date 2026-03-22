package com.example.magicapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppViewModelTest {

    private lateinit var viewModel: AppViewModel

    @Before
    fun setup() {
        viewModel = AppViewModel()
    }

    @Test
    fun `initial mode is GLANCE`() {
        assertEquals(AppMode.GLANCE, viewModel.mode.value)
    }

    @Test
    fun `initial mic state is not muted`() {
        assertFalse(viewModel.isMicMuted.value)
    }

    @Test
    fun `toggleMode switches from GLANCE to DETAIL`() {
        viewModel.toggleMode()
        assertEquals(AppMode.DETAIL, viewModel.mode.value)
    }

    @Test
    fun `toggleMode switches from DETAIL back to GLANCE`() {
        viewModel.toggleMode()
        viewModel.toggleMode()
        assertEquals(AppMode.GLANCE, viewModel.mode.value)
    }

    @Test
    fun `toggleMic mutes when unmuted`() {
        viewModel.toggleMic()
        assertTrue(viewModel.isMicMuted.value)
    }

    @Test
    fun `toggleMic unmutes when muted`() {
        viewModel.toggleMic()
        viewModel.toggleMic()
        assertFalse(viewModel.isMicMuted.value)
    }
}
