import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:focus_flow/models/session_model.dart';
import 'package:uuid/uuid.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';

class SessionProvider with ChangeNotifier {
  List<FocusSession> _sessions = [];
  final Uuid _uuid = const Uuid();
  
  // Getters
  List<FocusSession> get sessions => _sessions;
  List<FocusSession> get completedSessions => _sessions.where((s) => s.completed).toList();
  List<FocusSession> get recentSessions => _sessions.take(5).toList();
  
  // Get today's sessions
  List<FocusSession> getTodaySessions() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return _sessions.where((session) {
      final sessionDate = DateTime(
        session.startTime.year, 
        session.startTime.month, 
        session.startTime.day
      );
      return sessionDate.isAtSameMomentAs(today);
    }).toList();
  }
  
  // Get total focus time for today in minutes
  int getTodayFocusTime() {
    final todaySessions = getTodaySessions();
    return todaySessions.fold<int>(
      0, 
      (sum, session) => sum + (session.completed ? session.actualDurationMinutes : 0)
    );
  }
  
  // Create a new session
  FocusSession createSession({
    required int durationMinutes,
    required SessionType type,
  }) {
    final session = FocusSession(
      id: _uuid.v4(),
      startTime: DateTime.now(),
      durationMinutes: durationMinutes,
      type: type,
    );
    
    _sessions.insert(0, session);
    _saveSessions();
    notifyListeners();
    return session;
  }
  
  // Complete a session
  void completeSession(String sessionId) {
    final index = _sessions.indexWhere((s) => s.id == sessionId);
    if (index != -1) {
      _sessions[index] = _sessions[index].copyWith(
        endTime: DateTime.now(),
        completed: true,
      );
      _saveSessions();
      notifyListeners();
    }
  }
  
  // Delete a session
  void deleteSession(String sessionId) {
    _sessions.removeWhere((s) => s.id == sessionId);
    _saveSessions();
    notifyListeners();
  }
  
  // Load sessions from storage
  Future<void> loadSessions() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final sessionsJson = prefs.getStringList('sessions') ?? [];
      
      _sessions = sessionsJson
          .map((json) => FocusSession.fromMap(jsonDecode(json)))
          .toList();
      
      // Sort sessions by start time (newest first)
      _sessions.sort((a, b) => b.startTime.compareTo(a.startTime));
      
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading sessions: $e');
    }
  }
  
  // Save sessions to storage
  Future<void> _saveSessions() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final sessionsJson = _sessions
          .map((session) => jsonEncode(session.toMap()))
          .toList();
      
      await prefs.setStringList('sessions', sessionsJson);
    } catch (e) {
      debugPrint('Error saving sessions: $e');
    }
  }
}
