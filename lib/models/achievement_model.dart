import 'package:flutter/material.dart';

enum MilestoneType {
  focusSessionsCompleted,
  totalFocusHours,
  tasksCompleted,
  dailyStreakReached,
  // Add more types as needed
}

class Achievement {
  final String id;
  final String name;
  final String description;
  final String iconName; // Could be a key for IconData or an asset path
  final MilestoneType milestoneType;
  final int milestoneValue;
  bool isUnlocked;
  DateTime? unlockedAt;

  Achievement({
    required this.id,
    required this.name,
    required this.description,
    required this.iconName,
    required this.milestoneType,
    required this.milestoneValue,
    this.isUnlocked = false,
    this.unlockedAt,
  });

  // Optional: Add toMap and fromMap for persistence if not handled solely by provider logic
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'iconName': iconName,
      'milestoneType': milestoneType.index, // Store enum as index
      'milestoneValue': milestoneValue,
      'isUnlocked': isUnlocked,
      'unlockedAt': unlockedAt?.millisecondsSinceEpoch,
    };
  }

  factory Achievement.fromMap(Map<String, dynamic> map) {
    return Achievement(
      id: map['id'],
      name: map['name'],
      description: map['description'],
      iconName: map['iconName'],
      milestoneType: MilestoneType.values[map['milestoneType']],
      milestoneValue: map['milestoneValue'],
      isUnlocked: map['isUnlocked'] ?? false,
      unlockedAt: map['unlockedAt'] != null
          ? DateTime.fromMillisecondsSinceEpoch(map['unlockedAt'])
          : null,
    );
  }
}
