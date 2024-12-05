package tech.thispage.player_notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class PlayerNotificationPlugin: FlutterPlugin, MethodCallHandler {
    // 定义用于与 Flutter 通信的通道
    private lateinit var channel: MethodChannel
    // 上下文对象，用于获取系统服务
    private lateinit var context: Context
    // 通知管理器，用于管理通知
    private lateinit var notificationManager: NotificationManager
    // 播放器通知管理器，用于管理播放器通知
    private lateinit var playerNotificationManager: PlayerNotificationManager

    companion object {
        // 通知通道 ID
        private const val CHANNEL_ID = "player_notification_channel"
        // 通知 ID
        private const val NOTIFICATION_ID = 1
    }

    /**
     * 当插件附加到 Flutter 引擎时调用。
     * 初始化方法通道、通知管理器和播放器通知管理器。
     */
    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "tech.thispage.player_notification")
        context = binding.applicationContext
        
        // 初始化通知管理器
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // 初始化播放器通知管理器
        playerNotificationManager = PlayerNotificationManager(context, notificationManager, channel)
        
        // 设置方法调用处理器
        channel.setMethodCallHandler(this)
    }

    /**
     * 处理来自 Flutter 的方法调用。
     */
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "setSticky" -> {
                // 设置通知是否为粘性
                val sticky = call.argument<Boolean>("sticky") ?: true
                playerNotificationManager.setSticky(sticky)
                result.success(null)
            }
            "showNotification" -> {
                val title = call.argument<String>("title") ?: ""
                val artist = call.argument<String>("artist") ?: ""
                val imageUrl = call.argument<String>("imageUrl")
                val lyrics = call.argument<String>("lyrics") ?: ""
                val isPlaying = call.argument<Boolean>("isPlaying") ?: false
                val position = (call.argument<Number>("position")?.toLong()) ?: 0L
                val duration = (call.argument<Number>("duration")?.toLong()) ?: 0L

                playerNotificationManager.updateNotification(
                    title = title,
                    artist = artist,
                    imageUrl = imageUrl,
                    lyrics = lyrics,
                    isPlaying = isPlaying,
                    position = position,  // 移除 toLong()
                    duration = duration   // 移除 toLong()
                )
                result.success(null)
            }
            "hideNotification" -> {
                // 隐藏通知
                playerNotificationManager.hideNotification()
                result.success(null)
            }
            "updatePlayState" -> {
                // 更新播放状态
                val isPlaying = call.argument<Boolean>("isPlaying") ?: false
                playerNotificationManager.updatePlayState(isPlaying)
                result.success(null)
            }
            "updateLyrics" -> {
                // 更新歌词
                val lyrics = call.argument<String>("lyrics") ?: ""
                playerNotificationManager.updateLyrics(lyrics)
                result.success(null)
            }
            "updateProgress" -> {
                val position = (call.argument<Number>("position")?.toLong()) ?: 0L
                val duration = (call.argument<Number>("duration")?.toLong()) ?: 0L
                android.util.Log.d("PlayerNotification", "Method call updateProgress: position=$position, duration=$duration")
                playerNotificationManager.updateProgress(position, duration)
                result.success(null)
            }

            else -> result.notImplemented()
        }
    }

    /**
     * 当插件从 Flutter 引擎分离时调用。
     * 清理资源。
     */
    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        playerNotificationManager.dispose()
    }

    /**
     * 创建通知通道（仅在 Android O 及以上版本需要）。
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Player Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows player controls in notification"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                setBlockable(false)  // 设置为不可阻止
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                importance = NotificationManager.IMPORTANCE_HIGH
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}