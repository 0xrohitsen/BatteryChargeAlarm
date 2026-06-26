package com.chargealarm.app.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chargealarm.app.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BatteryInfo(
    val percentage: Int = 0,
    val isCharging: Boolean = false,
    val temperature: Float = 0f, // in Celsius
    val voltage: Float = 0f, // in Volts
    val health: String = "Unknown"
)

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    
    val masterAlarmFlow = prefs.masterAlarmFlow
    val hasSeenWelcomeFlow = prefs.hasSeenWelcomeFlow
    val selectedLimitsFlow = prefs.selectedLimitsFlow
    val customAudioUriFlow = prefs.customAudioUriFlow

    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
                val volt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000f
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val percentage = if (level != -1 && scale != -1) (level * 100) / scale else 0
                
                val healthStr = when(healthInt) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                    BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                    else -> "Unknown"
                }

                _batteryInfo.value = BatteryInfo(
                    percentage = percentage,
                    isCharging = isCharging,
                    temperature = temp,
                    voltage = volt,
                    health = healthStr
                )
            }
        }
    }

    init {
        application.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(batteryReceiver)
    }

    fun setMasterAlarm(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setMasterAlarm(enabled)
        }
    }

    fun setHasSeenWelcome(seen: Boolean) {
        viewModelScope.launch {
            prefs.setHasSeenWelcome(seen)
        }
    }

    fun toggleLimit(limit: Int) {
        viewModelScope.launch {
            prefs.toggleLimit(limit)
        }
    }

    fun setCustomAudioUri(uri: String?) {
        viewModelScope.launch {
            prefs.setCustomAudioUri(uri)
        }
    }
}
