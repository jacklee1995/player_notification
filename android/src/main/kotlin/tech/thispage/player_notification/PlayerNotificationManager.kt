package tech.thispage.player_notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.core.app.NotificationCompat
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import java.net.URL

class PlayerNotificationManager(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val channel: MethodChannel
) {
    // 用于管理协程的作业
    private var job: Job? = null
    // 定义协程作用域，使用主线程调度器
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    // 媒体会话，用于与媒体控制器交互
    private var mediaSession: MediaSessionCompat? = null
    // 当前显示的通知
    private var currentNotification: Notification? = null
    // 是否为粘性通知
    private var isSticky = true
    // 通知是否正在显示
    private var isNotificationShowing = false
    
    // 用来保存最后一次的图片URL
    private var lastImageUrl: String? = null

    private var position: Long = 0
    private var duration: Long = 0

    // 广播接收器，用于接收播放控制的广播
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 震动反馈
            val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }

            // 根据接收到的动作执行相应的操作
            when (intent?.action) {
                ACTION_PLAY -> {
                    channel.invokeMethod("onPlay", null)
                    currentNotification?.let { notification ->
                        val isPlaying = notification.extras.getBoolean("android.isPlaying", false)
                        updatePlayState(!isPlaying)
                    }
                }
                ACTION_PAUSE -> {
                    channel.invokeMethod("onPause", null)
                    currentNotification?.let { notification ->
                        val isPlaying = notification.extras.getBoolean("android.isPlaying", true)
                        updatePlayState(!isPlaying)
                    }
                }
                ACTION_NEXT -> {
                    // 先发送事件到 Flutter
                    channel.invokeMethod("onNext", null)
                    // 立即更新通知
                    currentNotification?.let { notification ->
                        val title = notification.extras.getString("android.title") ?: ""
                        val artist = notification.extras.getString("android.text") ?: ""
                        val lyrics = notification.extras.getString("android.subText") ?: ""
                        val isPlaying = notification.extras.getBoolean("android.isPlaying", false)
                        // 使用协程在主线程更新通知
                        scope.launch {
                            updateNotification(title, artist, null, lyrics, isPlaying)
                        }
                    }
                }
                ACTION_PREVIOUS -> {
                    // 先发送事件到 Flutter
                    channel.invokeMethod("onPrevious", null)
                    // 立即更新通知
                    currentNotification?.let { notification ->
                        val title = notification.extras.getString("android.title") ?: ""
                        val artist = notification.extras.getString("android.text") ?: ""
                        val lyrics = notification.extras.getString("android.subText") ?: ""
                        val isPlaying = notification.extras.getBoolean("android.isPlaying", false)
                        // 使用协程在主线程更新通知
                        scope.launch {
                            updateNotification(title, artist, null, lyrics, isPlaying)
                        }
                    }
                }
            }
        }
    }

    companion object {
        // 通知 ID
        const val NOTIFICATION_ID = 1
        // 通知通道 ID
        const val CHANNEL_ID = "player_notification_channel"

        // 播放控制动作常量
        private const val ACTION_PLAY = "tech.thispage.player_notification.PLAY"
        private const val ACTION_PAUSE = "tech.thispage.player_notification.PAUSE"
        private const val ACTION_NEXT = "tech.thispage.player_notification.NEXT"
        private const val ACTION_PREVIOUS = "tech.thispage.player_notification.PREVIOUS"

        // 添加静态通知引用
        @JvmStatic
        var currentNotification: Notification? = null
    }

    init {
        // 初始化媒体会话
        setupMediaSession()
        // 注册广播接收器
        registerReceiver()
    }

    /**
     * 设置通知是否为粘性。
     * @param sticky 是否为粘性通知
     */
    fun setSticky(sticky: Boolean) {
        isSticky = sticky
        if (sticky) {
            // 启动前台服务
            val intent = Intent(context, PlayerNotificationService::class.java).apply {
                action = PlayerNotificationService.ACTION_START
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)  // 添加新任务标志
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)  // 包含已停止的包
            }

            // 启动服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            // 如果通知不在显示，则恢复通知
            if (!isNotificationShowing) {
                currentNotification?.let { notification ->
                    // 先立即显示文本内容
                    val updatedNotification = createNotification(
                        title = notification.extras.getString("android.title") ?: "",
                        artist = notification.extras.getString("android.artist") ?: "",
                        bitmap = null,
                        lyrics = notification.extras.getString("android.subText") ?: "",
                        isPlaying = notification.extras.getBoolean("android.isPlaying", false),
                        position = notification.extras.getLong("android.position", 0),
                        duration = notification.extras.getLong("android.duration", 0)
                    )

                    // 立即显示通知
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                    Companion.currentNotification = updatedNotification
                    isNotificationShowing = true

                    // 如果有保存的图片URL，异步加载图片
                    lastImageUrl?.let { url ->
                        job?.cancel()
                        job = scope.launch {
                            val bitmap = loadBitmap(url)
                            if (bitmap != null) {
                                val notificationWithImage = createNotification(
                                    title = notification.extras.getString("android.title") ?: "",
                                    artist = notification.extras.getString("android.artist") ?: "",
                                    bitmap = bitmap,
                                    lyrics = notification.extras.getString("android.subText") ?: "",
                                    isPlaying = notification.extras.getBoolean("android.isPlaying", false),
                                    position = notification.extras.getLong("android.position", 0),
                                    duration = notification.extras.getLong("android.duration", 0)
                                )
                                notificationManager.notify(NOTIFICATION_ID, notificationWithImage)
                                Companion.currentNotification = notificationWithImage
                            }
                        }
                    }
                }
            }
        } else {
            // 停止前台服务
            val intent = Intent(context, PlayerNotificationService::class.java).apply {
                action = PlayerNotificationService.ACTION_STOP
            }
            context.startService(intent)
        }
    }

    /**
     * 注册广播接收器以监听播放控制动作。
     */
    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
        }
        context.registerReceiver(broadcastReceiver, filter)
    }

    /**
     * 更新通知内容。
     * @param title 歌曲标题
     * @param artist 艺术家名称
     * @param imageUrl 专辑封面图片 URL
     * @param lyrics 歌词
     * @param isPlaying 是否正在播放
     */
    fun updateNotification(
        title: String,
        artist: String,
        imageUrl: String?,
        lyrics: String,
        isPlaying: Boolean,
        position: Long = this.position,
        duration: Long = this.duration
    ) {
        this.position = position
        this.duration = duration
        this.lastImageUrl = imageUrl  // 保存图片URL

        // 先用空bitmap创建并显示通知，实现文本的即时更新
        var notification = createNotification(
            title = title,
            artist = artist,
            bitmap = null,
            lyrics = lyrics,
            isPlaying = isPlaying,
            position = position,
            duration = duration
        )
        notificationManager.notify(NOTIFICATION_ID, notification)
        currentNotification = notification
        isNotificationShowing = true

        // 如果有图片URL，则异步加载图片并更新通知
        if (imageUrl != null) {
            job?.cancel()
            job = scope.launch {
                val bitmap = loadBitmap(imageUrl)
                if (bitmap != null) {
                    notification = createNotification(
                        title = title,
                        artist = artist,
                        bitmap = bitmap,
                        lyrics = lyrics,
                        isPlaying = isPlaying,
                        position = position,
                        duration = duration
                    )
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    currentNotification = notification
                }
            }
        }
    }

    /**
     * 更新播放状态。
     * @param isPlaying 是否正在播放
     */
    fun updatePlayState(isPlaying: Boolean) {
        currentNotification?.let { notification ->
            val updatedNotification = createNotification(
                title = notification.extras.getString("android.title") ?: "",
                // 修改这里，从 android.text 改为 android.artist
                artist = notification.extras.getString("android.artist") ?: "",
                bitmap = null,
                lyrics = notification.extras.getString("android.subText") ?: "",
                isPlaying = isPlaying,
                position = notification.extras.getLong("android.position", 0),
                duration = notification.extras.getLong("android.duration", 0)
            )
            notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            currentNotification = updatedNotification
        }
    }

    /**
     * 更新歌词。
     * @param lyrics 新的歌词
     */
    fun updateLyrics(lyrics: String) {
        currentNotification?.let { notification ->
            val updatedNotification = createNotification(
                title = notification.extras.getString("android.title") ?: "",
                artist = notification.extras.getString("android.text") ?: "",
                bitmap = null,
                lyrics = lyrics,
                isPlaying = notification.extras.getBoolean("android.isPlaying", false),  // 从通知获取播放状态
                position = notification.extras.getLong("android.position", 0),  // 保持原有进度
                duration = notification.extras.getLong("android.duration", 0)   // 保持原有时长
            )
            notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            currentNotification = updatedNotification
        }
    }

    /**
     * 隐藏通知。
     */
    fun hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        currentNotification = null
        isNotificationShowing = false
    }

    /**
     * 创建通知。
     * @param title 歌曲标题
     * @param artist 艺术家名称
     * @param bitmap 专辑封面图片
     * @param lyrics 歌词
     * @param isPlaying 是否正在播放
     * @return 构建的通知对象
     */
    private fun createNotification(
        title: String,
        artist: String,
        bitmap: Bitmap?,
        lyrics: String,
        isPlaying: Boolean,
        position: Long = this.position,
        duration: Long = this.duration
    ): Notification {
        // 创建播放/暂停意图
        val playIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY).apply {
                `package` = context.packageName
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建上一首意图
        val prevIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(ACTION_PREVIOUS).apply {
                `package` = context.packageName
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建下一首意图
        val nextIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(ACTION_NEXT).apply {
                `package` = context.packageName
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 格式化时间显示
        val positionText = String.format("%02d:%02d", 
            position / 1000 / 60,
            (position / 1000) % 60
        )
        val durationText = String.format("%02d:%02d",
            duration / 1000 / 60,
            (duration / 1000) % 60
        )

        android.util.Log.d("PlayerNotification", "Formatting time: position=$positionText, duration=$durationText")

        // 构建通知
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(bitmap)
            .setShowWhen(false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAllowSystemGeneratedContextualActions(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        // 添加控制按钮
        builder.addAction(
            R.drawable.media_previous_button,
            "Previous",
            prevIntent
        ).addAction(
            if (isPlaying) R.drawable.media_pause_button else R.drawable.media_play_button,
            if (isPlaying) "Pause" else "Play",
            playIntent
        ).addAction(
            R.drawable.media_next_button,
            "Next",
            nextIntent
        )

        // 设置媒体样式
        builder.setStyle(MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2))

        // 添加进度条
        if (duration > 0) {
            android.util.Log.d("PlayerNotification", "Setting progress bar: position=$position, duration=$duration")
            builder.setProgress(duration.toInt(), position.toInt(), false)
        }

        // 更新媒体会话状态
        mediaSession?.setPlaybackState(
            android.support.v4.media.session.PlaybackStateCompat.Builder()
                .setState(
                    if (isPlaying) 
                        android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING 
                    else 
                        android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED,
                    position,
                    1.0f
                )
                .setActions(
                    android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY or
                    android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE or
                    android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )

        // 设置媒体元数据
        mediaSession?.setMetadata(
            android.support.v4.media.MediaMetadataCompat.Builder()
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .build()
        )

        // 构建通知对象
        val notification = builder.build()

        // 设置系统通知标志
        notification.flags = notification.flags or (
            Notification.FLAG_ONGOING_EVENT or
            Notification.FLAG_NO_CLEAR or
            Notification.FLAG_FOREGROUND_SERVICE
        )

        // 保存播放状态和进度信息
        notification.extras.apply {
            putBoolean("android.isPlaying", isPlaying)
            putLong("android.position", position)
            putLong("android.duration", duration)
            putString("android.artist", artist)
            android.util.Log.d("PlayerNotification", "Saved to extras: artist=$artist, position=$position, duration=$duration")
        }

        // 保存通知引用
        Companion.currentNotification = notification

        return notification
    }

    /**
     * 加载位图。
     * @param url 图片 URL
     * @return 加载的位图
     */
    private suspend fun loadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connect()
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    /**
     * 设置媒体会话。
     */
    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(context, "PlayerNotification").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    vibrate()
                    channel.invokeMethod("onPlay", null)
                }

                override fun onPause() {
                    vibrate()
                    channel.invokeMethod("onPause", null)
                }

                override fun onSkipToNext() {
                    vibrate()
                    channel.invokeMethod("onNext", null)
                }

                override fun onSkipToPrevious() {
                    vibrate()
                    channel.invokeMethod("onPrevious", null)
                }

                override fun onSeekTo(pos: Long) {
                    vibrate()
                    // 发送新的进度到 Flutter
                    channel.invokeMethod("onProgressChanged", mapOf(
                        "position" to pos,
                        "duration" to duration
                    ))
                    // 更新通知进度
                    updateProgress(pos, duration)
                }

            })
            
            isActive = true
        }
    }

    fun updateProgress(position: Long, duration: Long) {
        this.position = position
        this.duration = duration

        currentNotification?.let { notification ->
            val updatedNotification = createNotification(
                title = notification.extras.getString("android.title") ?: "",
                artist = notification.extras.getString("android.artist") ?: "",  // 使用正确的 key
                bitmap = null,
                lyrics = notification.extras.getString("android.subText") ?: "",
                isPlaying = notification.extras.getBoolean("android.isPlaying", false),
                position = position,
                duration = duration
            )
            notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            currentNotification = updatedNotification
        }
    }

    /**
     * 释放资源。
     */
    fun dispose() {
        job?.cancel()
        scope.cancel()
        mediaSession?.release()
        mediaSession = null

        // 停止前台服务
        val intent = Intent(context, PlayerNotificationService::class.java).apply {
            action = PlayerNotificationService.ACTION_STOP
        }
        context.startService(intent)

        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}