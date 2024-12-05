class PlayerNotificationData {
  final String title;
  final String artist;
  final String? imageUrl;
  final String lyrics;
  final bool isPlaying;
  final Duration position;
  final Duration duration;

  PlayerNotificationData({
    required this.title,
    required this.artist,
    this.imageUrl,
    this.lyrics = '',
    this.isPlaying = false,
    this.position = Duration.zero,
    this.duration = Duration.zero,
  });

  Map<String, dynamic> toMap() {
    return {
      'title': title,
      'artist': artist,
      'imageUrl': imageUrl,
      'lyrics': lyrics,
      'isPlaying': isPlaying,
      'position': position.inMilliseconds,
      'duration': duration.inMilliseconds,
    };
  }

  factory PlayerNotificationData.fromMap(Map<String, dynamic> map) {
    return PlayerNotificationData(
      title: map['title'] as String,
      artist: map['artist'] as String,
      imageUrl: map['imageUrl'] as String?,
      lyrics: map['lyrics'] as String? ?? '',
      isPlaying: map['isPlaying'] as bool? ?? false,
      position: Duration(milliseconds: map['position'] as int? ?? 0),
      duration: Duration(milliseconds: map['duration'] as int? ?? 0),
    );
  }
}
