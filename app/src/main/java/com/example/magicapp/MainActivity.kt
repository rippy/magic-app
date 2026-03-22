package com.example.magicapp

import android.Manifest
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.magicapp.ui.theme.MagicAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private val diagnosticViewModel: DiagnosticViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            diagnosticViewModel.gps.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MagicAppTheme {
                AppNavHost(
                    viewModel = viewModel,
                    diagnosticViewModel = diagnosticViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let { diagnosticViewModel.carButtons.onKeyEvent(it) }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let { diagnosticViewModel.carButtons.onKeyEvent(it) }
        return super.onKeyUp(keyCode, event)
    }
}
