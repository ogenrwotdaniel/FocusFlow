import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:focus_flow/models/settings_model.dart';
import 'package:focus_flow/models/session_segment_model.dart';

class SettingsProvider with ChangeNotifier {
  Settings _settings = Settings();
  bool _isLoaded = false;
  String _appVersion = '1.0.0'; // Example version

  // Getters
  Settings get settings => _settings;
  bool get isLoaded => _isLoaded;
  String get appVersion => _appVersion; // Getter for app version

  SettingsProvider() {
    loadSettings();
    _loadAppVersion(); // Load app version on init
  }

  // Example method to load app version (e.g., from package_info_plus)
  Future<void> _loadAppVersion() async {
    // In a real app, you might use package_info_plus like this:
    // PackageInfo packageInfo = await PackageInfo.fromPlatform();
    // _appVersion = packageInfo.version;
    // For now, we'll keep it static as an example
    notifyListeners(); // If appVersion can change or is loaded async
  }

  // Load settings from SharedPreferences
  Future<void> loadSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      _settings = Settings.fromMap({
        'focusSessionMinutes': prefs.getInt('focusSessionMinutes'),
        'shortBreakMinutes': prefs.getInt('shortBreakMinutes'),
        'longBreakMinutes': prefs.getInt('longBreakMinutes'),
        'sessionsBeforeLongBreak': prefs.getInt('sessionsBeforeLongBreak'),
        'autoStartBreaks': prefs.getBool('autoStartBreaks') ?? _settings.autoStartBreaks,
        'autoStartFocus': prefs.getBool('autoStartFocus') ?? _settings.autoStartFocus,
        'enableSounds': prefs.getBool('enableSounds') ?? _settings.enableSounds,
        'soundTheme': prefs.getString('soundTheme') ?? _settings.soundTheme,
        'enableMotivationalMessages': prefs.getBool('enableMotivationalMessages') ?? _settings.enableMotivationalMessages,
        'enableNotifications': prefs.getBool('enableNotifications') ?? _settings.enableNotifications,
        'enableGrowingGarden': (prefs.getInt('enableGrowingGarden') ?? 1) == 1,
        'dailyFocusGoalMinutes': prefs.getInt('dailyFocusGoalMinutes'),
        'weeklyFocusGoalMinutes': prefs.getInt('weeklyFocusGoalMinutes'),
        'vibrate': prefs.getBool('vibrate') ?? true,
        'customSoundPath': prefs.getString('customSoundPath'),
        'useCustomCycle': prefs.getBool('useCustomCycle') ?? false,
        'customCycleSegments': prefs.getString('customCycleSegments') != null
          ? jsonDecode(prefs.getString('customCycleSegments')!) as List<dynamic>
          : [], 
        'selectedAmbientSound': prefs.getString('selectedAmbientSound'),
        'ambientSoundVolume': prefs.getDouble('ambientSoundVolume'),
        'playAmbientSoundDuringFocus': prefs.getBool('playAmbientSoundDuringFocus'),
        'playAmbientSoundDuringBreaks': prefs.getBool('playAmbientSoundDuringBreaks'),
        'enableNextTaskSuggestion': prefs.getBool('enableNextTaskSuggestion'),
        // Smarter Notifications & Reminders
        'enableAdaptiveBreakReminder': prefs.getBool('enableAdaptiveBreakReminder'),
        'adaptiveBreakReminderThresholdMinutes': prefs.getInt('adaptiveBreakReminderThresholdMinutes'),
        'enablePlannedSessionStartReminder': prefs.getBool('enablePlannedSessionStartReminder'),
        'plannedSessionReminderLeadTimeMinutes': prefs.getInt('plannedSessionReminderLeadTimeMinutes'),
        'enableWindDownReminder': prefs.getBool('enableWindDownReminder'),
        'windDownReminderLeadTimeMinutes': prefs.getInt('windDownReminderLeadTimeMinutes'),
      });
      
      _isLoaded = true;
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading settings: $e');
    }
  }

  // Update timer durations
  Future<void> updateTimerDurations({
    required int focusMinutes,
    required int shortBreakMinutes,
    required int longBreakMinutes,
    required int sessionsBeforeLongBreak,
  }) async {
    _settings = _settings.copyWith(
      focusSessionMinutes: focusMinutes,
      shortBreakMinutes: shortBreakMinutes,
      longBreakMinutes: longBreakMinutes,
      sessionsBeforeLongBreak: sessionsBeforeLongBreak,
    );
    
    await _saveSettings();
    notifyListeners();
  }

  // Update automation settings
  Future<void> updateAutomationSettings({
    required bool autoStartBreaks,
    required bool autoStartFocus,
  }) async {
    _settings = _settings.copyWith(
      autoStartBreaks: autoStartBreaks,
      autoStartFocus: autoStartFocus,
    );
    
    await _saveSettings();
    notifyListeners();
  }

  // Update sound settings
  Future<void> updateSoundSettings({
    required bool enableSounds,
    required String soundTheme,
  }) async {
    _settings = _settings.copyWith(
      enableSounds: enableSounds,
      soundTheme: soundTheme,
    );
    
    await _saveSettings();
    notifyListeners();
  }

  // Update notification settings
  Future<void> updateNotificationSettings({
    required bool enableNotifications,
    required bool enableMotivationalMessages,
  }) async {
    _settings = _settings.copyWith(
      enableNotifications: enableNotifications,
      enableMotivationalMessages: enableMotivationalMessages,
    );
    
    await _saveSettings();
    notifyListeners();
  }

  // Update garden settings
  Future<void> updateGardenSettings({
    required bool enableGrowingGarden,
  }) async {
    _settings = _settings.copyWith(
      enableGrowingGarden: enableGrowingGarden,
    );
    
    await _saveSettings();
    notifyListeners();
  }

  // New method to update focus goals
  Future<void> updateFocusGoals({
    int? dailyGoal,
    int? weeklyGoal,
  }) async {
    _settings = _settings.copyWith(
      dailyFocusGoalMinutes: dailyGoal,
      weeklyFocusGoalMinutes: weeklyGoal,
    );
    await _saveSettings();
    notifyListeners();
  }

  // Method to update custom cycle toggle
  Future<void> updateUseCustomCycle(bool useCustomCycle) async {
    _settings = _settings.copyWith(useCustomCycle: useCustomCycle);
    await _saveSettings();
    notifyListeners();
  }

  // Method to update custom cycle segments
  Future<void> updateCustomCycleSegments(List<SessionSegment> segments) async {
    _settings = _settings.copyWith(customCycleSegments: segments);
    await _saveSettings();
    notifyListeners();
  }

  // --- Ambient Sound Settings --- 
  Future<void> updateSelectedAmbientSound(String soundPath) async {
    _settings = _settings.copyWith(selectedAmbientSound: soundPath);
    await _saveSettings();
    notifyListeners();
  }

  Future<void> updateAmbientSoundVolume(double volume) async {
    // Clamp volume between 0.0 and 1.0
    final clampedVolume = volume.clamp(0.0, 1.0);
    _settings = _settings.copyWith(ambientSoundVolume: clampedVolume);
    await _saveSettings();
    notifyListeners();
  }

  Future<void> updatePlayAmbientSoundDuringFocus(bool play) async {
    _settings = _settings.copyWith(playAmbientSoundDuringFocus: play);
    await _saveSettings();
    notifyListeners();
  }

  Future<void> updatePlayAmbientSoundDuringBreaks(bool play) async {
    _settings = _settings.copyWith(playAmbientSoundDuringBreaks: play);
    await _saveSettings();
    notifyListeners();
  }

  // Combined method for convenience
  Future<void> updateAmbientSoundPlaybackSettings({
    bool? playDuringFocus,
    bool? playDuringBreaks,
  }) async {
    _settings = _settings.copyWith(
      playAmbientSoundDuringFocus: playDuringFocus ?? _settings.playAmbientSoundDuringFocus,
      playAmbientSoundDuringBreaks: playDuringBreaks ?? _settings.playAmbientSoundDuringBreaks,
    );
    await _saveSettings();
    notifyListeners();
  }

  // --- Smarter Notifications & Reminders Settings ---
  Future<void> updateAdaptiveBreakReminderSettings({
    required bool enable,
    required int thresholdMinutes,
  }) async {
    _settings = _settings.copyWith(
      enableAdaptiveBreakReminder: enable,
      adaptiveBreakReminderThresholdMinutes: thresholdMinutes,
    );
    await _saveSettings();
    notifyListeners();
  }

  Future<void> updatePlannedSessionReminderSettings({
    required bool enable,
    required int leadTimeMinutes,
  }) async {
    _settings = _settings.copyWith(
      enablePlannedSessionStartReminder: enable,
      plannedSessionReminderLeadTimeMinutes: leadTimeMinutes,
    );
    await _saveSettings();
    notifyListeners();
  }

  Future<void> updateWindDownReminderSettings({
    required bool enable,
    required int leadTimeMinutes,
  }) async {
    _settings = _settings.copyWith(
      enableWindDownReminder: enable,
      windDownReminderLeadTimeMinutes: leadTimeMinutes,
    );
    await _saveSettings();
    notifyListeners();
  }

  // --- General Settings ---
  Future<void> updateTheme(String theme) async {
    _settings = _settings.copyWith(soundTheme: theme);
    await _saveSettings();
    notifyListeners();
  }

  // Method to update next task suggestion setting
  Future<void> updateEnableNextTaskSuggestion(bool value) async {
    _settings = _settings.copyWith(enableNextTaskSuggestion: value);
    await _saveSettings();
    notifyListeners();
  }

  // Save settings to SharedPreferences
  Future<void> _saveSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      await prefs.setInt('focusSessionMinutes', _settings.focusSessionMinutes);
      await prefs.setInt('shortBreakMinutes', _settings.shortBreakMinutes);
      await prefs.setInt('longBreakMinutes', _settings.longBreakMinutes);
      await prefs.setInt('sessionsBeforeLongBreak', _settings.sessionsBeforeLongBreak);
      await prefs.setBool('autoStartBreaks', _settings.autoStartBreaks);
      await prefs.setBool('autoStartFocus', _settings.autoStartFocus);
      await prefs.setBool('enableSounds', _settings.enableSounds);
      await prefs.setString('soundTheme', _settings.soundTheme);
      await prefs.setBool('enableMotivationalMessages', _settings.enableMotivationalMessages);
      await prefs.setBool('enableNotifications', _settings.enableNotifications);
      await prefs.setBool('enableGrowingGarden', _settings.enableGrowingGarden);
      await prefs.setInt('dailyFocusGoalMinutes', _settings.dailyFocusGoalMinutes);
      await prefs.setInt('weeklyFocusGoalMinutes', _settings.weeklyFocusGoalMinutes);
      await prefs.setBool('vibrate', _settings.vibrate);
      if (_settings.customSoundPath != null) {
        await prefs.setString('customSoundPath', _settings.customSoundPath!);
      } else {
        await prefs.remove('customSoundPath');
      }
      await prefs.setBool('useCustomCycle', _settings.useCustomCycle);
      await prefs.setString('customCycleSegments', jsonEncode(_settings.customCycleSegments.map((s) => s.toMap()).toList()));
      await prefs.setString('selectedAmbientSound', _settings.selectedAmbientSound);
      await prefs.setDouble('ambientSoundVolume', _settings.ambientSoundVolume);
      await prefs.setBool('playAmbientSoundDuringFocus', _settings.playAmbientSoundDuringFocus);
      await prefs.setBool('playAmbientSoundDuringBreaks', _settings.playAmbientSoundDuringBreaks);
      await prefs.setBool('enableNextTaskSuggestion', _settings.enableNextTaskSuggestion);

      // Smarter Notifications & Reminders
      await prefs.setBool('enableAdaptiveBreakReminder', _settings.enableAdaptiveBreakReminder);
      await prefs.setInt('adaptiveBreakReminderThresholdMinutes', _settings.adaptiveBreakReminderThresholdMinutes);
      await prefs.setBool('enablePlannedSessionStartReminder', _settings.enablePlannedSessionStartReminder);
      await prefs.setInt('plannedSessionReminderLeadTimeMinutes', _settings.plannedSessionReminderLeadTimeMinutes);
      await prefs.setBool('enableWindDownReminder', _settings.enableWindDownReminder);
      await prefs.setInt('windDownReminderLeadTimeMinutes', _settings.windDownReminderLeadTimeMinutes);
    } catch (e) {
      debugPrint('Error saving settings: $e');
    }
  }
}
