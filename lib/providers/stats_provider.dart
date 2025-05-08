import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:intl/intl.dart';
import 'dart:convert';
import 'package:focus_flow/providers/achievement_provider.dart';
import 'package:focus_flow/models/achievement_model.dart';
import 'package:focus_flow/models/session_log_model.dart';
import 'package:csv/csv.dart';
import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

class StatsProvider with ChangeNotifier {
  int _currentStreak = 0;
  int _longestStreak = 0;
  int _totalFocusMinutes = 0;
  int _totalSessions = 0;
  int _totalTasksCompleted = 0;
  Map<String, int> _dailyFocusMinutes = {}; // Keeps historical daily data
  String _lastActiveDate = ''; // For streak calculation

  // New fields for goal tracking
  int _focusedMinutesToday = 0;
  String _lastDateForDailyGoal = ''; // YYYY-MM-DD
  int _focusedMinutesThisWeek = 0;
  String _lastMondayForWeeklyGoal = ''; // YYYY-MM-DD of the week's Monday

  List<SessionLog> _sessionLogs = []; // List to store session logs

  // Getters
  int get currentStreak => _currentStreak;
  int get longestStreak => _longestStreak;
  int get totalFocusMinutes => _totalFocusMinutes;
  int get totalSessions => _totalSessions;
  int get totalTasksCompleted => _totalTasksCompleted;
  Map<String, int> get dailyFocusMinutesHistorcial => _dailyFocusMinutes; // Renamed for clarity
  String get lastActiveDate => _lastActiveDate;

  // Getters for goal progress
  int get focusedMinutesToday => _focusedMinutesToday;
  int get focusedMinutesThisWeek => _focusedMinutesThisWeek;

  List<SessionLog> get sessionLogs => List.unmodifiable(_sessionLogs); // Getter for session logs

  AchievementProvider? _achievementProvider;

  void setAchievementProvider(AchievementProvider provider) {
    _achievementProvider = provider;
  }

  StatsProvider() {
    loadStats();
  }

  String _getCurrentDateString() => DateFormat('yyyy-MM-dd').format(DateTime.now());

  String _getMondayOfWeekString(DateTime date) {
    DateTime monday = date.subtract(Duration(days: date.weekday - 1));
    return DateFormat('yyyy-MM-dd').format(monday);
  }

  Future<void> _checkAndResetPeriodicStats() async {
    final now = DateTime.now();
    final todayString = _getCurrentDateString();
    final currentWeekMondayString = _getMondayOfWeekString(now);

    bool needsSave = false;

    if (_lastDateForDailyGoal != todayString) {
      _focusedMinutesToday = 0;
      _lastDateForDailyGoal = todayString;
      needsSave = true;
    }

    if (_lastMondayForWeeklyGoal != currentWeekMondayString) {
      _focusedMinutesThisWeek = 0;
      _lastMondayForWeeklyGoal = currentWeekMondayString;
      needsSave = true;
    }
    if (needsSave) {
      // Save immediately if resets happened, especially if app was closed for a while
      await _saveStats(); 
    }
  }

  // Add focus minutes for today
  Future<void> addFocusMinutes(int minutes) async {
    await _checkAndResetPeriodicStats(); // Ensure stats are for current periods

    final today = _getCurrentDateString(); // Use consistent today string
    
    _totalFocusMinutes += minutes;
    _focusedMinutesToday += minutes; // Update current day's goal progress
    _focusedMinutesThisWeek += minutes; // Update current week's goal progress
    
    // Update historical daily focus minutes map
    if (_dailyFocusMinutes.containsKey(today)) {
      _dailyFocusMinutes[today] = (_dailyFocusMinutes[today] ?? 0) + minutes;
    } else {
      _dailyFocusMinutes[today] = minutes;
    }
    
    bool streakUpdated = false;
    if (_lastActiveDate != today) {
      final yesterday = DateFormat('yyyy-MM-dd').format(
        DateTime.now().subtract(const Duration(days: 1))
      );
      
      if (_lastActiveDate == yesterday) {
        _currentStreak++;
        streakUpdated = true;
      } else if (_lastActiveDate.isNotEmpty) { // Check not empty to avoid setting streak to 1 on first ever session
        _currentStreak = 1;
        streakUpdated = true;
      } else { // First ever session
        _currentStreak = 1;
        streakUpdated = true;
      }
      
      _lastActiveDate = today;
      
      if (_currentStreak > _longestStreak) {
        _longestStreak = _currentStreak;
        _achievementProvider?.checkAndUnlockAchievements(MilestoneType.dailyStreakReached, _longestStreak);
      }
      // Always check current streak for achievements that might trigger at lower values than longest
      if (streakUpdated) {
        _achievementProvider?.checkAndUnlockAchievements(MilestoneType.dailyStreakReached, _currentStreak);
      }
    }
    
    _totalSessions++;
    _achievementProvider?.checkAndUnlockAchievements(MilestoneType.focusSessionsCompleted, _totalSessions);

    // Check for total focus hours, pass total minutes directly
    _achievementProvider?.checkAndUnlockAchievements(MilestoneType.totalFocusHours, _totalFocusMinutes);
    
    await _saveStats();
    notifyListeners();
  }

  // New method to increment tasks completed
  Future<void> incrementTasksCompleted() async {
    _totalTasksCompleted++;
    _achievementProvider?.checkAndUnlockAchievements(MilestoneType.tasksCompleted, _totalTasksCompleted);
    await _saveStats();
    notifyListeners();
  }

  // Add a new session log
  Future<void> addSessionLog(SessionLog log) async {
    _sessionLogs.add(log);
    // Optional: Limit the number of logs stored to prevent excessive memory usage
    // if (_sessionLogs.length > 1000) { // Example limit
    //   _sessionLogs.removeAt(0);
    // }
    await _saveStats();
    notifyListeners();
  }

  // Generate CSV string from session logs
  String generateSessionLogsCsv() {
    if (_sessionLogs.isEmpty) {
      return ''; // Or a message like "No session data to export."
    }

    List<List<dynamic>> rows = [];

    // Add headers
    rows.add([
      "Session ID",
      "Start Time",
      "End Time",
      "Duration (Minutes)",
      "Session Type"
    ]);

    // Date formatter for consistent output
    final DateFormat formatter = DateFormat('yyyy-MM-dd HH:mm:ss');

    // Add data rows
    for (var log in _sessionLogs) {
      rows.add([
        log.id,
        formatter.format(log.startTime.toLocal()), // Convert to local time for readability
        formatter.format(log.endTime.toLocal()),   // Convert to local time for readability
        log.durationMinutes,
        sessionLogTypeToString(log.sessionType) // Use the helper from the model
      ]);
    }

    return const ListToCsvConverter().convert(rows);
  }

  // Export session data as CSV
  Future<bool> exportSessionData() async {
    final csvData = generateSessionLogsCsv();
    if (csvData.isEmpty) {
      debugPrint('No session data to export.');
      // Optionally, provide feedback to the user via a different mechanism
      // (e.g., a toast or snackbar if called from UI)
      return false;
    }

    try {
      final directory = await getTemporaryDirectory();
      final filePath = '${directory.path}/focus_flow_session_logs_${DateFormat('yyyyMMdd_HHmmss').format(DateTime.now())}.csv';
      final file = File(filePath);

      await file.writeAsString(csvData);
      debugPrint('CSV file saved to: $filePath');

      final result = await Share.shareXFiles(
        [XFile(filePath)], 
        text: 'FocusFlow Session Logs',
        subject: 'FocusFlow Session Logs Export'
      );

      if (result.status == ShareResultStatus.success) {
        debugPrint('Successfully shared session logs.');
        return true;
      } else {
        debugPrint('Sharing failed or was cancelled: ${result.status}');
        return false;
      }
    } catch (e) {
      debugPrint('Error exporting session data: $e');
      return false;
    }
  }

  // Get focus time for a specific date from historical data
  int getFocusTimeForDate(DateTime date) {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    return _dailyFocusMinutes[dateStr] ?? 0;
  }

  // Get average daily focus time from historical data
  double getAverageDailyFocusTime() {
    if (_dailyFocusMinutes.isEmpty) return 0;
    final totalMinutes = _dailyFocusMinutes.values.fold<int>(0, (sum, mins) => sum + mins);
    return totalMinutes / _dailyFocusMinutes.length;
  }

  // Get weekly focus time (last 7 days) from historical data
  Map<String, int> getHistoricalWeeklyFocusTime() {
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
      _totalTasksCompleted = prefs.getInt('totalTasksCompleted') ?? 0;
      _lastActiveDate = prefs.getString('lastActiveDate') ?? '';
      
      // Load new goal-tracking fields
      _focusedMinutesToday = prefs.getInt('focusedMinutesToday') ?? 0;
      _lastDateForDailyGoal = prefs.getString('lastDateForDailyGoal') ?? _getCurrentDateString();
      _focusedMinutesThisWeek = prefs.getInt('focusedMinutesThisWeek') ?? 0;
      _lastMondayForWeeklyGoal = prefs.getString('lastMondayForWeeklyGoal') ?? _getMondayOfWeekString(DateTime.now());

      final dailyStatsJson = prefs.getString('dailyFocusMinutes');
      if (dailyStatsJson != null) {
        final Map<String, dynamic> decoded = jsonDecode(dailyStatsJson);
        _dailyFocusMinutes = decoded.map((key, value) => MapEntry(key, value as int));
      }

      // Load session logs
      final sessionLogsJsonList = prefs.getStringList('sessionLogsList');
      if (sessionLogsJsonList != null) {
        _sessionLogs = sessionLogsJsonList
            .map((logJson) => SessionLog.fromJson(jsonDecode(logJson)))
            .toList();
      }

      // Check if day/week rolled over since last use
      await _checkAndResetPeriodicStats(); 
      
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading stats: $e');
    } // Added await
  }

  // Save stats to shared preferences
  Future<void> _saveStats() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      await prefs.setInt('currentStreak', _currentStreak);
      await prefs.setInt('longestStreak', _longestStreak);
      await prefs.setInt('totalFocusMinutes', _totalFocusMinutes);
      await prefs.setInt('totalSessions', _totalSessions);
      await prefs.setInt('totalTasksCompleted', _totalTasksCompleted);
      await prefs.setString('lastActiveDate', _lastActiveDate);
      await prefs.setString('dailyFocusMinutes', jsonEncode(_dailyFocusMinutes));

      // Save new goal-tracking fields
      await prefs.setInt('focusedMinutesToday', _focusedMinutesToday);
      await prefs.setString('lastDateForDailyGoal', _lastDateForDailyGoal);
      await prefs.setInt('focusedMinutesThisWeek', _focusedMinutesThisWeek);
      await prefs.setString('lastMondayForWeeklyGoal', _lastMondayForWeeklyGoal);

      // Save session logs
      final sessionLogsJsonList = _sessionLogs.map((log) => jsonEncode(log.toJson())).toList();
      await prefs.setStringList('sessionLogsList', sessionLogsJsonList);

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
    _totalTasksCompleted = 0;
    _dailyFocusMinutes = {};
    _lastActiveDate = '';

    _sessionLogs = []; // Clear session logs

    // Reset new goal-tracking fields
    _focusedMinutesToday = 0;
    _lastDateForDailyGoal = _getCurrentDateString();
    _focusedMinutesThisWeek = 0;
    _lastMondayForWeeklyGoal = _getMondayOfWeekString(DateTime.now());
    
    await _saveStats();
    notifyListeners();
  }
}
