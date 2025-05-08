// lib/models/session_log_model.dart
import 'package:flutter/foundation.dart';

enum SessionLogType { focus, shortBreak, longBreak }

String sessionLogTypeToString(SessionLogType type) {
  return type.toString().split('.').last;
}

SessionLogType sessionLogTypeFromString(String typeString) {
  return SessionLogType.values.firstWhere(
    (e) => describeEnum(e) == typeString,
    orElse: () => SessionLogType.focus, // Default or throw error
  );
}

class SessionLog {
  final String id; // Unique ID for each log
  final DateTime startTime;
  final DateTime endTime;
  final int durationMinutes; // Actual duration
  final SessionLogType sessionType;
  // final String? notes; // Optional: for future notes per session
  // final String? taskId; // Optional: for linking to a task

  SessionLog({
    required this.id,
    required this.startTime,
    required this.endTime,
    required this.durationMinutes,
    required this.sessionType,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'startTime': startTime.toIso8601String(),
        'endTime': endTime.toIso8601String(),
        'durationMinutes': durationMinutes,
        'sessionType': sessionLogTypeToString(sessionType),
      };

  factory SessionLog.fromJson(Map<String, dynamic> json) => SessionLog(
        id: json['id'] as String,
        startTime: DateTime.parse(json['startTime'] as String),
        endTime: DateTime.parse(json['endTime'] as String),
        durationMinutes: json['durationMinutes'] as int,
        sessionType: sessionLogTypeFromString(json['sessionType'] as String),
      );
}
