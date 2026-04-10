package com.purchasely.shaker.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.purchasely.ext.PLYRunningMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunningModeRepositoryTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    private var storedString: String? = "full"

    @Before
    fun setUp() {
        storedString = "full"
        editor = mockk(relaxed = true) {
            every { putString(any(), any()) } answers {
                storedString = secondArg()
                this@mockk
            }
        }
        prefs = mockk {
            every { getString(any(), any()) } answers { storedString ?: secondArg() }
            every { edit() } returns editor
        }
        context = mockk {
            every { getSharedPreferences(any(), any()) } returns prefs
        }
    }

    @Test
    fun `default mode is Full`() {
        val repo = RunningModeRepository(context)
        assertEquals(PLYRunningMode.Full, repo.runningMode)
    }

    @Test
    fun `isObserverMode is false when Full`() {
        val repo = RunningModeRepository(context)
        assertFalse(repo.isObserverMode)
    }

    @Test
    fun `setting to PaywallObserver persists observer string`() {
        val repo = RunningModeRepository(context)
        repo.runningMode = PLYRunningMode.PaywallObserver
        verify { editor.putString("running_mode", "observer") }
    }

    @Test
    fun `reading PaywallObserver from storage`() {
        storedString = "observer"
        val repo = RunningModeRepository(context)
        assertEquals(PLYRunningMode.PaywallObserver, repo.runningMode)
        assertTrue(repo.isObserverMode)
    }

    @Test
    fun `setting to Full persists full string`() {
        storedString = "observer"
        val repo = RunningModeRepository(context)
        repo.runningMode = PLYRunningMode.Full
        verify { editor.putString("running_mode", "full") }
    }

    @Test
    fun `unknown stored value defaults to Full`() {
        storedString = "unknown"
        val repo = RunningModeRepository(context)
        assertEquals(PLYRunningMode.Full, repo.runningMode)
    }
}
