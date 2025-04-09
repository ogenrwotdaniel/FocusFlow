import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:focus_flow/models/settings_model.dart';

class SettingsProvider with ChangeNotifier {
  Settings _settings = Settings();
  bool _isLoaded = false;

  // Getters
  Settings get settings => _settings;
  bool get isLoaded => _isLoaded;

  SettingsProvider() {
    loadSettings();
  }

  // Load settings from SharedPreferences
  Future<void> loadSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      _settings = Settings(
        focusSessionMinutes: prefs.getInt('focusSessionMinutes') ?? 25,
        shortBreakMinutes: prefs.getInt('shortBreakMinutes') ?? 5,
        longBreakMinutes: prefs.getInt('longBreakMinutes') ?? 15,
        sessionsBeforeLongBreak: prefs.getInt('sessionsBeforeLongBreak') ?? 4,
        autoStartBreaks: prefs.getBool('autoStartBreaks') ?? false,
        autoStartFocus: prefs.getBool('autoStartFocus') ?? false,
        enableSounds: prefs.getBool('enableSounds') ?? true,
        soundTheme: prefs.getString('soundTheme') ?? 'nature',
        enableMotivationalMessages: prefs.getBool('enableMotivationalMessages') ?? true,
        enableNotifications: prefs.getBool('enableNotifications') ?? true,
        enableGrowingGarden: prefs.getBool('enableGrowingGarden') ?? true,
      );
      
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
    } catch (e) {
      debugPrint('Error saving settings: $e');
    }
  }
}
