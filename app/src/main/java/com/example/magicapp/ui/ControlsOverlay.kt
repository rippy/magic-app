package com.example.magicapp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.magicapp.AppViewModel

@Composable
fun ControlsOverlay(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val isMicMuted by viewModel.isMicMuted.collectAsState()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mic mute toggle
        IconButton(
            onClick = { viewModel.toggleMic() },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isMicMuted)
                        android.R.drawable.ic_btn_speak_now
                    else
                        android.R.drawable.ic_btn_speak_now
                ),
                contentDescription = if (isMicMuted) "Unmute mic" else "Mute mic",
                tint = if (isMicMuted) Color(0xFFFF6B6B) else Color(0xFF81C784),
                modifier = Modifier.size(24.dp)
            )
        }

        // Mode toggle (Glance <-> Detail)
        IconButton(
            onClick = { viewModel.toggleMode() },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_agenda),
                contentDescription = "Toggle mode",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        // Settings (placeholder)
        IconButton(
            onClick = { /* TODO: open settings */ },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_preferences),
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
