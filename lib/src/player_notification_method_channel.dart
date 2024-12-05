import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'player_notification_platform_interface.dart';
import 'models/player_notification_data.dart';

class MethodChannelPlayerNotification extends PlayerNotificationPlatform {
  @visibleForTesting
  final methodChannel =
      const MethodChannel('tech.thispage.player_notification');

  void Function()? _onPlay;
  void Function()? _onPause;
  void Function()? _onNext;
  void Function()? _onPrevious;
  void Function(Duration position, Duration duration)? _onProgressChanged;

  MethodChannelPlayerNotification() {
    methodChannel.setMethodCallHandler(_handleMethodCall);
  }

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onPlay':
        _onPlay?.call();
        break;
      case 'onPause':
        _onPause?.call();
        break;
      case 'onNext':
        _onNext?.call();
        break;
      case 'onPrevious':
        _onPrevious?.call();
        break;
      case 'onProgressChanged':
        final position = Duration(milliseconds: call.arguments['position'] as int);
        final duration = Duration(milliseconds: call.arguments['duration'] as int);
        _onProgressChanged?.call(position, duration);
        break;
    }
  }

  @override
  Future<void> setSticky(bool sticky) async {
    await methodChannel.invokeMethod('setSticky', {
      'sticky': sticky,
    });
  }

  @override
  Future<void> show(PlayerNotificationData data) async {
    await methodChannel.invokeMethod('showNotification', data.toMap());
  }

  @override
  Future<void> hide() async {
    await methodChannel.invokeMethod('hideNotification');
  }

  @override
  Future<void> updatePlayState(bool isPlaying) async {
    await methodChannel.invokeMethod('updatePlayState', {
      'isPlaying': isPlaying,
    });
  }

  @override
  Future<void> updateLyrics(String lyrics) async {
    await methodChannel.invokeMethod('updateLyrics', {
      'lyrics': lyrics,
    });
  }

  @override
  void setPlaybackListener({
    void Function()? onPlay,
    void Function()? onPause,
    void Function()? onNext,
    void Function()? onPrevious,
  }) {
    _onPlay = onPlay;
    _onPause = onPause;
    _onNext = onNext;
    _onPrevious = onPrevious;
  }

  @override
  Future<void> updateProgress(Duration position, Duration duration) async {
    await methodChannel.invokeMethod('updateProgress', {
      'position': position.inMilliseconds,
      'duration': duration.inMilliseconds,
    });
  }

  @override
  void setProgressListener(void Function(Duration position, Duration duration)? onProgressChanged) {
    _onProgressChanged = onProgressChanged;
  }
}
