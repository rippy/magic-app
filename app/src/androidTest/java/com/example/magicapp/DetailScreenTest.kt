package com.example.magicapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.magicapp.ui.DetailScreen
import com.example.magicapp.ui.theme.MagicAppTheme
import org.junit.Rule
import org.junit.Test

class DetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun detailScreen_showsFirstFeatureByDefault() {
        val viewModel = AppViewModel()
        composeTestRule.setContent {
            MagicAppTheme {
                DetailScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText(Feature.entries.first().label).assertIsDisplayed()
    }

    @Test
    fun detailScreen_showsAllSidebarIcons() {
        val viewModel = AppViewModel()
        composeTestRule.setContent {
            MagicAppTheme {
                DetailScreen(viewModel = viewModel)
            }
        }
        Feature.entries.forEach { feature ->
            composeTestRule.onNodeWithContentDescription(feature.label).assertIsDisplayed()
        }
    }

    @Test
    fun detailScreen_selectingFeatureUpdatesPanel() {
        val viewModel = AppViewModel()
        composeTestRule.setContent {
            MagicAppTheme {
                DetailScreen(viewModel = viewModel)
            }
        }
        val targetFeature = Feature.COMPASS
        composeTestRule.onNodeWithContentDescription(targetFeature.label).performClick()
        composeTestRule.onNodeWithText(targetFeature.label).assertIsDisplayed()
    }
}
