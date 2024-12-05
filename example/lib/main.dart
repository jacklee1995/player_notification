import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:player_notification/player_notification.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  if (Platform.isAndroid) {
    // 请求通知权限
    await Permission.notification.request();

    // 如果需要系统级权限
    if (await Permission.systemAlertWindow.status.isDenied) {
      await Permission.systemAlertWindow.request();
    }
  }

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Player Notification Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const PlayerNotificationDemo(),
    );
  }
}

class PlayerNotificationDemo extends StatefulWidget {
  const PlayerNotificationDemo({super.key});

  @override
  State<PlayerNotificationDemo> createState() => _PlayerNotificationDemoState();
}

class _PlayerNotificationDemoState extends State<PlayerNotificationDemo> {
  final _playerNotification = PlayerNotification();
  bool _isPlaying = false;
  String _currentLyrics = '暂无歌词';
  int _currentSongIndex = 0;
  bool _isSticky = true; // 常驻状态控制
  Duration _position = Duration.zero;
  Duration _duration = const Duration(minutes: 3, seconds: 30); // 设置一个默认时长
  Timer? _progressTimer;

  // 模拟歌曲列表
  final List<Map<String, String>> _songs = [
    {
      'title': '起风了',
      'artist': '周深',
      'imageUrl':
          'https://onecms-res.cloudinary.com/image/upload/s--2Rbc2xc5--/f_auto,q_auto/c_fill,g_auto,h_622,w_830/v1/tdy-migration/19410372.JPG?itok=dEoez4y4',
      'lyrics': '这一路上走走停停...',
    },
    {
      'title': '我记得',
      'artist': '赵雷',
      'imageUrl': 'https://example.com/song2.jpg',
      'lyrics': '清晨从梦中醒来...',
    },
    {
      'title': '像我这样的人',
      'artist': '毛不易',
      'imageUrl': 'https://example.com/song3.jpg',
      'lyrics': '像我这样优秀的人...',
    },
  ];

  @override
  void initState() {
    super.initState();
    _setupNotificationListeners();
    _showCurrentSong();
    _startProgressTimer();
  }

  void _setupNotificationListeners() {
    _playerNotification.setPlaybackListener(
      onPlay: () {
        setState(() {
          _isPlaying = true;
          _updateNotificationPlayState();
        });
      },
      onPause: () {
        setState(() {
          _isPlaying = false;
          _updateNotificationPlayState();
        });
      },
      onNext: () {
        setState(() {
          _currentSongIndex = (_currentSongIndex + 1) % _songs.length;
          // 直接调用 show 更新所有信息
          _showCurrentSong();
        });
      },
      onPrevious: () {
        setState(() {
          _currentSongIndex =
              (_currentSongIndex - 1 + _songs.length) % _songs.length;
          // 直接调用 show 更新所有信息
          _showCurrentSong();
        });
      },
    );

    // 进度条拖动监听
    _playerNotification.setProgressListener((position, duration) {
      setState(() {
        _position = position;
        // 如果需要，也可以更新 duration
        // _duration = duration;
      });

      // 停止计时器，防止与拖动冲突
      _progressTimer?.cancel();

      // 重新启动计时器
      _startProgressTimer();
    });
  }

  void _showCurrentSong() {
    final song = _songs[_currentSongIndex];
    _playerNotification.show(
      title: song['title']!,
      artist: song['artist']!,
      imageUrl: song['imageUrl'],
      lyrics: song['lyrics']!,
      isPlaying: _isPlaying,
      position: _position,
      duration: _duration,
    );
    setState(() {
      _currentLyrics = song['lyrics']!;
      _position = Duration.zero; // 重置播放位置
      _duration = const Duration(minutes: 3, seconds: 30); // 设置新歌曲的时长
      _playerNotification.updateProgress(_position, _duration);
    });
  }

  void _startProgressTimer() {
    _progressTimer?.cancel();
    _progressTimer = Timer.periodic(const Duration(milliseconds: 500), (timer) {
      if (_isPlaying) {
        setState(() {
          _position += const Duration(milliseconds: 500);
          if (_position > _duration) {
            _position = Duration.zero;
          }
          _playerNotification.updateProgress(_position, _duration);
        });
      }
    });
  }

  void _updateNotificationPlayState() {
    _playerNotification.updatePlayState(_isPlaying);
  }

  void _playNextSong() {
    setState(() {
      _currentSongIndex = (_currentSongIndex + 1) % _songs.length;
      _showCurrentSong();
    });
  }

  void _playPreviousSong() {
    setState(() {
      _currentSongIndex =
          (_currentSongIndex - 1 + _songs.length) % _songs.length;
      _showCurrentSong();
    });
  }

  void _togglePlayPause() {
    setState(() {
      _isPlaying = !_isPlaying;
      _updateNotificationPlayState();
    });
  }

  void _updateLyrics(String lyrics) {
    setState(() {
      _currentLyrics = lyrics;
      _playerNotification.updateLyrics(lyrics);
    });
  }

  @override
  Widget build(BuildContext context) {
    final currentSong = _songs[_currentSongIndex];

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('播放器通知示例'),
        actions: [
          // 常驻通知栏控制开关
          Switch(
            value: _isSticky,
            onChanged: (value) {
              setState(() {
                _isSticky = value;
                _playerNotification.setSticky(_isSticky);
              });
            },
          ),
        ],
      ),
      body: Column(
        children: [
          Center(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // 歌曲信息
                  Text(
                    currentSong['title']!,
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    currentSong['artist']!,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 32),

                  // 歌词显示
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.grey[200],
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      _currentLyrics,
                      textAlign: TextAlign.center,
                      style: const TextStyle(fontSize: 16),
                    ),
                  ),
                  const SizedBox(height: 32),

                  // 播放控制
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.skip_previous),
                        iconSize: 48,
                        onPressed: _playPreviousSong,
                      ),
                      const SizedBox(width: 16),
                      IconButton(
                        icon: Icon(_isPlaying ? Icons.pause : Icons.play_arrow),
                        iconSize: 64,
                        onPressed: _togglePlayPause,
                      ),
                      const SizedBox(width: 16),
                      IconButton(
                        icon: const Icon(Icons.skip_next),
                        iconSize: 48,
                        onPressed: _playNextSong,
                      ),
                    ],
                  ),
                  const SizedBox(height: 32),

                  // 测试按钮
                  ElevatedButton(
                    onPressed: () {
                      _updateLyrics('测试更新歌词 - ${DateTime.now().toString()}');
                    },
                    child: const Text('更新歌词'),
                  ),
                ],
              ),
            ),
          ),
          // 进度条
          Slider(
            value: _position.inSeconds.toDouble(),
            max: _duration.inSeconds.toDouble(),
            onChanged: (value) {
              setState(() {
                _position = Duration(seconds: value.toInt());
                _playerNotification.updateProgress(_position, _duration);
              });
            },
          ),

          // 时间显示
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(_formatDuration(_position)),
              Text(_formatDuration(_duration)),
            ],
          ),
        ],
      ),
    );
  }

  // 格式化时间的方法
  String _formatDuration(Duration duration) {
    String twoDigits(int n) => n.toString().padLeft(2, '0');
    String minutes = twoDigits(duration.inMinutes.remainder(60));
    String seconds = twoDigits(duration.inSeconds.remainder(60));
    return "$minutes:$seconds";
  }

  @override
  void dispose() {
    _progressTimer?.cancel();
    _playerNotification.hide();
    super.dispose();
  }
}
