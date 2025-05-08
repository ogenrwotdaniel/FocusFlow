import 'package:focus_flow/models/session_model.dart'; // For SessionType
import 'package:uuid/uuid.dart';
import 'dart:convert';

const _uuid = Uuid();

class ScheduledSession {
  final String id;
  final String? name; // Optional custom name for the scheduled session
  final DateTime plannedStartTime;
  final int plannedDurationMinutes;
  final SessionType sessionType;
  final String? notes;
  final bool isStarted; // To track if this scheduled session has been converted to an active session
  final int? notificationId; // For scheduling local notifications
  final int? windDownNotificationId; // For the wind-down reminder

  ScheduledSession({
    String? id,
    this.name,
    required this.plannedStartTime,
    required this.plannedDurationMinutes,
    required this.sessionType,
    this.notes,
    this.isStarted = false,
    this.notificationId,
    this.windDownNotificationId,
  }) : id = id ?? _uuid.v4();

  ScheduledSession copyWith({
    String? id,
    String? name,
    DateTime? plannedStartTime,
    int? plannedDurationMinutes,
    SessionType? sessionType,
    String? notes,
    bool? isStarted,
    int? notificationId,
    int? windDownNotificationId,
  }) {
    return ScheduledSession(
      id: id ?? this.id,
      name: name ?? this.name,
      plannedStartTime: plannedStartTime ?? this.plannedStartTime,
      plannedDurationMinutes: plannedDurationMinutes ?? this.plannedDurationMinutes,
      sessionType: sessionType ?? this.sessionType,
      notes: notes ?? this.notes,
      isStarted: isStarted ?? this.isStarted,
      notificationId: notificationId ?? this.notificationId,
      windDownNotificationId: windDownNotificationId ?? this.windDownNotificationId,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'plannedStartTime': plannedStartTime.millisecondsSinceEpoch,
      'plannedDurationMinutes': plannedDurationMinutes,
      'sessionType': sessionType.index,
      'notes': notes,
      'isStarted': isStarted,
      'notificationId': notificationId,
      'windDownNotificationId': windDownNotificationId,
    };
  }

  factory ScheduledSession.fromMap(Map<String, dynamic> map) {
    return ScheduledSession(
      id: map['id'] as String,
      name: map['name'] as String?,
      plannedStartTime: DateTime.fromMillisecondsSinceEpoch(map['plannedStartTime'] as int),
      plannedDurationMinutes: map['plannedDurationMinutes'] as int,
      sessionType: SessionType.values[map['sessionType'] as int],
      notes: map['notes'] as String?,
      isStarted: map['isStarted'] as bool? ?? false,
      notificationId: map['notificationId'] as int?,
      windDownNotificationId: map['windDownNotificationId'] as int?,
    );
  }

  String toJson() => json.encode(toMap());
}
