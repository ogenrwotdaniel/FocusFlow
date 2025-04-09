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
  final TaskStatus status;
  final int priority; // 1 = Low, 2 = Medium, 3 = High
  
  Task({
    String? id,
    required this.name,
    this.description = '',
    DateTime? createdAt,
    this.status = TaskStatus.todo,
    this.priority = 2,
  }) : 
    id = id ?? const Uuid().v4(),
    createdAt = createdAt ?? DateTime.now();
  
  Task copyWith({
    String? id,
    String? name,
    String? description,
    DateTime? createdAt,
    TaskStatus? status,
    int? priority,
  }) {
    return Task(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      createdAt: createdAt ?? this.createdAt,
      status: status ?? this.status,
      priority: priority ?? this.priority,
    );
  }
  
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'createdAt': createdAt.millisecondsSinceEpoch,
      'status': status.index,
      'priority': priority,
    };
  }
  
  factory Task.fromMap(Map<String, dynamic> map) {
    return Task(
      id: map['id'],
      name: map['name'],
      description: map['description'],
      createdAt: DateTime.fromMillisecondsSinceEpoch(map['createdAt']),
      status: TaskStatus.values[map['status']],
      priority: map['priority'],
    );
  }
}
