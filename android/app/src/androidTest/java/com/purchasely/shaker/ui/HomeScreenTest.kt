package com.purchasely.shaker.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scaffold for Compose UI tests.
 *
 * A full HomeScreen test would require Koin test modules and mocked
 * repositories. This file validates that the androidTest build pipeline
 * compiles and that the Compose test infrastructure is wired correctly.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun composeTestInfrastructure_works() {
        composeTestRule.setContent {
            Text("Hello Shaker")
        }
        composeTestRule.onNodeWithText("Hello Shaker").assertIsDisplayed()
    }
}
