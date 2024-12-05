// import 'package:flutter_test/flutter_test.dart';
// import 'package:player_notification/src/player_notification_impl.dart';
// import 'package:player_notification/src/player_notification_platform_interface.dart';
// import 'package:player_notification/src/player_notification_method_channel.dart';
// import 'package:plugin_platform_interface/plugin_platform_interface.dart';

// class MockPlayerNotificationPlatform
//     with MockPlatformInterfaceMixin
//     implements PlayerNotificationPlatform {

//   @override
//   Future<String?> getPlatformVersion() => Future.value('42');
// }

// void main() {
//   final PlayerNotificationPlatform initialPlatform = PlayerNotificationPlatform.instance;

//   test('$MethodChannelPlayerNotification is the default instance', () {
//     expect(initialPlatform, isInstanceOf<MethodChannelPlayerNotification>());
//   });

//   test('getPlatformVersion', () async {
//     PlayerNotification playerNotificationPlugin = PlayerNotification();
//     MockPlayerNotificationPlatform fakePlatform = MockPlayerNotificationPlatform();
//     PlayerNotificationPlatform.instance = fakePlatform;

//     expect(await playerNotificationPlugin.getPlatformVersion(), '42');
//   });
// }
