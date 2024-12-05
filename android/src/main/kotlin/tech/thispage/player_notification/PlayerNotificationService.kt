package tech.thispage.player_notification

import android.app.Service
import android.content.Intent
import android.os.Build

/**
 * PlayerNotificationService 是一个前台服务，用于管理播放器通知。
 */
class PlayerNotificationService : BaseForegroundService() {
    companion object {
        const val ACTION_START = "START_SERVICE"
        const val ACTION_STOP = "STOP_SERVICE"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                PlayerNotificationManager.currentNotification?.let { notification ->
                    startForeground(PlayerNotificationManager.NOTIFICATION_ID, notification)
                }
            }
            ACTION_STOP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}