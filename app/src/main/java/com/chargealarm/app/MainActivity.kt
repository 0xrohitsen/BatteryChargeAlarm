package com.chargealarm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chargealarm.app.theme.BatteryChargeAlarmTheme
import com.chargealarm.app.ui.screens.HomeScreen
import com.chargealarm.app.ui.screens.SettingsScreen
import com.chargealarm.app.ui.screens.SplashScreen
import com.chargealarm.app.ui.screens.WelcomeScreen
import com.chargealarm.app.viewmodel.BatteryViewModel

class MainActivity : ComponentActivity() {
    private val batteryViewModel: BatteryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryChargeAlarmTheme {
                AppNavigation(viewModel = batteryViewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: BatteryViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                viewModel = viewModel,
                onNavigateToWelcome = {
                    navController.navigate("welcome") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("welcome") {
            WelcomeScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo("welcome") { inclusive = true }
                }
            })
        }
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
