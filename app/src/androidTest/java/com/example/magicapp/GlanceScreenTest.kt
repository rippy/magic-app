package com.example.magicapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.magicapp.ui.GlanceScreen
import com.example.magicapp.ui.theme.MagicAppTheme
import org.junit.Rule
import org.junit.Test

class GlanceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun glanceScreen_showsHeroContent() {
        val viewModel = AppViewModel()
        composeTestRule.setContent {
            MagicAppTheme {
                GlanceScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("Hero Content").assertIsDisplayed()
    }

    @Test
    fun glanceScreen_showsWifiTile() {
        val viewModel = AppViewModel()
        composeTestRule.setContent {
            MagicAppTheme {
                GlanceScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText(Feature.WIFI_SCANNER.label).assertIsDisplayed()
    }

    @Test
    fun glanceScreen_showsBtTile() {
        val viewModel = AppViewModel()
        composeTestRule.setContent {
            MagicAppTheme {
                GlanceScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText(Feature.BT_SCANNER.label).assertIsDisplayed()
    }

    @Test
    fun glanceScreen_showsAllQuickTiles() {
        val viewModel = AppViewModel()
        composeTestRule.setContent {
            MagicAppTheme {
                GlanceScreen(viewModel = viewModel)
            }
        }
        Feature.glanceTiles.forEach { feature ->
            composeTestRule.onNodeWithText(feature.label).assertIsDisplayed()
        }
    }
}
