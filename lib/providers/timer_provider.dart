import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:focus_flow/models/session_model.dart';

class TimerProvider with ChangeNotifier {
  Timer? _timer;
  Duration _duration = const Duration(minutes: 25);
  Duration _remainingTime = const Duration(minutes: 25);
  DateTime? _startTime;
  DateTime? _endTime;
  bool _isActive = false;
  bool _isPaused = false;
  bool _isCompleted = false;
  SessionType _currentSessionType = SessionType.focus;
  int _completedSessions = 0;

  // Getters
  Duration get duration => _duration;
  Duration get remainingTime => _remainingTime;
  DateTime? get startTime => _startTime;
  DateTime? get endTime => _endTime;
  bool get isActive => _isActive;
  bool get isPaused => _isPaused;
  bool get isCompleted => _isCompleted;
  SessionType get currentSessionType => _currentSessionType;
  int get completedSessions => _completedSessions;
  
  double get progressPercentage {
    if (_duration.inSeconds == 0) return 0;
    return 1 - (_remainingTime.inSeconds / _duration.inSeconds);
  }

  // Initialize timer with duration
  void initializeTimer(int minutes, SessionType sessionType) {
    _duration = Duration(minutes: minutes);
    _remainingTime = Duration(minutes: minutes);
    _currentSessionType = sessionType;
    notifyListeners();
  }

  // Start timer
  void startTimer() {
    if (_isActive && !_isPaused) return;
    
    if (!_isActive) {
      _startTime = DateTime.now();
      _isActive = true;
      _isPaused = false;
      _isCompleted = false;
    } else {
      _isPaused = false;
    }
    
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_remainingTime.inSeconds > 0) {
        _remainingTime = _remainingTime - const Duration(seconds: 1);
        notifyListeners();
      } else {
        completeTimer();
      }
    });
    
    notifyListeners();
  }

  // Pause timer
  void pauseTimer() {
    if (!_isActive || _isPaused || _isCompleted) return;
    
    _timer?.cancel();
    _isPaused = true;
    notifyListeners();
  }

  // Reset timer
  void resetTimer() {
    _timer?.cancel();
    _remainingTime = _duration;
    _isActive = false;
    _isPaused = false;
    _isCompleted = false;
    _startTime = null;
    _endTime = null;
    notifyListeners();
  }

  // Complete timer
  void completeTimer() {
    _timer?.cancel();
    _endTime = DateTime.now();
    _isActive = false;
    _isPaused = false;
    _isCompleted = true;
    
    if (_currentSessionType == SessionType.focus) {
      _completedSessions++;
    }
    
    notifyListeners();
  }

  // Skip the current session and move to the next
  void skipSession() {
    _timer?.cancel();
    _isActive = false;
    _isPaused = false;
    _isCompleted = false;
    _startTime = null;
    _endTime = null;
    notifyListeners();
  }

  // Get the next session type (Focus → Short Break → Focus → Short Break → ... → Long Break)
  SessionType getNextSessionType() {
    if (_currentSessionType == SessionType.focus) {
      // After focus, decide if it should be short break or long break
      if (_completedSessions % 4 == 0) {
        return SessionType.longBreak;
      } else {
        return SessionType.shortBreak;
      }
    } else {
      // After any break, next is always focus
      return SessionType.focus;
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
