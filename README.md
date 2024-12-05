# player_notification

一个用于在通知栏显示音乐播放器控制器的 Flutter 插件。

## 功能特点

- 在通知栏显示音乐播放控制器
- 支持显示歌曲标题、艺术家、专辑封面
- 支持显示和更新歌词
- 提供播放/暂停、上一首/下一首控制按钮
- 支持进度条显示和控制
- 支持粘性通知（可选）
- 支持锁屏控制

## 安装

将以下内容添加到你的 `pubspec.yaml` 文件中：

```yaml
dependencies:
  player_notification: ^1.0.0
```

## 使用方法

### 基本用法

```dart
import 'package:player_notification/player_notification.dart';

// 创建播放器通知数据
final data = PlayerNotificationData(
  title: '歌曲标题',
  artist: '艺术家',
  imageUrl: '专辑封面URL',
  lyrics: '歌词',
  isPlaying: true,
  position: Duration(seconds: 30),
  duration: Duration(minutes: 3),
);

// 显示通知
await PlayerNotification.show(data);

// 隐藏通知
await PlayerNotification.hide();
```

### 设置粘性通知

```dart
// 设置为粘性通知（不可滑动删除）
await PlayerNotification.setSticky(true);
```

### 更新播放状态

```dart
// 更新播放/暂停状态
await PlayerNotification.updatePlayState(isPlaying: true);
```

### 更新歌词

```dart
// 更新歌词显示
await PlayerNotification.updateLyrics('新的歌词内容');
```

### 更新进度

```dart
// 更新播放进度
await PlayerNotification.updateProgress(
  position: Duration(seconds: 45),
  duration: Duration(minutes: 3),
);
```

### 监听控制事件

```dart
// 设置播放控制监听器
PlayerNotification.setPlaybackListener(
  onPlay: () {
    // 处理播放事件
  },
  onPause: () {
    // 处理暂停事件
  },
  onNext: () {
    // 处理下一首事件
  },
  onPrevious: () {
    // 处理上一首事件
  },
);

// 设置进度变化监听器
PlayerNotification.setProgressListener((position, duration) {
  // 处理进度变化事件
});
```

## Android 配置

在 `android/app/src/main/AndroidManifest.xml` 中添加以下权限：

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.VIBRATE" />
```

并在 `application` 标签内添加服务声明：

```xml
<service android:name="tech.thispage.player_notification.PlayerNotificationService" />
```

## 注意事项

1. 目前仅支持 Android 平台
2. 需要 Android API Level 16 或更高版本
3. 粘性通知需要 `FOREGROUND_SERVICE` 权限
4. 震动反馈需要 `VIBRATE` 权限

## 示例

查看 [example](example) 目录获取完整示例。

## 许可证

MPL License
