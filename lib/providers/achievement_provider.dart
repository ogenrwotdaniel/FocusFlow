import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'package:focus_flow/models/achievement_model.dart';

class AchievementProvider with ChangeNotifier {
  List<Achievement> _achievements = [];
  static const String _achievementsKey = 'unlocked_achievements_data';

  AchievementProvider() {
    _initializeAchievements();
    _loadAchievementsStatus();
  }

  List<Achievement> get allAchievements => _achievements;
  List<Achievement> get unlockedAchievements => _achievements.where((a) => a.isUnlocked).toList();

  void _initializeAchievements() {
    // Define all possible achievements here
    _achievements = [
      // Focus Sessions Completed
      Achievement(
        id: 'focus_sessions_1',
        name: 'Focus Seedling',
        description: 'Completed 1 focus session.',
        iconName: 'emoji_events_outlined', // Placeholder icon
        milestoneType: MilestoneType.focusSessionsCompleted,
        milestoneValue: 1,
      ),
      Achievement(
        id: 'focus_sessions_5',
        name: 'Focus Sprout',
        description: 'Completed 5 focus sessions.',
        iconName: 'emoji_events_outlined',
        milestoneType: MilestoneType.focusSessionsCompleted,
        milestoneValue: 5,
      ),
      Achievement(
        id: 'focus_sessions_10',
        name: 'Focus Sapling',
        description: 'Completed 10 focus sessions.',
        iconName: 'star_outline', 
        milestoneType: MilestoneType.focusSessionsCompleted,
        milestoneValue: 10,
      ),
      Achievement(
        id: 'focus_sessions_25',
        name: 'Focused Junior',
        description: 'Completed 25 focus sessions.',
        iconName: 'star',
        milestoneType: MilestoneType.focusSessionsCompleted,
        milestoneValue: 25,
      ),
      Achievement(
        id: 'focus_sessions_50',
        name: 'Focused Senior',
        description: 'Completed 50 focus sessions.',
        iconName: 'military_tech_outlined',
        milestoneType: MilestoneType.focusSessionsCompleted,
        milestoneValue: 50,
      ),
      Achievement(
        id: 'focus_sessions_100',
        name: 'Focus Master',
        description: 'Completed 100 focus sessions.',
        iconName: 'military_tech',
        milestoneType: MilestoneType.focusSessionsCompleted,
        milestoneValue: 100,
      ),

      // Total Focus Hours (value in minutes)
      Achievement(
        id: 'focus_hours_1',
        name: 'Hour Hero (1H)',
        description: 'Accumulate 1 hour of focus time.',
        iconName: 'timer_outlined',
        milestoneType: MilestoneType.totalFocusHours,
        milestoneValue: 60, // 1 hour
      ),
      Achievement(
        id: 'focus_hours_5',
        name: 'Hour Hero (5H)',
        description: 'Accumulate 5 hours of focus time.',
        iconName: 'timer',
        milestoneType: MilestoneType.totalFocusHours,
        milestoneValue: 300, // 5 hours
      ),
      Achievement(
        id: 'focus_hours_10',
        name: 'Dedicated Doer (10H)',
        description: 'Accumulate 10 hours of focus time.',
        iconName: 'hourglass_bottom_outlined',
        milestoneType: MilestoneType.totalFocusHours,
        milestoneValue: 600, // 10 hours
      ),
      Achievement(
        id: 'focus_hours_25',
        name: 'Marathoner Mind (25H)',
        description: 'Accumulate 25 hours of focus time.',
        iconName: 'hourglass_full_outlined',
        milestoneType: MilestoneType.totalFocusHours,
        milestoneValue: 1500, // 25 hours
      ),

      // Tasks Completed
      Achievement(
        id: 'tasks_completed_1',
        name: 'Task Tackler',
        description: 'Completed 1 task.',
        iconName: 'check_box_outlined',
        milestoneType: MilestoneType.tasksCompleted,
        milestoneValue: 1,
      ),
      Achievement(
        id: 'tasks_completed_10',
        name: 'List Conqueror',
        description: 'Completed 10 tasks.',
        iconName: 'checklist_rtl_outlined',
        milestoneType: MilestoneType.tasksCompleted,
        milestoneValue: 10,
      ),
      Achievement(
        id: 'tasks_completed_25',
        name: 'Productivity Pro',
        description: 'Completed 25 tasks.',
        iconName: 'fact_check_outlined',
        milestoneType: MilestoneType.tasksCompleted,
        milestoneValue: 25,
      ),

      // Daily Streaks
      Achievement(
        id: 'streak_3_days',
        name: 'Streak Starter (3D)',
        description: 'Maintained a 3-day focus streak.',
        iconName: 'whatshot_outlined',
        milestoneType: MilestoneType.dailyStreakReached,
        milestoneValue: 3,
      ),
      Achievement(
        id: 'streak_7_days',
        name: 'Weekly Warrior (7D)',
        description: 'Maintained a 7-day focus streak.',
        iconName: 'local_fire_department_outlined',
        milestoneType: MilestoneType.dailyStreakReached,
        milestoneValue: 7,
      ),
      Achievement(
        id: 'streak_14_days',
        name: 'Consistent Committer (14D)',
        description: 'Maintained a 14-day focus streak.',
        iconName: 'fireplace_outlined',
        milestoneType: MilestoneType.dailyStreakReached,
        milestoneValue: 14,
      ),
      Achievement(
        id: 'streak_30_days',
        name: 'Steadfast Sage (30D)',
        description: 'Maintained a 30-day focus streak.',
        iconName: 'celebration_outlined',
        milestoneType: MilestoneType.dailyStreakReached,
        milestoneValue: 30,
      ),
    ];
  }

  Future<void> _loadAchievementsStatus() async {
    final prefs = await SharedPreferences.getInstance();
    final String? achievementsJson = prefs.getString(_achievementsKey);

    if (achievementsJson != null) {
      try {
        final List<dynamic> decodedList = jsonDecode(achievementsJson);
        final Map<String, dynamic> unlockedData = { 
          for (var item in decodedList.cast<Map<String, dynamic>>()) item['id']: item
        };

        for (var achievement in _achievements) {
          if (unlockedData.containsKey(achievement.id)) {
            achievement.isUnlocked = unlockedData[achievement.id]['isUnlocked'] ?? false;
            achievement.unlockedAt = unlockedData[achievement.id]['unlockedAt'] != null
                ? DateTime.fromMillisecondsSinceEpoch(unlockedData[achievement.id]['unlockedAt'])
                : null;
          }
        }
      } catch (e) {
        print('Error loading achievements status: $e');
        // Could reset to default if format is corrupted
      }
    }

    notifyListeners();
  }

  Future<void> _saveAchievementsStatus() async {
    final prefs = await SharedPreferences.getInstance();
    // Save only essential data for unlocked achievements to avoid overwriting definitions
    final List<Map<String, dynamic>> unlockedAchievementsData = _achievements
        .where((a) => a.isUnlocked)
        .map((a) => {
              'id': a.id,
              'isUnlocked': a.isUnlocked,
              'unlockedAt': a.unlockedAt?.millisecondsSinceEpoch,
            })
        .toList();
    final String achievementsJson = jsonEncode(unlockedAchievementsData);
    await prefs.setString(_achievementsKey, achievementsJson);
  }

  void unlockAchievement(String achievementId, {bool forceUnlock = false}) {
    final achievement = _achievements.firstWhere((a) => a.id == achievementId, orElse: () => throw Exception('Achievement $achievementId not found'));
    
    if (!achievement.isUnlocked || forceUnlock) {
      achievement.isUnlocked = true;
      achievement.unlockedAt = DateTime.now();
      print('Achievement Unlocked: ${achievement.name}');
      _saveAchievementsStatus();
      notifyListeners();
      // TODO: Show a notification/toast to the user about the new achievement
    }
  }

  void checkAndUnlockAchievements(MilestoneType type, int currentValue) {
    final relevantAchievements = _achievements.where(
      (a) => a.milestoneType == type && !a.isUnlocked && currentValue >= a.milestoneValue
    ).toList();
    
    bool anyUnlocked = false;
    for (var achievement in relevantAchievements) {
      // unlockAchievement already handles the check for !isUnlocked, but this outer check is fine
      if (!achievement.isUnlocked) { // Double check to be safe
          achievement.isUnlocked = true;
          achievement.unlockedAt = DateTime.now();
          print('Achievement Unlocked: ${achievement.name}');
          // TODO: Show a notification/toast to the user about the new achievement
          anyUnlocked = true;
      }
    }
    if (anyUnlocked) {
        _saveAchievementsStatus(); // Save if any new achievement was unlocked
        notifyListeners();
    }
  }
  
  void resetAllAchievements() {
    for (var achievement in _achievements) {
      achievement.isUnlocked = false;
      achievement.unlockedAt = null;
    }
    _saveAchievementsStatus();
    notifyListeners();
    print('All achievements have been reset.');
  }
}
