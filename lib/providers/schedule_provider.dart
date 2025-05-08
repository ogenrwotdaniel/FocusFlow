import 'package:flutter/foundation.dart';
import 'package:focus_flow/models/scheduled_session_model.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'package:focus_flow/services/notification_service.dart';
import 'package:focus_flow/providers/settings_provider.dart';

class ScheduleProvider with ChangeNotifier {
  List<ScheduledSession> _scheduledSessions = [];
  SettingsProvider? _settingsProvider;
  NotificationService? _notificationService;
  // Store previous settings values to detect changes
  bool? _previousEnablePlannedReminder;
  int? _previousPlannedReminderLeadTime;
  bool? _previousEnableWindDownReminder;
  int? _previousWindDownLeadTime;

  List<ScheduledSession> get scheduledSessions => _scheduledSessions;

  ScheduleProvider() {
    // loadScheduledSessions will be called by the proxy provider after settings are available.
  }

  // Call this method from the ProxyProvider in main.dart
  void updateDependencies(SettingsProvider settingsProvider, NotificationService notificationService) {
    if (_settingsProvider != settingsProvider) {
      _settingsProvider?.removeListener(_handleSettingsChange); // Remove listener from old provider
      _settingsProvider = settingsProvider;
      _settingsProvider?.addListener(_handleSettingsChange); // Add listener to new provider
      // Store initial values
      _previousEnablePlannedReminder = _settingsProvider?.settings.enablePlannedSessionStartReminder;
      _previousPlannedReminderLeadTime = _settingsProvider?.settings.plannedSessionReminderLeadTimeMinutes;
      _previousEnableWindDownReminder = _settingsProvider?.settings.enableWindDownReminder;
      _previousWindDownLeadTime = _settingsProvider?.settings.windDownReminderLeadTimeMinutes;
    }

    if (_notificationService != notificationService) {
      _notificationService = notificationService;
    }
    
    // Load sessions and then update reminders once dependencies are set
    // This should ideally only run once or if dependencies truly change significantly
    if (_scheduledSessions.isEmpty) { // Basic check to avoid redundant full refreshes if possible
        loadScheduledSessions().then((_) => updateAllSessionReminders());
    }
  }

  void _handleSettingsChange() {
    if (_settingsProvider == null) return;

    final newEnablePlannedReminder = _settingsProvider!.settings.enablePlannedSessionStartReminder;
    final newPlannedReminderLeadTime = _settingsProvider!.settings.plannedSessionReminderLeadTimeMinutes;
    final newEnableWindDownReminder = _settingsProvider!.settings.enableWindDownReminder;
    final newWindDownLeadTime = _settingsProvider!.settings.windDownReminderLeadTimeMinutes;

    bool changed = false;
    if (newEnablePlannedReminder != _previousEnablePlannedReminder) {
      _previousEnablePlannedReminder = newEnablePlannedReminder;
      changed = true;
      debugPrint('[ScheduleProvider] enablePlannedSessionStartReminder setting changed to $newEnablePlannedReminder');
    }
    if (newPlannedReminderLeadTime != _previousPlannedReminderLeadTime) {
      _previousPlannedReminderLeadTime = newPlannedReminderLeadTime;
      changed = true;
      debugPrint('[ScheduleProvider] plannedSessionReminderLeadTimeMinutes setting changed to $newPlannedReminderLeadTime');
    }
    if (newEnableWindDownReminder != _previousEnableWindDownReminder) {
      _previousEnableWindDownReminder = newEnableWindDownReminder;
      changed = true;
      debugPrint('[ScheduleProvider] enableWindDownReminder setting changed to $newEnableWindDownReminder');
    }
    if (newWindDownLeadTime != _previousWindDownLeadTime) {
      _previousWindDownLeadTime = newWindDownLeadTime;
      changed = true;
      debugPrint('[ScheduleProvider] windDownReminderLeadTimeMinutes setting changed to $newWindDownLeadTime');
    }

    if (changed) {
      updateAllSessionReminders();
    }
  }

  @override
  void dispose() {
    _settingsProvider?.removeListener(_handleSettingsChange);
    super.dispose();
  }

  Future<void> _scheduleReminderForSession(ScheduledSession session) async {
    if (_settingsProvider == null || _notificationService == null) return;

    if (!_settingsProvider!.settings.enablePlannedSessionStartReminder) {
      // If reminders are disabled globally, ensure any existing one for this session is cancelled.
      if (session.notificationId != null) {
        await _cancelReminderForSession(session); // No suppressNotify needed, direct call
      }
      return;
    }

    final leadTimeMinutes = _settingsProvider!.settings.plannedSessionReminderLeadTimeMinutes;
    final scheduledTime = session.plannedStartTime.subtract(Duration(minutes: leadTimeMinutes));

    // Ensure notificationId is present; generate if somehow missing
    final notificationId = session.notificationId ?? (session.id.hashCode & 0x7FFFFFFF);
    final windDownNotificationId = session.windDownNotificationId ?? ((session.id.hashCode & 0x3FFFFFFF) + 0x40000000);

    final sessionWithEnsuredNotificationId = session.copyWith(
      notificationId: notificationId,
      windDownNotificationId: windDownNotificationId
    );

    // Update the session in the list if notificationId was just generated
    // This part is tricky as we don't want to call notifyListeners from here if called in a loop.
    // Better to ensure notificationId is assigned when session is created/updated.

    if (scheduledTime.isAfter(DateTime.now())) {
      await _notificationService!.scheduleSessionNotification(
        id: notificationId,
        title: sessionWithEnsuredNotificationId.name ?? 'Focus Session Reminder',
        body: 'Starts in $leadTimeMinutes min: ${sessionWithEnsuredNotificationId.name ?? sessionWithEnsuredNotificationId.sessionType.name}',
        scheduledTime: scheduledTime,
        payload: sessionWithEnsuredNotificationId.id,
      );
      debugPrint('[ScheduleProvider] Reminder scheduled for session ${session.id} at $scheduledTime');
    } else {
      debugPrint('[ScheduleProvider] Reminder for session ${session.id} not scheduled (time is in the past).');
      // If reminder time is in the past but session start time is in future, cancel any stale notif
      if (session.notificationId != null && session.plannedStartTime.isAfter(DateTime.now())){
         await _cancelReminderForSession(session, suppressNotify: true);
      }
    }
  }

  Future<void> _scheduleWindDownReminderForSession(ScheduledSession session) async {
    if (_settingsProvider == null || _notificationService == null) return;

    if (!_settingsProvider!.settings.enableWindDownReminder) {
      if (session.windDownNotificationId != null) {
        await _cancelWindDownReminderForSession(session); 
      }
      return;
    }

    final leadTimeMinutes = _settingsProvider!.settings.windDownReminderLeadTimeMinutes;
    final scheduledTime = session.plannedStartTime.subtract(Duration(minutes: leadTimeMinutes));
    
    final windDownNotificationId = session.windDownNotificationId!;

    if (scheduledTime.isAfter(DateTime.now())) {
      await _notificationService!.scheduleSessionNotification(
        id: windDownNotificationId,
        title: 'Time to Wind Down for: ${session.name ?? 'Focus Session'}',
        body: 'Your session starts in $leadTimeMinutes minutes. Prepare to focus!',
        scheduledTime: scheduledTime,
        payload: 'wind_down_${session.id}', // Differentiate payload if needed
      );
      debugPrint('[ScheduleProvider] Wind-down reminder scheduled for session ${session.id} at $scheduledTime');
    } else {
      debugPrint('[ScheduleProvider] Wind-down reminder for session ${session.id} not scheduled (time is in the past).');
      if (session.windDownNotificationId != null && session.plannedStartTime.isAfter(DateTime.now())) {
        await _cancelWindDownReminderForSession(session);
      }
    }
  }

  Future<void> _cancelReminderForSession(ScheduledSession session, {bool suppressNotify = false}) async {
    if (_notificationService == null || session.notificationId == null) return;
    await _notificationService!.cancelNotification(session.notificationId!);
    debugPrint('[ScheduleProvider] Planned start reminder cancelled for session ${session.id}');
  }

  Future<void> _cancelWindDownReminderForSession(ScheduledSession session, {bool suppressNotify = false}) async {
    if (_notificationService == null || session.windDownNotificationId == null) return;
    await _notificationService!.cancelNotification(session.windDownNotificationId!);
    debugPrint('[ScheduleProvider] Wind-down reminder cancelled for session ${session.id}');
  }

  Future<void> updateAllSessionReminders() async {
    if (_settingsProvider == null || _notificationService == null) {
      debugPrint('[ScheduleProvider] Cannot update all reminders: dependencies not set.');
      return;
    }
    debugPrint('[ScheduleProvider] Updating all session reminders...');
    for (var session in List<ScheduledSession>.from(_scheduledSessions)) { // Iterate over a copy
      // Always cancel first to handle changes in session time or global settings
      if (session.notificationId != null) {
        await _cancelReminderForSession(session, suppressNotify: true);
      }
      if (session.windDownNotificationId != null) {
        await _cancelWindDownReminderForSession(session, suppressNotify: true);
      }
      // Then reschedule if applicable
      await _scheduleReminderForSession(session);
      await _scheduleWindDownReminderForSession(session);
    }
    // No notifyListeners() here as this is often called during other operations that will notify.
  }

  Future<void> addScheduledSession(ScheduledSession session) async {
    // Ensure notificationId is assigned before adding
    final notificationId = session.notificationId ?? (session.id.hashCode & 0x7FFFFFFF);
    final windDownNotificationId = session.windDownNotificationId ?? ((session.id.hashCode & 0x3FFFFFFF) + 0x40000000);

    final sessionToAdd = session.copyWith(
      notificationId: notificationId,
      windDownNotificationId: windDownNotificationId
    );

    _scheduledSessions.add(sessionToAdd);
    await _scheduleReminderForSession(sessionToAdd);
    await _scheduleWindDownReminderForSession(sessionToAdd);
    await _saveScheduledSessions();
    notifyListeners();
  }

  Future<void> removeScheduledSession(String sessionId) async {
    final sessionIndex = _scheduledSessions.indexWhere((s) => s.id == sessionId);
    if (sessionIndex != -1) {
      final sessionToRemove = _scheduledSessions[sessionIndex];
      await _cancelReminderForSession(sessionToRemove);
      await _cancelWindDownReminderForSession(sessionToRemove);
      _scheduledSessions.removeAt(sessionIndex);
      await _saveScheduledSessions();
      notifyListeners();
    }
  }

  Future<void> updateScheduledSession(ScheduledSession updatedSession) async {
    final index = _scheduledSessions.indexWhere((s) => s.id == updatedSession.id);
    if (index != -1) {
      final oldSession = _scheduledSessions[index];
      await _cancelReminderForSession(oldSession); // Cancel old one first
      await _cancelWindDownReminderForSession(oldSession);

      // Ensure notificationId is consistent or regenerated if necessary
      final notificationId = updatedSession.notificationId ?? (updatedSession.id.hashCode & 0x7FFFFFFF);
      final windDownNotificationId = updatedSession.windDownNotificationId ?? ((updatedSession.id.hashCode & 0x3FFFFFFF) + 0x40000000);
      
      final sessionToUpdate = updatedSession.copyWith(
        notificationId: notificationId,
        windDownNotificationId: windDownNotificationId
      );

      _scheduledSessions[index] = sessionToUpdate;
      await _scheduleReminderForSession(sessionToUpdate); // Schedule new one
      await _scheduleWindDownReminderForSession(sessionToUpdate);
      await _saveScheduledSessions();
      notifyListeners();
    }
  }

  // Marks a session as started, e.g., when user acts on a notification
  Future<void> markSessionAsStarted(String sessionId) async {
    final index = _scheduledSessions.indexWhere((s) => s.id == sessionId);
    if (index != -1 && !_scheduledSessions[index].isStarted) {
      _scheduledSessions[index] = _scheduledSessions[index].copyWith(isStarted: true);
      // Optionally cancel notification if it was for the start time and not a pre-reminder
      // if (_scheduledSessions[index].notificationId != null) {
      //   await NotificationService().cancelNotification(_scheduledSessions[index].notificationId!);
      // }
      await _saveScheduledSessions();
      notifyListeners();
    }
  }

  Future<void> _saveScheduledSessions() async {
    final prefs = await SharedPreferences.getInstance();
    final List<String> sessionsJson =
        _scheduledSessions.map((s) => jsonEncode(s.toMap())).toList();
    await prefs.setStringList('scheduledSessions', sessionsJson);
  }

  Future<void> loadScheduledSessions() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final sessionsJson = prefs.getStringList('scheduledSessions') ?? [];
      _scheduledSessions = sessionsJson
          .map((json) => ScheduledSession.fromMap(jsonDecode(json)))
          .toList();
      _scheduledSessions.sort((a, b) => a.plannedStartTime.compareTo(b.plannedStartTime)); // Sort by planned time
      // Do not call updateAllSessionReminders here directly;
      // it will be called by updateDependencies after this load completes.
      notifyListeners(); // Notify that sessions are loaded
    } catch (e) {
      debugPrint('Error loading scheduled sessions: $e');
      // Consider how to handle errors, maybe clear corrupted data
    }
  }
}
