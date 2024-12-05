package tech.thispage.player_notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager

abstract class BaseForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {  
        super.onCreate()
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PlayerNotification::ServiceLock").apply {
                acquire(10L * 60L * 1000L)  // 10分钟的超时时间
            }
        }
    }

    override fun onDestroy() { 
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null  

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {  
        return START_STICKY
    }
}