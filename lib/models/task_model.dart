import 'package:uuid/uuid.dart';

enum TaskStatus {
  todo,
  inProgress,
  completed,
}

class Task {
  final String id;
  final String name;
  final String description;
  final DateTime createdAt;
  final DateTime? completedAt;
  final TaskStatus status;
  final int priority;
  final int estimatedPomodoros;
  final int completedPomodoros;
  final String? projectId;

  Task({
    String? id,
    required this.name,
    this.description = '',
    DateTime? createdAt,
    this.completedAt,
    this.status = TaskStatus.todo,
    this.priority = 2,
    this.estimatedPomodoros = 1,
    this.completedPomodoros = 0,
    this.projectId,
  }) : 
    id = id ?? const Uuid().v4(),
    createdAt = createdAt ?? DateTime.now();
  
  Task copyWith({
    String? id,
    String? name,
    String? description,
    DateTime? createdAt,
    DateTime? completedAt,
    bool? clearCompletedAt,
    TaskStatus? status,
    int? priority,
    int? estimatedPomodoros,
    int? completedPomodoros,
    String? projectId,
  }) {
    return Task(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      createdAt: createdAt ?? this.createdAt,
      completedAt: clearCompletedAt == true ? null : completedAt ?? this.completedAt,
      status: status ?? this.status,
      priority: priority ?? this.priority,
      estimatedPomodoros: estimatedPomodoros ?? this.estimatedPomodoros,
      completedPomodoros: completedPomodoros ?? this.completedPomodoros,
      projectId: projectId ?? this.projectId,
    );
  }
  
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'createdAt': createdAt.millisecondsSinceEpoch,
      'completedAt': completedAt?.millisecondsSinceEpoch,
      'status': status.index,
      'priority': priority,
      'estimatedPomodoros': estimatedPomodoros,
      'completedPomodoros': completedPomodoros,
      'projectId': projectId,
    };
  }
  
  factory Task.fromMap(Map<String, dynamic> map) {
    return Task(
      id: map['id'],
      name: map['name'],
      description: map['description'],
      createdAt: DateTime.fromMillisecondsSinceEpoch(map['createdAt']),
      completedAt: map['completedAt'] != null 
          ? DateTime.fromMillisecondsSinceEpoch(map['completedAt'])
          : null,
      status: TaskStatus.values[map['status']],
      priority: map['priority'],
      estimatedPomodoros: map['estimatedPomodoros'] ?? 1,
      completedPomodoros: map['completedPomodoros'] ?? 0,
      projectId: map['projectId'],
    );
  }
}
