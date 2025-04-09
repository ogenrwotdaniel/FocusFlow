import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:intl/intl.dart';
import 'dart:convert';

class StatsProvider with ChangeNotifier {
  int _currentStreak = 0;
  int _longestStreak = 0;
  int _totalFocusMinutes = 0;
  int _totalSessions = 0;
  Map<String, int> _dailyFocusMinutes = {};
  String _lastActiveDate = '';

  // Getters
  int get currentStreak => _currentStreak;
  int get longestStreak => _longestStreak;
  int get totalFocusMinutes => _totalFocusMinutes;
  int get totalSessions => _totalSessions;
  Map<String, int> get dailyFocusMinutes => _dailyFocusMinutes;
  String get lastActiveDate => _lastActiveDate;

  StatsProvider() {
    loadStats();
  }

  // Add focus minutes for today
  Future<void> addFocusMinutes(int minutes) async {
    // Get today's date in yyyy-MM-dd format
    final today = DateFormat('yyyy-MM-dd').format(DateTime.now());
    
    // Update total minutes
    _totalFocusMinutes += minutes;
    
    // Update daily minutes
    if (_dailyFocusMinutes.containsKey(today)) {
      _dailyFocusMinutes[today] = (_dailyFocusMinutes[today] ?? 0) + minutes;
    } else {
      _dailyFocusMinutes[today] = minutes;
    }
    
    // Update streak if needed
    if (_lastActiveDate != today) {
      final yesterday = DateFormat('yyyy-MM-dd').format(
        DateTime.now().subtract(const Duration(days: 1))
      );
      
      if (_lastActiveDate == yesterday) {
        // Continuing streak
        _currentStreak++;
      } else if (_lastActiveDate != '') {
        // Broke streak, restart
        _currentStreak = 1;
      } else {
        // First day with data
        _currentStreak = 1;
      }
      
      _lastActiveDate = today;
      
      // Update longest streak if needed
      if (_currentStreak > _longestStreak) {
        _longestStreak = _currentStreak;
      }
    }
    
    // Increment session count
    _totalSessions++;
    
    await _saveStats();
    notifyListeners();
  }

  // Get focus time for a specific date
  int getFocusTimeForDate(DateTime date) {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    return _dailyFocusMinutes[dateStr] ?? 0;
  }

  // Get average daily focus time
  double getAverageDailyFocusTime() {
    if (_dailyFocusMinutes.isEmpty) return 0;
    final totalMinutes = _dailyFocusMinutes.values.fold<int>(0, (sum, mins) => sum + mins);
    return totalMinutes / _dailyFocusMinutes.length;
  }

  // Get weekly focus time (last 7 days)
  Map<String, int> getWeeklyFocusTime() {
    final result = <String, int>{};
    final now = DateTime.now();
    
    for (int i = 6; i >= 0; i--) {
      final date = now.subtract(Duration(days: i));
      final dateStr = DateFormat('yyyy-MM-dd').format(date);
      final dayName = DateFormat('E').format(date); // Mon, Tue, etc.
      result[dayName] = _dailyFocusMinutes[dateStr] ?? 0;
    }
    
    return result;
  }

  // Load stats from shared preferences
  Future<void> loadStats() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      _currentStreak = prefs.getInt('currentStreak') ?? 0;
      _longestStreak = prefs.getInt('longestStreak') ?? 0;
      _totalFocusMinutes = prefs.getInt('totalFocusMinutes') ?? 0;
      _totalSessions = prefs.getInt('totalSessions') ?? 0;
      _lastActiveDate = prefs.getString('lastActiveDate') ?? '';
      
      final dailyStatsJson = prefs.getString('dailyFocusMinutes');
      if (dailyStatsJson != null) {
        final Map<String, dynamic> decoded = jsonDecode(dailyStatsJson);
        _dailyFocusMinutes = decoded.map((key, value) => MapEntry(key, value as int));
      }
      
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading stats: $e');
    }
  }

  // Save stats to shared preferences
  Future<void> _saveStats() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      await prefs.setInt('currentStreak', _currentStreak);
      await prefs.setInt('longestStreak', _longestStreak);
      await prefs.setInt('totalFocusMinutes', _totalFocusMinutes);
      await prefs.setInt('totalSessions', _totalSessions);
      await prefs.setString('lastActiveDate', _lastActiveDate);
      await prefs.setString('dailyFocusMinutes', jsonEncode(_dailyFocusMinutes));
    } catch (e) {
      debugPrint('Error saving stats: $e');
    }
  }

  // Reset all stats
  Future<void> resetStats() async {
    _currentStreak = 0;
    _longestStreak = 0;
    _totalFocusMinutes = 0;
    _totalSessions = 0;
    _dailyFocusMinutes = {};
    _lastActiveDate = '';
    
    await _saveStats();
    notifyListeners();
  }
}
