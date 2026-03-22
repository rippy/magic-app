package com.example.magicapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.magicapp.ui.theme.MagicAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private val diagnosticViewModel: DiagnosticViewModel by viewModels()

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
    }
}
