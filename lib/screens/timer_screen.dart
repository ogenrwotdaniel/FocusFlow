import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:focus_flow/providers/timer_provider.dart';
import 'package:focus_flow/providers/session_provider.dart';
import 'package:focus_flow/providers/settings_provider.dart';
import 'package:focus_flow/providers/stats_provider.dart';
import 'package:focus_flow/providers/garden_provider.dart';
import 'package:focus_flow/models/session_model.dart';
import 'package:focus_flow/models/scheduled_session_model.dart';
import 'package:focus_flow/models/task_model.dart';
import 'package:focus_flow/providers/task_provider.dart';
import 'package:percent_indicator/circular_percent_indicator.dart';
import 'package:focus_flow/services/notification_service.dart';
import 'package:focus_flow/screens/settings_screen.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';
import 'package:permission_handler/permission_handler.dart';
import 'dart:async'; // For Timer
import 'package:percent_indicator/linear_percent_indicator.dart'; // New import

class TimerScreen extends StatefulWidget {
  final ScheduledSession? scheduledSession;

  const TimerScreen({super.key, this.scheduledSession});

  @override
  State<TimerScreen> createState() => _TimerScreenState();
}

class _TimerScreenState extends State<TimerScreen> with WidgetsBindingObserver {
  String? _currentSessionId;
  final _sessionNameController = TextEditingController();
  bool _isOverlayVisible = false;
  Timer? _overlayUpdateTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (widget.scheduledSession?.name != null) {
      _sessionNameController.text = widget.scheduledSession!.name!;
    }
    _initializeTimer();
    _checkOverlayStatus();

    // Listen for messages from overlay
    FlutterOverlayWindow.overlayListener.listen((data) {
      if (data is Map && data['action'] == 'getTime') {
        _sendTimeToOverlay();
      }
    });
  }

  Future<void> _checkOverlayStatus() async {
    _isOverlayVisible = await FlutterOverlayWindow.isActive() ?? false;
    if (mounted) setState(() {});
  }

  void _sendTimeToOverlay() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    if (timerProvider.isActive && !timerProvider.isPaused) {
      FlutterOverlayWindow.shareData({'remainingTime': timerProvider.remainingTime.inSeconds});
    }
  }

  void _startOverlayUpdateTimer() {
    _overlayUpdateTimer?.cancel(); // Cancel any existing timer
    _overlayUpdateTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_isOverlayVisible) {
        _sendTimeToOverlay();
      } else {
        timer.cancel(); // Stop sending updates if overlay is not visible
      }
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _sessionNameController.dispose();
    WakelockPlus.disable();
    _overlayUpdateTimer?.cancel();
    // Consider closing the overlay if the screen is disposed, or let it persist?
    // For now, let's assume it should close if the main timer screen is gone.
    // if (_isOverlayVisible) {
    //   FlutterOverlayWindow.closeOverlay();
    // }
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    if (state == AppLifecycleState.paused) {
      _showTimerNotificationIfNeeded();
      // If overlay is active, we might not need the notification, or vice-versa.
      // For now, let them both be active.
    } else if (state == AppLifecycleState.resumed) {
      NotificationService().cancelNotification(1); // Timer notification ID
      _checkOverlayStatus(); // Refresh overlay status when app resumes
    }
  }

  Future<void> _toggleOverlay() async {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    if (!timerProvider.isActive || timerProvider.isPaused) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Timer must be running to show overlay.')),
      );
      return;
    }

    if (_isOverlayVisible) {
      await FlutterOverlayWindow.closeOverlay();
      _overlayUpdateTimer?.cancel();
      setState(() => _isOverlayVisible = false);
    } else {
      if (await Permission.systemAlertWindow.request().isGranted) {
        await FlutterOverlayWindow.showOverlay(
          height: 100, 
          width: 200,
          alignment: OverlayAlignment.topCenter,
          overlayTitle: "FocusFlow Timer",
          enableDrag: true,
        );
        // Send initial time and start periodic updates
        _sendTimeToOverlay(); 
        _startOverlayUpdateTimer();
        setState(() => _isOverlayVisible = true);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Overlay permission is required to show floating timer.')),
        );
      }
    }
  }

  void _initializeTimer() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final timerProvider = Provider.of<TimerProvider>(context, listen: false);
      final settingsProvider = Provider.of<SettingsProvider>(context, listen: false);

      if (widget.scheduledSession != null) {
        timerProvider.initializeFromScheduledSession(widget.scheduledSession!);
      } else if (!timerProvider.isActive) {
        timerProvider.initializeTimer(
          settingsProvider.settings.focusSessionMinutes,
          SessionType.focus,
          name: _sessionNameController.text.isNotEmpty ? _sessionNameController.text : null,
        );
      }
    });
  }

  void _startTimer() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    final sessionProvider = Provider.of<SessionProvider>(context, listen: false);
    final settings = Provider.of<SettingsProvider>(context, listen: false).settings;

    WakelockPlus.enable(); 

    // If it's a new session (not resuming a paused one)
    if (timerProvider.remainingTime == timerProvider.duration || _currentSessionId == null) {
       // _currentSessionId = sessionProvider.startNewSession(
       //   name: _sessionNameController.text.isNotEmpty ? _sessionNameController.text : null,
       //   type: timerProvider.currentSessionType,
       //   durationMinutes: timerProvider.duration.inMinutes,
       // );
    }
    timerProvider.startTimer();
    if (_isOverlayVisible) {
      _sendTimeToOverlay();
      _startOverlayUpdateTimer(); // Restart updates if overlay is already visible
    }
  }

  void _pauseTimer() {
    Provider.of<TimerProvider>(context, listen: false).pauseTimer();
    WakelockPlus.disable(); 
    _overlayUpdateTimer?.cancel(); // Stop sending updates when paused
  }

  void _resetTimer() {
    Provider.of<TimerProvider>(context, listen: false).resetTimer();
    WakelockPlus.disable(); 
    _currentSessionId = null; 
    _sessionNameController.text = ''; 
    if (_isOverlayVisible) { // Close overlay if timer is reset
      FlutterOverlayWindow.closeOverlay();
      _isOverlayVisible = false;
    }
    _overlayUpdateTimer?.cancel();
  }

  void _skipSession() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    final settingsProvider = Provider.of<SettingsProvider>(context, listen: false);

    WakelockPlus.disable(); 

    if (_currentSessionId != null && timerProvider.isActive) {
      // If a session was active, consider it 'skipped' or 'cancelled'
    }

    timerProvider.skipSession();
    if (_isOverlayVisible) { // Close overlay if session is skipped
      FlutterOverlayWindow.closeOverlay();
      _isOverlayVisible = false;
    }
    _overlayUpdateTimer?.cancel();

    if (settingsProvider.settings.enableNotifications) {
      final remainingMinutes = timerProvider.remainingTime.inMinutes;
      final remainingSeconds = timerProvider.remainingTime.inSeconds % 60;
      WakelockPlus.enable();

      NotificationService().showTimerNotification(
        id: 1,
        title: 'Focus Timer Running',
        body: 'Remaining: $remainingMinutes:${remainingSeconds.toString().padLeft(2, '0')}',
      );
    }
  }

  void _handleTimerCompletion() async {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    final sessionProvider = Provider.of<SessionProvider>(context, listen: false);
    final settingsProvider = Provider.of<SettingsProvider>(context, listen: false);
    final statsProvider = Provider.of<StatsProvider>(context, listen: false);
    final gardenProvider = Provider.of<GardenProvider>(context, listen: false);
    final taskProvider = Provider.of<TaskProvider>(context); // Get TaskProvider

    // Task completion logic - BEFORE other session handling
    if (timerProvider.currentSessionType == SessionType.focus && timerProvider.linkedTaskId != null) {
      final linkedTask = taskProvider.getTaskById(timerProvider.linkedTaskId!);
      if (linkedTask != null) {
        taskProvider.incrementCompletedPomodoros(linkedTask.id);
        
        // Refresh linkedTask to get the updated pomodoro count
        // Need to do this within a try-catch in case the task was deleted in the meantime (edge case)
        try {
          final updatedTask = taskProvider.getTaskById(linkedTask.id);
          // Use setState only if we need to update the UI based on this immediate change before a rebuild
          // linkedTask's pomodoro count display will update on next build anyway if taskProvider notifies listeners.
          // However, for the completion check below, we need the fresh data.
          if (updatedTask != null) {
            if (updatedTask.status != TaskStatus.completed && 
                updatedTask.completedPomodoros >= updatedTask.estimatedPomodoros) {
              // Show dialog to mark task as complete
              bool? markAsComplete = await showDialog<bool>(
                context: context,
                barrierDismissible: false, // User must choose an option
                builder: (BuildContext dialogContext) {
                  return AlertDialog(
                    title: const Text('Task Pomodoros Completed'),
                    content: Text('"${updatedTask.name}" has all estimated Pomodoros completed. Mark task as complete?'),
                    actions: <Widget>[
                      TextButton(
                        child: const Text('Not Now'),
                        onPressed: () {
                          Navigator.of(dialogContext).pop(false);
                        },
                      ),
                      ElevatedButton(
                        child: const Text('Mark Complete'),
                        onPressed: () {
                          Navigator.of(dialogContext).pop(true);
                        },
                      ),
                    ],
                  );
                },
              );

              if (markAsComplete == true) {
                taskProvider.updateTaskStatus(updatedTask.id, TaskStatus.completed);
              }
            }
          }
        } catch (e) {
          // Task might have been deleted, or another error. Log or handle if necessary.
          print('Error refreshing linked task: $e');
        }
      }
    }

    // Original completion logic continues...
    WakelockPlus.disable();

    if (_currentSessionId != null) { // This check might be part of legacy session handling
      // sessionProvider.completeSession(_currentSessionId!); // This seems to be legacy too

      if (timerProvider.currentSessionType == SessionType.focus) {
        final actualMinutes = timerProvider.duration.inMinutes; // Or use loggedDuration from TimerProvider's completeTimer
        // statsProvider.addFocusMinutes(actualMinutes); // This is handled by TimerProvider's _logSessionActivity
        // gardenProvider.addFocusPoints(actualMinutes);

        if (settingsProvider.settings.enableNotifications) {
          NotificationService().showMotivationNotification(
            id: 2,
            title: 'Focus Session Completed!',
            body: 'Great job! You completed a $actualMinutes-minute focus session.',
          );
        }
      }
    }

    final nextSessionType = timerProvider.getNextSessionType();
    int nextDuration;
    String? nextName;

    if (nextSessionType == SessionType.focus) {
      nextDuration = settingsProvider.settings.focusSessionMinutes;
    } else if (nextSessionType == SessionType.shortBreak) {
      nextDuration = settingsProvider.settings.shortBreakMinutes;
    } else {
      nextDuration = settingsProvider.settings.longBreakMinutes;
    }

    timerProvider.initializeTimer(nextDuration, nextSessionType, name: nextName);
    _currentSessionId = null;
    _sessionNameController.clear();
  }

  void _showTimerNotificationIfNeeded() {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false);
    final settingsProvider = Provider.of<SettingsProvider>(context, listen: false);

    if (timerProvider.isActive && !timerProvider.isPaused && settingsProvider.settings.enableNotifications) {
      final remainingMinutes = timerProvider.remainingTime.inMinutes;
      final remainingSeconds = timerProvider.remainingTime.inSeconds % 60;
      WakelockPlus.enable();

      NotificationService().showTimerNotification(
        id: 1,
        title: 'Focus Timer Running',
        body: 'Remaining: $remainingMinutes:${remainingSeconds.toString().padLeft(2, '0')}',
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final timerProvider = Provider.of<TimerProvider>(context);
    final settingsProvider = Provider.of<SettingsProvider>(context);
    final taskProvider = Provider.of<TaskProvider>(context); // Added TaskProvider

    // Get the actual linked task object
    final String? linkedTaskId = timerProvider.linkedTaskId;
    final Task? actualLinkedTask = linkedTaskId != null ? taskProvider.getTaskById(linkedTaskId) : null;

    // Determine if the overlay toggle should be shown
    // bool canShowOverlay = timerProvider.isActive && !timerProvider.isPaused; // Original condition
    // Updated condition: Show if active, regardless of pause state, to allow toggling off even if paused.
    bool canShowOverlay = timerProvider.isActive;

    final Color currentSessionColor = _getSessionTypeColor(timerProvider.currentSessionType, context);

    return Scaffold(
      appBar: AppBar(
        title: Text(timerProvider.sessionName ?? _getSessionTypeText(timerProvider.currentSessionType)),
        actions: [
          if (canShowOverlay)
            IconButton(
              icon: Icon(_isOverlayVisible ? Icons.visibility_off_outlined : Icons.visibility_outlined),
              tooltip: _isOverlayVisible ? 'Hide Overlay' : 'Show Overlay',
              onPressed: _toggleOverlay,
            ),
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const SettingsScreen()),
              );
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            // if (settingsProvider.settings.showGoalProgressTimerScreen) // TODO: Revisit this feature
            //   _buildGoalProgressSection(context),
            
            // Suggested Task Card
            if (timerProvider.suggestedNextTask != null && !timerProvider.isActive)
              _buildSuggestedTaskCard(context, timerProvider, settingsProvider, taskProvider),

            _buildLinkedTaskSection(context, actualLinkedTask),
            const SizedBox(height: 20),
            Consumer<TimerProvider>(
              builder: (context, timerProvider, child) {
                return Expanded(
                  child: CircularPercentIndicator(
                    radius: 140,
                    lineWidth: 15.0,
                    percent: timerProvider.progressPercentage.clamp(0.0, 1.0),
                    center: Text(
                      timerProvider.isActive || timerProvider.isPaused
                          ? '${timerProvider.remainingTime.inMinutes}:${(timerProvider.remainingTime.inSeconds % 60).toString().padLeft(2, '0')}'
                          : (timerProvider.currentSessionType == SessionType.focus
                              ? "START FOCUS"
                              : timerProvider.currentSessionType == SessionType.shortBreak
                                  ? "TAKE BREAK"
                                  : "TAKE LONG BREAK"),
                      style: const TextStyle(
                        fontSize: 60,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    progressColor: currentSessionColor,
                    backgroundColor: Colors.grey.shade200,
                    circularStrokeCap: CircularStrokeCap.round,
                    animation: true,
                    animateFromLastPercent: true,
                  ),
                );
              },
            ),
            const SizedBox(height: 50),

            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ElevatedButton.icon(
                  label: const Text('Reset'),
                  icon: const Icon(Icons.refresh),
                  onPressed: _resetTimer,
                  style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 15)),
                ),
                ElevatedButton.icon(
                  label: timerProvider.isActive && !timerProvider.isPaused
                      ? const Text('Pause')
                      : const Text('Start'),
                  icon: timerProvider.isActive && !timerProvider.isPaused
                      ? const Icon(Icons.pause)
                      : const Icon(Icons.play_arrow),
                  onPressed: timerProvider.isActive && !timerProvider.isPaused
                      ? _pauseTimer
                      : _startTimer,
                  style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 15)),
                ),
                ElevatedButton.icon(
                  label: const Text('Skip'),
                  icon: const Icon(Icons.skip_next),
                  onPressed: _skipSession,
                  style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 15)),
                ),
              ],
            ),

            const SizedBox(height: 40),

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
                      context,
                    ),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 24,
                      vertical: 12,
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
      ),
    );
  }

  // New method to build the suggested task card
  Widget _buildSuggestedTaskCard(BuildContext context, TimerProvider timerProvider, SettingsProvider settingsProvider, TaskProvider taskProvider) {
    final suggestedTask = timerProvider.suggestedNextTask!;

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 16.0),
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Next up: "${suggestedTask.name}"?',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            if (suggestedTask.description != null && suggestedTask.description!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8.0, bottom: 12.0),
                child: Text(
                  suggestedTask.description!,
                  style: Theme.of(context).textTheme.bodyMedium,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                ),
              ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                TextButton(
                  style: TextButton.styleFrom(
                    foregroundColor: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                  ),
                  child: const Text("Dismiss"),
                  onPressed: () {
                    timerProvider.clearSuggestedTask();
                  },
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.primary,
                    foregroundColor: Theme.of(context).colorScheme.onPrimary,
                  ),
                  child: const Text("Link & Start Next"),
                  onPressed: () {
                    final taskName = suggestedTask.name; // Grab name before clearing
                    final focusDuration = settingsProvider.settings.focusSessionMinutes;
                    
                    timerProvider.linkSuggestedTaskToNextSession();
                    timerProvider.initializeTimer(focusDuration, SessionType.focus, name: taskName);
                    _startTimer(); // This local method calls timerProvider.startTimer() and handles Wakelock
                  },
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // Builds the section for displaying goal progress
  Widget _buildGoalProgressSection(BuildContext context) {
    final statsProvider = Provider.of<StatsProvider>(context);
    final settingsProvider = Provider.of<SettingsProvider>(context);

    final dailyGoal = settingsProvider.settings.dailyFocusGoalMinutes;
    final weeklyGoal = settingsProvider.settings.weeklyFocusGoalMinutes;

    final dailyProgress = dailyGoal > 0 ? (statsProvider.focusedMinutesToday / dailyGoal).clamp(0.0, 1.0) : 0.0;
    final weeklyProgress = weeklyGoal > 0 ? (statsProvider.focusedMinutesThisWeek / weeklyGoal).clamp(0.0, 1.0) : 0.0;

    String formatTime(int totalMinutes) {
      if (totalMinutes < 60) return '$totalMinutes min';
      final hours = totalMinutes ~/ 60;
      final minutes = totalMinutes % 60;
      if (minutes == 0) return '$hours h';
      return '$hours h $minutes min';
    }

    return Column(
      children: [
        _buildProgressIndicatorRow(
          context,
          title: 'Today\'s Goal:',
          currentValue: statsProvider.focusedMinutesToday,
          goalValue: dailyGoal,
          progress: dailyProgress,
        ),
        const SizedBox(height: 10),
        _buildProgressIndicatorRow(
          context,
          title: 'This Week\'s Goal:',
          currentValue: statsProvider.focusedMinutesThisWeek,
          goalValue: weeklyGoal,
          progress: weeklyProgress,
        ),
      ],
    );
  }

  Widget _buildProgressIndicatorRow(BuildContext context, {
    required String title,
    required int currentValue,
    required int goalValue,
    required double progress,
  }) {
    String formatTime(int totalMinutes) {
      if (totalMinutes == 0 && goalValue == 0) return 'Not set'; // Or handle as you see fit if goal is 0
      if (totalMinutes < 60) return '$totalMinutes min';
      final hours = totalMinutes ~/ 60;
      final minutes = totalMinutes % 60;
      if (minutes == 0) return '$hours h';
      return '$hours h $minutes min';
    }
    
    final progressText = goalValue > 0 
      ? '${formatTime(currentValue)} / ${formatTime(goalValue)}'
      : 'Goal not set';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            Text(progressText, style: Theme.of(context).textTheme.titleSmall),
          ],
        ),
        const SizedBox(height: 4),
        if (goalValue > 0)
          LinearPercentIndicator(
            lineHeight: 10.0,
            percent: progress,
            backgroundColor: Colors.grey.shade300,
            progressColor: Theme.of(context).colorScheme.primary,
            barRadius: const Radius.circular(5),
          )
        else 
          Container(height: 10, child: Center(child: Text("Set a goal in settings to see progress.", style: TextStyle(color: Colors.grey),))), // Placeholder if goal is 0
      ],
    );
  }

  Widget _buildLinkedTaskSection(BuildContext context, Task? linkedTask) {
    final timerProvider = Provider.of<TimerProvider>(context, listen: false); // No need to listen here
    final taskProvider = Provider.of<TaskProvider>(context, listen: false);
    return Column(
      children: [
        if (linkedTask == null)
          TextButton.icon(
            icon: const Icon(Icons.link),
            label: const Text('Link a task to this session'),
            onPressed: () {
              // _showTaskSelectionDialog will need TimerProvider now
              _showTaskSelectionDialog(context, taskProvider, timerProvider);
            },
          )
        else
          Column(
            children: [
              Text('Working on: ${linkedTask.name}', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text('Pomodoros: ${linkedTask.completedPomodoros}/${linkedTask.estimatedPomodoros}', style: const TextStyle(fontSize: 14)),
              const SizedBox(height: 8),
              TextButton.icon(
                icon: const Icon(Icons.edit_note),
                label: const Text('Change or Unlink Task'),
                onPressed: () {
                  // _showTaskSelectionDialog will need TimerProvider now
                  _showTaskSelectionDialog(context, taskProvider, timerProvider);
                },
              ),
              // Add 'Mark as Complete' button here
              const SizedBox(height: 8),
              ElevatedButton.icon(
                icon: const Icon(Icons.check_circle_outline),
                label: const Text('Mark Task as Complete'),
                onPressed: () {
                  taskProvider.updateTaskStatus(linkedTask.id, TaskStatus.completed);
                  // Optionally, unlink the task after marking it complete
                  if (timerProvider.linkedTaskId == linkedTask.id) {
                    timerProvider.linkTaskToSession(null);
                  }
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('Task "${linkedTask.name}" marked as complete.')),
                  );
                },
                style: ElevatedButton.styleFrom(backgroundColor: Colors.green, foregroundColor: Colors.white),
              )
            ],
          ),
      ],
    );
  }

  Future<void> _showTaskSelectionDialog(BuildContext context, TaskProvider taskProvider, TimerProvider timerProvider) async {
    // Get the current linked task ID from TimerProvider to correctly show checkmark
    final String? currentLinkedTaskId = timerProvider.linkedTaskId;

    final List<Task> availableTasks = taskProvider.tasks
        .where((task) => task.status == TaskStatus.todo || task.status == TaskStatus.inProgress)
        .toList();

    // If no tasks are available and no task is currently linked, show a message.
    // (The original check also considered _linkedTask != null, now use currentLinkedTaskId)
    if (availableTasks.isEmpty && currentLinkedTaskId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No tasks available to link. Add some tasks first!')),
      );
      return;
    }
    
    await showDialog(
      context: context,
      builder: (BuildContext dialogContext) {
        return AlertDialog(
          title: const Text('Select a Task'),
          content: SizedBox(
            width: double.maxFinite,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Unlink button if a task is currently linked
                if (currentLinkedTaskId != null)
                  ListTile(
                    leading: const Icon(Icons.link_off, color: Colors.redAccent),
                    title: const Text('Unlink current task'),
                    onTap: () {
                      timerProvider.linkTaskToSession(null); // Use TimerProvider
                      Navigator.pop(dialogContext);
                    },
                  ),
                if (currentLinkedTaskId != null && availableTasks.isNotEmpty)
                  const Divider(),
                if (availableTasks.isNotEmpty)
                  Expanded(
                    child: ListView.builder(
                      shrinkWrap: true,
                      itemCount: availableTasks.length,
                      itemBuilder: (context, index) {
                        final task = availableTasks[index];
                        return ListTile(
                          leading: Icon(
                            currentLinkedTaskId == task.id ? Icons.check_circle : Icons.radio_button_unchecked,
                            color: currentLinkedTaskId == task.id ? Theme.of(context).colorScheme.primary : null,
                          ),
                          title: Text(task.name),
                          subtitle: Text('Est: ${task.estimatedPomodoros}, Done: ${task.completedPomodoros}'),
                          onTap: () {
                            timerProvider.linkTaskToSession(task.id); // Use TimerProvider
                            Navigator.pop(dialogContext);
                          },
                        );
                      },
                    ),
                  )
                else if (availableTasks.isEmpty && currentLinkedTaskId != null)
                   const Padding(
                     padding: EdgeInsets.all(8.0),
                     child: Text('No other tasks available to link.', textAlign: TextAlign.center),
                   )
                else // This case should ideally be caught by the initial check
                  const Padding(
                    padding: EdgeInsets.all(8.0),
                    child: Text('No tasks to link.', textAlign: TextAlign.center),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.pop(dialogContext);
              },
              child: const Text('Cancel'),
            ),
          ],
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
