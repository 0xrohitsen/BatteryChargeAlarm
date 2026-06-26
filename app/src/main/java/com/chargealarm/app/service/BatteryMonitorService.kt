package com.chargealarm.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chargealarm.app.R
import com.chargealarm.app.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BatteryMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var prefs: PreferencesRepository
    private var mediaPlayer: MediaPlayer? = null
    
    // To prevent playing the same alarm multiple times for the same percentage
    private var lastAnnouncedPercentage: Int = -1

    private var fullChargeReminderJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                if (!isCharging) {
                    fullChargeReminderJob?.cancel()
                    fullChargeReminderJob = null
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    stopSelf()
                    return
                }

                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    val batteryPct = (level * 100) / scale.toFloat()
                    val percentage = batteryPct.toInt()
                    
                    serviceScope.launch {
                        val limits = prefs.selectedLimitsFlow.first()
                        
                        if (percentage == 100 && limits.contains(100)) {
                            if (fullChargeReminderJob == null || fullChargeReminderJob?.isActive != true) {
                                fullChargeReminderJob = launch {
                                    lastAnnouncedPercentage = 100
                                    
                                    // Play 4 times continuously
                                    repeat(4) {
                                        if (isActive) {
                                            playAlarmSync(100)
                                            delay(500)
                                        }
                                    }
                                    
                                    // Then repeat every 5 minutes until charger is removed
                                    while (isActive) {
                                        delay(5 * 60 * 1000)
                                        if (isActive) {
                                            playAlarmSync(100)
                                        }
                                    }
                                }
                            }
                        } else {
                            fullChargeReminderJob?.cancel()
                            fullChargeReminderJob = null
                            
                            if (limits.contains(percentage) && lastAnnouncedPercentage != percentage) {
                                lastAnnouncedPercentage = percentage
                                playAlarmSync(percentage)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesRepository(this)
        createNotificationChannel()
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
        
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "battery_monitor",
                "Battery Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs while charging to monitor battery level"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "battery_monitor")
            .setContentTitle("Battery Charge Alarm")
            .setContentText("Monitoring battery level...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private suspend fun playAlarmSync(percentage: Int) {
        val customUri = prefs.customAudioUriFlow.first()
        
        suspendCancellableCoroutine<Unit> { cont ->
            mediaPlayer?.release()
            
            try {
                if (customUri != null) {
                    mediaPlayer = MediaPlayer.create(this@BatteryMonitorService, Uri.parse(customUri))
                } else {
                    val resId = when (percentage) {
                        20 -> R.raw.charge_20
                        30 -> R.raw.charge_30
                        40 -> R.raw.charge_40
                        50 -> R.raw.charge_50
                        60 -> R.raw.charge_60
                        70 -> R.raw.charge_70
                        80 -> R.raw.charge_80
                        90 -> R.raw.charge_90
                        100 -> R.raw.charge_100
                        else -> 0
                    }
                    if (resId != 0) {
                        mediaPlayer = MediaPlayer.create(this@BatteryMonitorService, resId)
                    }
                }
                
                if (mediaPlayer == null) {
                    if (cont.isActive) cont.resume(Unit)
                    return@suspendCancellableCoroutine
                }

                mediaPlayer?.setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    if (cont.isActive) cont.resume(Unit)
                }
                mediaPlayer?.setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    if (cont.isActive) cont.resume(Unit)
                    true
                }
                mediaPlayer?.start()
                
            } catch (e: Exception) {
                e.printStackTrace()
                if (cont.isActive) cont.resume(Unit)
            }
            
            cont.invokeOnCancellation {
                try {
                    mediaPlayer?.stop()
                } catch (e: Exception) {
                    // Ignore
                }
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        mediaPlayer?.release()
        mediaPlayer = null
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
