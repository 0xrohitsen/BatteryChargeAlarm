package com.chargealarm.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.chargealarm.app.data.PreferencesRepository

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            val prefs = PreferencesRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                val masterAlarm = prefs.masterAlarmFlow.first()
                if (masterAlarm) {
                    BatteryMonitorService.startService(context)
                }
            }
        } else if (intent.action == Intent.ACTION_POWER_DISCONNECTED) {
            BatteryMonitorService.stopService(context)
        }
    }
}
