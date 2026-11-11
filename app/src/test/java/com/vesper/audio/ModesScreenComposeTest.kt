package com.vesper.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import com.vesper.audio.ui.SoundProfile
import com.vesper.audio.ui.screens.ModesScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ModesScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testModesScreenScrollAndSelection() {
        var selectedProfile by mutableStateOf(SoundProfile.BALANCED)
        var navigatedToEq = false
        var navigatedBack = false

        composeTestRule.setContent {
            ModesScreen(
                activeProfile = selectedProfile,
                onProfileSelected = { selectedProfile = it },
                onNavigateToEqualizer = { navigatedToEq = true },
                onBackClick = { navigatedBack = true }
            )
        }

        composeTestRule.waitForIdle()

        // Verify primary scrollable profile collection and bottom switch
        composeTestRule.onNodeWithTag("modes_profile_list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("modes_bottom_navigation").assertIsDisplayed()
        composeTestRule.onNodeWithTag("modes_profile_balanced").assertIsDisplayed()

        // Scroll to and select Bass profile
        composeTestRule.onNodeWithTag("modes_profile_list").performScrollToNode(hasTestTag("modes_profile_bass"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("modes_profile_bass").performClick()
        composeTestRule.waitForIdle()
        assertEquals(SoundProfile.BASS_BOOST, selectedProfile)

        // Scroll down to the final custom profile
        composeTestRule.onNodeWithTag("modes_profile_list").performScrollToNode(hasTestTag("modes_profile_custom"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("modes_profile_custom").assertIsDisplayed()

        // Select Custom profile
        composeTestRule.onNodeWithTag("modes_profile_custom").performClick()
        composeTestRule.waitForIdle()
        assertEquals(SoundProfile.CUSTOM, selectedProfile)

        // Test bottom switch to Equalizer
        composeTestRule.onNodeWithTag("modes_switch_to_eq").performClick()
        composeTestRule.waitForIdle()
        assertTrue(navigatedToEq)

        // Test Back Button
        composeTestRule.onNodeWithTag("modes_back_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(navigatedBack)
    }
}
