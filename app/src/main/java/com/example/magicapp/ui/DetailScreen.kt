package com.example.magicapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.magicapp.AppViewModel
import com.example.magicapp.DiagnosticViewModel
import com.example.magicapp.Feature

@Composable
fun DetailScreen(
    viewModel: AppViewModel,
    diagnosticViewModel: DiagnosticViewModel,
    modifier: Modifier = Modifier
) {
    var selectedFeature by remember { mutableStateOf(Feature.entries.first()) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sidebar — 56dp wide
        Sidebar(
            selectedFeature = selectedFeature,
            onFeatureSelected = { selectedFeature = it },
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
        )

        // Feature panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            FeaturePlaceholder(
                feature = selectedFeature,
                modifier = Modifier.fillMaxSize()
            )

            // Controls overlay — top-right corner
            ControlsOverlay(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun Sidebar(
    selectedFeature: Feature,
    onFeatureSelected: (Feature) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Feature.entries.forEach { feature ->
            IconButton(
                onClick = { onFeatureSelected(feature) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_compass),
                    contentDescription = feature.label,
                    tint = if (feature == selectedFeature)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun FeaturePlaceholder(
    feature: Feature,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = feature.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
