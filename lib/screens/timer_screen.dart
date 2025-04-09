import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:focus_flow/providers/timer_provider.dart';
import 'package:focus_flow/providers/session_provider.dart';
import 'package:focus_flow/providers/settings_provider.dart';
import 'package:focus_flow/providers/stats_provider.dart';
import 'package:focus_flow/providers/garden_provider.dart';
import 'package:focus_flow/models/session_model.dart';
import 'package:percent_indicator/circular_percent_indicator.dart';
import 'package:focus_flow/services/notification_service.dart';

class TimerScreen extends StatefulWidget {
  const TimerScreen({super.key});

  @override
  State<TimerScreen> createState() => _TimerScreenState();
}

class _TimerScreenState extends State<TimerScreen> with WidgetsBindingObserver {
  String? _currentSessionId;
  
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _initializeTimer();
  }
  
  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }
  
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused) {
      // App went to background, show notification if timer is running
      _showTimerNotificationIfNeeded();
    } else if (state == AppLifecycleState.resumed) {
      // App came to foreground, cancel any timer notifications
      NotificationService().cancelNotification(1);
    }
  }
  
  void _initializeTimer() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final timerProvider = Provider.of<TimerProvider>(context, listen: false);
      final settingsProvider = Provider.of<SettingsProvider>(context, listen: false);
      
      if (!timerProvider.isActive) {
        timerProvider.initializeTimer(
          settingsProvider.settings.focusSessionMinutes,
          SessionType.focus,
        );
      }
    });
  }
  
  void _showTimerNotificationIfNeeded() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    final settingsProvider = Provider.of<SettingsProvider>(context, listen: false);
    
    if (timerProvider.isActive && !timerProvider.isPaused && settingsProvider.settings.enableNotifications) {
      final remainingMinutes = timerProvider.remainingTime.inMinutes;
      final remainingSeconds = timerProvider.remainingTime.inSeconds % 60;
      
      NotificationService().showTimerNotification(
        id: 1,
        title: 'Focus Timer Running',
        body: 'Remaining: $remainingMinutes:${remainingSeconds.toString().padLeft(2, '0')}',
      );
    }
  }
  
  void _handleTimerCompletion() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    final sessionProvider = Provider.of<SessionProvider>(context, listen: false);
    final statsProvider = Provider.of<StatsProvider>(context, listen: false);
    final gardenProvider = Provider.of<GardenProvider>(context, listen: false);
    final settingsProvider = Provider.of<SettingsProvider>(context, listen: false);
    
    // Complete the current session
    if (_currentSessionId != null) {
      sessionProvider.completeSession(_currentSessionId!);
      
      // Add stats only if it was a focus session
      if (timerProvider.currentSessionType == SessionType.focus) {
        final actualMinutes = timerProvider.duration.inMinutes;
        statsProvider.addFocusMinutes(actualMinutes);
        gardenProvider.addFocusPoints(actualMinutes);
        
        // Show completion notification
        if (settingsProvider.settings.enableNotifications) {
          NotificationService().showMotivationNotification(
            id: 2,
            title: 'Focus Session Completed!',
            body: 'Great job! You completed a ${actualMinutes}-minute focus session.',
          );
        }
      }
    }
    
    // Determine next session type
    final nextSessionType = timerProvider.getNextSessionType();
    int nextDuration;
    
    if (nextSessionType == SessionType.focus) {
      nextDuration = settingsProvider.settings.focusSessionMinutes;
    } else if (nextSessionType == SessionType.shortBreak) {
      nextDuration = settingsProvider.settings.shortBreakMinutes;
    } else {
      nextDuration = settingsProvider.settings.longBreakMinutes;
    }
    
    // Initialize next session
    timerProvider.initializeTimer(nextDuration, nextSessionType);
    
    // If auto-start is enabled for the appropriate session type, start the next timer
    final settings = settingsProvider.settings;
    final shouldAutoStart = 
      (nextSessionType == SessionType.focus && settings.autoStartFocus) ||
      (nextSessionType != SessionType.focus && settings.autoStartBreaks);
      
    if (shouldAutoStart) {
      _startTimer();
    }
  }
  
  void _startTimer() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    final sessionProvider = Provider.of<SessionProvider>(context, listen: false);
    
    if (!timerProvider.isActive || timerProvider.isPaused) {
      if (!timerProvider.isActive) {
        // Create a new session
        final session = sessionProvider.createSession(
          durationMinutes: timerProvider.duration.inMinutes,
          type: timerProvider.currentSessionType,
        );
        _currentSessionId = session.id;
      }
      
      timerProvider.startTimer();
    }
  }
  
  void _pauseTimer() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    if (timerProvider.isActive && !timerProvider.isPaused) {
      timerProvider.pauseTimer();
      NotificationService().cancelNotification(1);
    }
  }
  
  void _resetTimer() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    timerProvider.resetTimer();
    _currentSessionId = null;
    NotificationService().cancelNotification(1);
  }
  
  void _skipSession() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    final sessionProvider = Provider.of<SessionProvider>(context, listen: false);
    final settingsProvider = Provider.of<SettingsProvider>(context, listen: false);
    
    // Cancel the current session if one exists
    if (_currentSessionId != null) {
      sessionProvider.deleteSession(_currentSessionId!);
      _currentSessionId = null;
    }
    
    // Determine next session type
    final nextSessionType = timerProvider.getNextSessionType();
    int nextDuration;
    
    if (nextSessionType == SessionType.focus) {
      nextDuration = settingsProvider.settings.focusSessionMinutes;
    } else if (nextSessionType == SessionType.shortBreak) {
      nextDuration = settingsProvider.settings.shortBreakMinutes;
    } else {
      nextDuration = settingsProvider.settings.longBreakMinutes;
    }
    
    // Initialize next session
    timerProvider.initializeTimer(nextDuration, nextSessionType);
    
    NotificationService().cancelNotification(1);
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<TimerProvider>(
      builder: (context, timerProvider, child) {
        final minutes = timerProvider.remainingTime.inMinutes;
        final seconds = timerProvider.remainingTime.inSeconds % 60;
        final formattedTime = '$minutes:${seconds.toString().padLeft(2, '0')}';
        
        return Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Session type indicator
              Text(
                _getSessionTypeText(timerProvider.currentSessionType),
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: _getSessionTypeColor(timerProvider.currentSessionType, context),
                ),
              ),
              const SizedBox(height: 30),
              
              // Timer display
              CircularPercentIndicator(
                radius: 140,
                lineWidth: 15.0,
                percent: timerProvider.progressPercentage.clamp(0.0, 1.0),
                center: Text(
                  formattedTime,
                  style: const TextStyle(
                    fontSize: 60,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                progressColor: _getSessionTypeColor(timerProvider.currentSessionType, context),
                backgroundColor: Colors.grey.shade200,
                circularStrokeCap: CircularStrokeCap.round,
                animation: true,
                animateFromLastPercent: true,
              ),
              const SizedBox(height: 50),
              
              // Control buttons
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Reset button
                  IconButton(
                    icon: const Icon(Icons.refresh),
                    iconSize: 32,
                    onPressed: _resetTimer,
                  ),
                  const SizedBox(width: 32),
                  
                  // Play/Pause button
                  FloatingActionButton(
                    backgroundColor: _getSessionTypeColor(timerProvider.currentSessionType, context),
                    foregroundColor: Colors.white,
                    onPressed: timerProvider.isActive && !timerProvider.isPaused 
                        ? _pauseTimer 
                        : _startTimer,
                    child: Icon(
                      timerProvider.isActive && !timerProvider.isPaused
                          ? Icons.pause
                          : Icons.play_arrow,
                      size: 40,
                    ),
                  ),
                  const SizedBox(width: 32),
                  
                  // Skip button
                  IconButton(
                    icon: const Icon(Icons.skip_next),
                    iconSize: 32,
                    onPressed: _skipSession,
                  ),
                ],
              ),
              
              const SizedBox(height: 40),
              
              // Session progress
              if (timerProvider.isActive || timerProvider.isCompleted)
                Consumer<SessionProvider>(
                  builder: (context, sessionProvider, child) {
                    final totalSessions = timerProvider.completedSessions;
                    final sessionText = totalSessions == 1 
                        ? '1 session completed today' 
                        : '$totalSessions sessions completed today';
                    
                    return Text(
                      sessionText,
                      style: const TextStyle(
                        fontSize: 16,
                        color: Colors.grey,
                      ),
                    );
                  },
                ),
              
              // Timer completed view
              if (timerProvider.isCompleted)
                Padding(
                  padding: const EdgeInsets.only(top: 20.0),
                  child: ElevatedButton(
                    onPressed: () {
                      _handleTimerCompletion();
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _getSessionTypeColor(
                        timerProvider.getNextSessionType(), 
                        context
                      ),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(
                        horizontal: 24, 
                        vertical: 12
                      ),
                    ),
                    child: Text(
                      timerProvider.getNextSessionType() == SessionType.focus
                          ? 'Start Focus Session'
                          : timerProvider.getNextSessionType() == SessionType.shortBreak
                              ? 'Start Short Break'
                              : 'Start Long Break',
                      style: const TextStyle(fontSize: 16),
                    ),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
  
  String _getSessionTypeText(SessionType type) {
    switch (type) {
      case SessionType.focus:
        return 'Focus Session';
      case SessionType.shortBreak:
        return 'Short Break';
      case SessionType.longBreak:
        return 'Long Break';
      default:
        return 'Focus Session';
    }
  }
  
  Color _getSessionTypeColor(SessionType type, BuildContext context) {
    switch (type) {
      case SessionType.focus:
        return Theme.of(context).colorScheme.primary;
      case SessionType.shortBreak:
        return Colors.orange;
      case SessionType.longBreak:
        return Colors.green;
      default:
        return Theme.of(context).colorScheme.primary;
    }
  }
}
