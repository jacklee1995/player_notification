import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'player_notification_method_channel.dart';
import 'models/player_notification_data.dart';

abstract class PlayerNotificationPlatform extends PlatformInterface {
  PlayerNotificationPlatform() : super(token: _token);

  static final Object _token = Object();
  static PlayerNotificationPlatform _instance =
      MethodChannelPlayerNotification();

  static PlayerNotificationPlatform get instance => _instance;

  static set instance(PlayerNotificationPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> setSticky(bool sticky) {
    throw UnimplementedError('setSticky() has not been implemented.');
  }

  Future<void> show(PlayerNotificationData data) {
    throw UnimplementedError('show() has not been implemented.');
  }

  Future<void> hide() {
    throw UnimplementedError('hide() has not been implemented.');
  }

  Future<void> updatePlayState(bool isPlaying) {
    throw UnimplementedError('updatePlayState() has not been implemented.');
  }

  Future<void> updateLyrics(String lyrics) {
    throw UnimplementedError('updateLyrics() has not been implemented.');
  }

  void setPlaybackListener({
    void Function()? onPlay,
    void Function()? onPause,
    void Function()? onNext,
    void Function()? onPrevious,
  }) {
    throw UnimplementedError('setPlaybackListener() has not been implemented.');
  }

  // 新的进度相关方法
  Future<void> updateProgress(Duration position, Duration duration) {
    throw UnimplementedError('updateProgress() has not been implemented.');
  }

  void setProgressListener(void Function(Duration position, Duration duration)? onProgressChanged) {
    throw UnimplementedError('setProgressListener() has not been implemented.');
  }
}
