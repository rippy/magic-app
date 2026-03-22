package com.example.magicapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val ROUTE_GLANCE = "glance"
private const val ROUTE_DETAIL = "detail"

@Composable
fun AppNavHost(
    viewModel: AppViewModel,
    navController: NavHostController = rememberNavController()
) {
    val mode by viewModel.mode.collectAsState()

    // Drive navigation from state — never navigate inside composition body
    LaunchedEffect(mode) {
        when (mode) {
            AppMode.GLANCE -> navController.navigate(ROUTE_GLANCE) {
                popUpTo(ROUTE_GLANCE) { inclusive = true }
                launchSingleTop = true
            }
            AppMode.DETAIL -> navController.navigate(ROUTE_DETAIL) {
                popUpTo(ROUTE_GLANCE) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_GLANCE) {
        composable(ROUTE_GLANCE) {
            GlanceScreenPlaceholder(viewModel = viewModel)
        }
        composable(ROUTE_DETAIL) {
            DetailScreenPlaceholder(viewModel = viewModel)
        }
    }
}

@Composable
private fun GlanceScreenPlaceholder(viewModel: AppViewModel) {
    // Replaced in Task 9
}

@Composable
private fun DetailScreenPlaceholder(viewModel: AppViewModel) {
    // Replaced in Task 10
}
