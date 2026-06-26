package com.chargealarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chargealarm.app.viewmodel.BatteryViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: BatteryViewModel,
    onNavigateToWelcome: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val hasSeenWelcome by viewModel.hasSeenWelcomeFlow.collectAsStateWithLifecycle(initialValue = false)
    
    LaunchedEffect(hasSeenWelcome) {
        delay(1500)
        if (hasSeenWelcome) {
            onNavigateToHome()
        } else {
            onNavigateToWelcome()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Battery Charge Alarm",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}
