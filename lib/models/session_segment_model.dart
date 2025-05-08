import 'package:focus_flow/models/session_model.dart';

class SessionSegment {
  final SessionType type;
  final int durationMinutes;

  SessionSegment({
    required this.type,
    required this.durationMinutes,
  });

  SessionSegment copyWith({
    SessionType? type,
    int? durationMinutes,
  }) {
    return SessionSegment(
      type: type ?? this.type,
      durationMinutes: durationMinutes ?? this.durationMinutes,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'type': type.index, // Storing enum as index
      'durationMinutes': durationMinutes,
    };
  }

  factory SessionSegment.fromMap(Map<String, dynamic> map) {
    return SessionSegment(
      type: SessionType.values[map['type'] as int],
      durationMinutes: map['durationMinutes'] as int,
    );
  }

  @override
  String toString() => 'SessionSegment(type: $type, durationMinutes: $durationMinutes)';

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
  
    return other is SessionSegment &&
      other.type == type &&
      other.durationMinutes == durationMinutes;
  }

  @override
  int get hashCode => type.hashCode ^ durationMinutes.hashCode;
}
