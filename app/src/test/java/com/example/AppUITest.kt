package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSearchUIAndLogCreationSimulated() {
        composeTestRule.setContent {
            MainAppContent()
        }

        // Verify the title is displayed
        composeTestRule.onNodeWithText("VVF Smart Manager").assertExists()
        
        // Enter a search query in the AI Co-Pilot Assistant
        composeTestRule.onNodeWithTag("search_input").performTextInput("find my holiday photos")
        
        // Click the send button
        composeTestRule.onNodeWithTag("search_button").performClick()
        
        // Verify the Co-Pilot panel is visible
        composeTestRule.onNodeWithText("VVF AI Smart Co-Pilot").assertExists()
    }
}
