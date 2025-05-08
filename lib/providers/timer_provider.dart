import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:focus_flow/models/session_model.dart';
import 'package:focus_flow/models/scheduled_session_model.dart';
import 'package:focus_flow/providers/settings_provider.dart';
import 'package:focus_flow/providers/ambient_sound_provider.dart';
import 'package:focus_flow/providers/task_provider.dart'; 
import 'package:focus_flow/providers/stats_provider.dart'; 
import 'package:focus_flow/models/session_log_model.dart';
import 'package:focus_flow/models/task_model.dart'; 
import 'package:focus_flow/services/notification_service.dart';

class TimerProvider with ChangeNotifier {
  SettingsProvider _settingsProvider;
  AmbientSoundProvider _ambientSoundProvider;
  TaskProvider _taskProvider; 
  StatsProvider _statsProvider; 
  NotificationService _notificationService;
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
  String? _sessionName;
  String? _linkedTaskId; 
  int _currentCustomCycleIndex = 0; 
  int _continuousFocusTimeInMinutes = 0;

  // New state for suggested task
  Task? _suggestedNextTask;
  Task? get suggestedNextTask => _suggestedNextTask;

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
  String? get sessionName => _sessionName;
  String? get linkedTaskId => _linkedTaskId; 
  
  double get progressPercentage {
    if (_duration.inSeconds == 0) return 0;
    return 1 - (_remainingTime.inSeconds / _duration.inSeconds);
  }

  TimerProvider(
    this._settingsProvider, 
    this._ambientSoundProvider, 
    this._taskProvider, 
    this._statsProvider,
    this._notificationService
  ) { 
    // Initialize with default focus session settings
    initializeTimer(_settingsProvider.settings.focusSessionMinutes, SessionType.focus);
  }

  void updateProviders(
    SettingsProvider settingsProvider, 
    AmbientSoundProvider ambientSoundProvider,
    TaskProvider taskProvider, 
    StatsProvider statsProvider, 
    NotificationService notificationService
  ) {
    bool settingsValuesChanged = _settingsProvider.settings != settingsProvider.settings;
    _settingsProvider = settingsProvider;
    _ambientSoundProvider = ambientSoundProvider;
    _taskProvider = taskProvider; 
    _statsProvider = statsProvider; 
    _notificationService = notificationService;

    if (settingsValuesChanged && !_isActive) {
      // Re-initialize with the current session type but potentially new duration from settings
      int newDuration;
      switch (_currentSessionType) {
        case SessionType.focus:
          newDuration = _settingsProvider.settings.focusSessionMinutes;
          break;
        case SessionType.shortBreak:
          newDuration = _settingsProvider.settings.shortBreakMinutes;
          break;
        case SessionType.longBreak:
          newDuration = _settingsProvider.settings.longBreakMinutes;
          break;
      }
      initializeTimer(newDuration, _currentSessionType, name: _sessionName);
    }
  }

  // Initialize timer with duration and optional name
  void initializeTimer(int minutes, SessionType sessionType, {String? name}) {
    _duration = Duration(minutes: minutes);
    _remainingTime = Duration(minutes: minutes);
    _currentSessionType = sessionType;
    _sessionName = name;
    notifyListeners();
  }

  // New method to initialize from ScheduledSession
  void initializeFromScheduledSession(ScheduledSession scheduledSession) {
    initializeTimer(
      scheduledSession.plannedDurationMinutes,
      scheduledSession.sessionType,
      name: scheduledSession.name,
    );
  }

  // Start timer
  void startTimer() {
    if (_isActive && !_isPaused) return;
    
    // Reset continuous focus time if a break session is starting
    if (_currentSessionType == SessionType.shortBreak || _currentSessionType == SessionType.longBreak) {
      _continuousFocusTimeInMinutes = 0;
      debugPrint('[TimerProvider] Continuous focus time reset: Break session starting.');
    }
    
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
    _ambientSoundProvider?.updatePlaybackBasedOnSession(_currentSessionType.name, true);
    notifyListeners();
  }

  // Pause timer
  void pauseTimer() {
    if (!_isActive || _isPaused || _isCompleted) return;
    
    _timer?.cancel();
    _isPaused = true;
    _ambientSoundProvider?.updatePlaybackBasedOnSession(_currentSessionType.name, false);
    notifyListeners();
  }

  // Reset timer
  void resetTimer() {
    _timer?.cancel();
    _continuousFocusTimeInMinutes = 0; // Reset continuous focus time
    debugPrint('[TimerProvider] Continuous focus time reset: Timer manually reset.');

    // Log aborted session if it was active
    if (_isActive && _startTime != null) {
      final endTime = DateTime.now();
      final actualDuration = endTime.difference(_startTime!).inMinutes;
      // Ensure duration is not negative if reset happens very quickly
      final loggedDuration = actualDuration > 0 ? actualDuration : 0; 
      _logSessionActivity(_startTime!, endTime, loggedDuration, _currentSessionType, completedSuccessfully: false);
    }

    _remainingTime = _duration; 
    _isActive = false;
    _isPaused = false;
    _isCompleted = false;
    _startTime = null;
    _endTime = null;
    _sessionName = null;
    _linkedTaskId = null; 
    _currentCustomCycleIndex = 0; 
    _ambientSoundProvider?.updatePlaybackBasedOnSession(_currentSessionType.name, false);
    notifyListeners();
  }

  // Complete timer
  Future<void> completeTimer() async {
    if (!_isActive || _isPaused) return;

    _timer?.cancel();
    _isActive = false;
    _isCompleted = true;
    _endTime = DateTime.now();

    // Calculate actual duration in case of early completion or system discrepancies
    final actualDuration = _endTime!.difference(_startTime!).inMinutes;
    // Ensure logged duration isn't negative if something went wrong with times
    final loggedDuration = actualDuration > 0 ? actualDuration : _duration.inMinutes; 

    // Log session completion for stats
    _logSessionActivity(_startTime!, _endTime!, loggedDuration, _currentSessionType);

    if (_currentSessionType == SessionType.focus) {
      _continuousFocusTimeInMinutes += loggedDuration;
      debugPrint('[TimerProvider] Continuous focus time updated: $_continuousFocusTimeInMinutes minutes.');
      
      // Increment completed pomodoros for the linked task if any
      if (_linkedTaskId != null) {
        _taskProvider.incrementCompletedPomodoros(_linkedTaskId!);
        debugPrint('[TimerProvider] Pomodoro completed for task: $_linkedTaskId');
      }

      // Handle Adaptive Break Reminder
      if (_settingsProvider.settings.enableAdaptiveBreakReminder && 
          _continuousFocusTimeInMinutes >= _settingsProvider.settings.adaptiveBreakReminderThresholdMinutes) {
        debugPrint('[TimerProvider] Adaptive break threshold reached. Scheduling reminder.');
        if (_settingsProvider.settings.enableNotifications) {
          _notificationService.showMotivationNotification(
            id: 1001, // Unique ID for adaptive break reminder
            title: 'Time for a Break?',
            body: 'You\'ve been focusing for $_continuousFocusTimeInMinutes minutes. Consider taking a short break!'
          );
        }
        _continuousFocusTimeInMinutes = 0; // Reset after showing reminder
      }
    } else {
      // Reset continuous focus time when a break session completes
      _continuousFocusTimeInMinutes = 0;
      debugPrint('[TimerProvider] Continuous focus time reset after break completion.');
    }

    // --- Next Task Suggestion Logic ---
    if (_settingsProvider.settings.enableNextTaskSuggestion && 
        _currentSessionType == SessionType.focus && 
        _linkedTaskId != null) {
      final completedTask = _taskProvider.getTaskById(_linkedTaskId!);
      if (completedTask != null && completedTask.status == TaskStatus.completed) {
        debugPrint('[TimerProvider] Suggesting next task after completion of: ${completedTask.name}');
        _suggestedNextTask = _taskProvider.getNextSuggestedTask(currentProjectId: null);
        if (_suggestedNextTask != null) {
          debugPrint('[TimerProvider] Suggested next task: ${_suggestedNextTask!.name}');
        } else {
          debugPrint('[TimerProvider] No next task suggested.');
        }
      } else {
        // Task not marked as completed, or not found, so don't suggest
        _suggestedNextTask = null; 
      }
    } else {
      _suggestedNextTask = null; // Clear if suggestion not applicable
    }
    // --- End Next Task Suggestion Logic ---

    // Determine next session type and duration
    SessionType nextSessionType;
    int nextDuration;

    if (_settingsProvider != null) {
      final settings = _settingsProvider.settings;
      if (settings.useCustomCycle && settings.customCycleSegments.isNotEmpty) {
        _currentCustomCycleIndex++;
        if (_currentCustomCycleIndex >= settings.customCycleSegments.length) {
          _currentCustomCycleIndex = 0; 
        }
        final nextSegment = settings.customCycleSegments[_currentCustomCycleIndex];
        nextSessionType = nextSegment.type;
        nextDuration = nextSegment.durationMinutes;
      } else {
        nextSessionType = getNextSessionType(); 
        if (nextSessionType == SessionType.focus) {
          nextDuration = settings.focusSessionMinutes;
        } else if (nextSessionType == SessionType.shortBreak) {
          nextDuration = settings.shortBreakMinutes;
        } else { 
          nextDuration = settings.longBreakMinutes;
        }
      }
      
      bool shouldAutoStart = false;
      if (_currentSessionType == SessionType.focus && settings.autoStartBreaks) { 
        shouldAutoStart = true;
      } else if (_currentSessionType != SessionType.focus && settings.autoStartFocus) { 
        shouldAutoStart = true;
      }

      if (shouldAutoStart) {
        _timer?.cancel(); 
        _isActive = false;
        _isPaused = false;
        _isCompleted = false; 
        _startTime = null;
        _endTime = null;

        initializeTimer(nextDuration, nextSessionType);
        startTimer();
      }
    }
  }

  // Skip the current session and move to the next
  void skipSession() {
    _timer?.cancel();

    // Log skipped session if it was active
    if (_isActive && _startTime != null) {
      final endTime = DateTime.now();
      final actualDuration = endTime.difference(_startTime!).inMinutes;
      final loggedDuration = actualDuration > 0 ? actualDuration : 0;
      _logSessionActivity(_startTime!, endTime, loggedDuration, _currentSessionType, completedSuccessfully: false);
    }

    _isActive = false;
    _isPaused = false;
    _isCompleted = false; 
    _startTime = null;
    _endTime = null;

    notifyListeners(); 

    if (_settingsProvider != null) {
      final settings = _settingsProvider.settings;
      SessionType nextSessionType;
      int nextDuration;

      if (settings.useCustomCycle && settings.customCycleSegments.isNotEmpty) {
        _currentCustomCycleIndex++;
        if (_currentCustomCycleIndex >= settings.customCycleSegments.length) {
          _currentCustomCycleIndex = 0; 
        }
        final nextSegment = settings.customCycleSegments[_currentCustomCycleIndex];
        nextSessionType = nextSegment.type;
        nextDuration = nextSegment.durationMinutes;
      } else {
        nextSessionType = getNextSessionType(); 
        if (nextSessionType == SessionType.focus) {
          nextDuration = settings.focusSessionMinutes;
        } else if (nextSessionType == SessionType.shortBreak) {
          nextDuration = settings.shortBreakMinutes;
        } else { 
          nextDuration = settings.longBreakMinutes;
        }
      }

      bool autoStartingNext = false;
      if (_currentSessionType == SessionType.focus && settings.autoStartBreaks) {
        autoStartingNext = true;
      } else if ((_currentSessionType == SessionType.shortBreak || _currentSessionType == SessionType.longBreak) && settings.autoStartFocus) {
        autoStartingNext = true;
      }

      initializeTimer(nextDuration, nextSessionType); 

      if (autoStartingNext) {
        startTimer(); 
      }
    } else {
      initializeTimer(25, SessionType.focus); 
    }
  }

  // New: Method to link or unlink a task from the current/next session
  void linkTaskToSession(String? taskId) {
    _linkedTaskId = taskId;
    // If a task is manually linked, clear any existing suggestion
    if (taskId != null && _suggestedNextTask != null) {
      _suggestedNextTask = null;
    }
    notifyListeners();
  }

  // New methods for managing suggested task
  void linkSuggestedTaskToNextSession() {
    if (_suggestedNextTask != null) {
      _linkedTaskId = _suggestedNextTask!.id;
      _suggestedNextTask = null; // Clear suggestion after linking
      notifyListeners();
      debugPrint('[TimerProvider] Linked suggested task: $_linkedTaskId');
    }
  }

  void clearSuggestedTask() {
    if (_suggestedNextTask != null) {
      _suggestedNextTask = null;
      notifyListeners();
      debugPrint('[TimerProvider] Cleared suggested task.');
    }
  }

  // Helper function to map TimerProvider's SessionType to SessionLogType
  SessionLogType _mapSessionTypeToLogType(SessionType type) {
    switch (type) {
      case SessionType.focus:
        return SessionLogType.focus;
      case SessionType.shortBreak:
        return SessionLogType.shortBreak;
      case SessionType.longBreak:
        return SessionLogType.longBreak;
      default:
        return SessionLogType.focus; // Should not happen
    }
  }

  // Helper function to log session activity
  Future<void> _logSessionActivity(DateTime startTime, DateTime endTime, int durationMinutes, SessionType sessionType, {bool completedSuccessfully = true}) async {
    if (_statsProvider != null) {
      final log = SessionLog(
        id: DateTime.now().millisecondsSinceEpoch.toString(),
        startTime: startTime,
        endTime: endTime,
        durationMinutes: durationMinutes,
        sessionType: _mapSessionTypeToLogType(sessionType),
      );
      await _statsProvider.addSessionLog(log);
      // If it was a focus session and completed successfully, also call existing stats updates
      if (sessionType == SessionType.focus && completedSuccessfully) {
         _statsProvider.addFocusMinutes(durationMinutes);
         _completedSessions++; // Increment completed focus sessions for cycle logic
      }
    }
  }

  // Get the next session type (Focus → Short Break → Focus → Short Break → ... → Long Break)
  SessionType getNextSessionType() {
    if (_currentSessionType == SessionType.focus) {
      int sessionsBeforeLong = _settingsProvider?.settings.sessionsBeforeLongBreak ?? 4;
      if (sessionsBeforeLong <= 0) sessionsBeforeLong = 4; 
      
      if (_completedSessions > 0 && _completedSessions % sessionsBeforeLong == 0) {
        return SessionType.longBreak;
      } else {
        return SessionType.shortBreak;
      }
    } else {
      return SessionType.focus;
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
