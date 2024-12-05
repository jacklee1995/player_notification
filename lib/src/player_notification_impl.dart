import 'player_notification_platform_interface.dart';
import 'models/player_notification_data.dart';

class PlayerNotification {
  static PlayerNotificationPlatform get _platform =>
      PlayerNotificationPlatform.instance;

  // 控制常驻
  Future<void> setSticky(bool sticky) => _platform.setSticky(sticky);

  Future<void> show({
    required String title,
    required String artist,
    String? imageUrl,
    String lyrics = '',
    bool isPlaying = false,
    Duration position = Duration.zero,
    Duration duration = Duration.zero,
  }) {
    return _platform.show(PlayerNotificationData(
      title: title,
      artist: artist,
      imageUrl: imageUrl,
      lyrics: lyrics,
      isPlaying: isPlaying,
      position: position,  // 传递进度
      duration: duration,  // 传递时长
    ));
  }

  Future<void> hide() => _platform.hide();

  Future<void> updatePlayState(bool isPlaying) =>
      _platform.updatePlayState(isPlaying);

  Future<void> updateLyrics(String lyrics) => _platform.updateLyrics(lyrics);

  void setPlaybackListener({
    void Function()? onPlay,
    void Function()? onPause,
    void Function()? onNext,
    void Function()? onPrevious,
  }) {
    _platform.setPlaybackListener(
      onPlay: onPlay,
      onPause: onPause,
      onNext: onNext,
      onPrevious: onPrevious,
    );
  }

  Future<void> updateProgress(Duration position, Duration duration) {
    return _platform.updateProgress(position, duration);
  }

  void setProgressListener(void Function(Duration position, Duration duration)? onProgressChanged) {
    _platform.setProgressListener(onProgressChanged);
  }
}
