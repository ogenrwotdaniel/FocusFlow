class FocusSession {
  final String id;
  final DateTime startTime;
  final DateTime? endTime;
  final int durationMinutes;
  final bool completed;
  final SessionType type;

  FocusSession({
    required this.id,
    required this.startTime,
    this.endTime,
    required this.durationMinutes,
    this.completed = false,
    this.type = SessionType.focus,
  });

  // Calculate actual duration in minutes
  int get actualDurationMinutes {
    if (endTime == null) {
      return 0;
    }
    return endTime!.difference(startTime).inMinutes;
  }

  // Calculate completion percentage
  double get completionPercentage {
    if (endTime == null) {
      return 0.0;
    }
    if (!completed) {
      return 0.0;
    }
    return (actualDurationMinutes / durationMinutes) * 100;
  }

  // Create a copy of the session with updated values
  FocusSession copyWith({
    String? id,
    DateTime? startTime,
    DateTime? endTime,
    int? durationMinutes,
    bool? completed,
    SessionType? type,
  }) {
    return FocusSession(
      id: id ?? this.id,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      durationMinutes: durationMinutes ?? this.durationMinutes,
      completed: completed ?? this.completed,
      type: type ?? this.type,
    );
  }

  // Convert session to Map
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'startTime': startTime.millisecondsSinceEpoch,
      'endTime': endTime?.millisecondsSinceEpoch,
      'durationMinutes': durationMinutes,
      'completed': completed ? 1 : 0,
      'type': type.index,
    };
  }

  // Create session from Map
  factory FocusSession.fromMap(Map<String, dynamic> map) {
    return FocusSession(
      id: map['id'],
      startTime: DateTime.fromMillisecondsSinceEpoch(map['startTime']),
      endTime: map['endTime'] != null
          ? DateTime.fromMillisecondsSinceEpoch(map['endTime'])
          : null,
      durationMinutes: map['durationMinutes'],
      completed: map['completed'] == 1,
      type: SessionType.values[map['type']],
    );
  }
}

enum SessionType {
  focus,
  shortBreak,
  longBreak,
}
