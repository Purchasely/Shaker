package com.purchasely.shaker.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingRepositoryTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    private var storedBoolean: Boolean = false

    @Before
    fun setUp() {
        storedBoolean = false
        editor = mockk(relaxed = true) {
            every { putBoolean(any(), any()) } answers {
                storedBoolean = secondArg()
                this@mockk
            }
        }
        prefs = mockk {
            every { getBoolean(any(), any()) } answers { storedBoolean }
            every { edit() } returns editor
        }
        context = mockk {
            every { getSharedPreferences(any(), any()) } returns prefs
        }
    }

    @Test
    fun `initial state is false`() {
        val repo = OnboardingRepository(context)
        assertFalse(repo.isOnboardingCompleted)
    }

    @Test
    fun `setting to true persists`() {
        val repo = OnboardingRepository(context)
        repo.isOnboardingCompleted = true
        assertTrue(repo.isOnboardingCompleted)
        verify { editor.putBoolean("onboarding_completed", true) }
    }

    @Test
    fun `setting to false persists`() {
        storedBoolean = true
        val repo = OnboardingRepository(context)
        repo.isOnboardingCompleted = false
        assertFalse(repo.isOnboardingCompleted)
        verify { editor.putBoolean("onboarding_completed", false) }
    }

    @Test
    fun `reads stored value on creation`() {
        storedBoolean = true
        val repo = OnboardingRepository(context)
        assertTrue(repo.isOnboardingCompleted)
    }
}
