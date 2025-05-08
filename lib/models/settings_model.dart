import 'package:flutter/material.dart';
import 'package:focus_flow/models/session_segment_model.dart';

// Default values for settings
const int defaultFocusSessionMinutes = 25;
const int defaultShortBreakMinutes = 5;
const int defaultLongBreakMinutes = 15;
const int defaultSessionsBeforeLongBreak = 4;
const bool defaultAutoStartBreaks = false;
const bool defaultAutoStartFocus = false;
const bool defaultEnableSounds = true;
const String defaultSoundTheme = 'nature';
const bool defaultEnableMotivationalMessages = true;
const bool defaultEnableNotifications = true;
const bool defaultEnableGrowingGarden = true;
const int defaultDailyFocusGoalMinutes = 120;
const int defaultWeeklyFocusGoalMinutes = 600;
const bool defaultVibrate = true;
const bool defaultUseCustomCycle = false;
const String defaultSelectedAmbientSound = 'none';
const double defaultAmbientSoundVolume = 0.5;
const bool defaultPlayAmbientSoundDuringFocus = false;
const bool defaultPlayAmbientSoundDuringBreaks = false;
const bool defaultEnableNextTaskSuggestion = true;

// Defaults for Smarter Notifications & Reminders
const bool defaultEnableAdaptiveBreakReminder = true;
const int defaultAdaptiveBreakReminderThresholdMinutes = 90;
const bool defaultEnablePlannedSessionStartReminder = true;
const int defaultPlannedSessionReminderLeadTimeMinutes = 15;
const bool defaultEnableWindDownReminder = true;
const int defaultWindDownReminderLeadTimeMinutes = 5;

class Settings {
  final int focusSessionMinutes;
  final int shortBreakMinutes;
  final int longBreakMinutes;
  final int sessionsBeforeLongBreak;
  final bool autoStartBreaks;
  final bool autoStartFocus;
  final bool enableSounds;
  final String soundTheme;
  final bool enableMotivationalMessages;
  final bool enableNotifications;
  final bool enableGrowingGarden;
  final int dailyFocusGoalMinutes;
  final int weeklyFocusGoalMinutes;
  final bool vibrate;
  final String? customSoundPath;
  final bool useCustomCycle;
  final List<SessionSegment> customCycleSegments;
  final String selectedAmbientSound;
  final double ambientSoundVolume;
  final bool playAmbientSoundDuringFocus;
  final bool playAmbientSoundDuringBreaks;
  final bool enableNextTaskSuggestion;

  // Smarter Notifications & Reminders
  final bool enableAdaptiveBreakReminder;
  final int adaptiveBreakReminderThresholdMinutes;
  final bool enablePlannedSessionStartReminder;
  final int plannedSessionReminderLeadTimeMinutes;
  final bool enableWindDownReminder;
  final int windDownReminderLeadTimeMinutes;

  Settings({
    this.focusSessionMinutes = defaultFocusSessionMinutes,
    this.shortBreakMinutes = defaultShortBreakMinutes,
    this.longBreakMinutes = defaultLongBreakMinutes,
    this.sessionsBeforeLongBreak = defaultSessionsBeforeLongBreak,
    this.autoStartBreaks = defaultAutoStartBreaks,
    this.autoStartFocus = defaultAutoStartFocus,
    this.enableSounds = defaultEnableSounds,
    this.soundTheme = defaultSoundTheme,
    this.enableMotivationalMessages = defaultEnableMotivationalMessages,
    this.enableNotifications = defaultEnableNotifications,
    this.enableGrowingGarden = defaultEnableGrowingGarden,
    this.dailyFocusGoalMinutes = defaultDailyFocusGoalMinutes,
    this.weeklyFocusGoalMinutes = defaultWeeklyFocusGoalMinutes,
    this.vibrate = defaultVibrate,
    this.customSoundPath,
    this.useCustomCycle = defaultUseCustomCycle,
    this.customCycleSegments = const [],
    this.selectedAmbientSound = defaultSelectedAmbientSound,
    this.ambientSoundVolume = defaultAmbientSoundVolume,
    this.playAmbientSoundDuringFocus = defaultPlayAmbientSoundDuringFocus,
    this.playAmbientSoundDuringBreaks = defaultPlayAmbientSoundDuringBreaks,
    this.enableNextTaskSuggestion = defaultEnableNextTaskSuggestion,
    // Smarter Notifications & Reminders
    this.enableAdaptiveBreakReminder = defaultEnableAdaptiveBreakReminder,
    this.adaptiveBreakReminderThresholdMinutes = defaultAdaptiveBreakReminderThresholdMinutes,
    this.enablePlannedSessionStartReminder = defaultEnablePlannedSessionStartReminder,
    this.plannedSessionReminderLeadTimeMinutes = defaultPlannedSessionReminderLeadTimeMinutes,
    this.enableWindDownReminder = defaultEnableWindDownReminder,
    this.windDownReminderLeadTimeMinutes = defaultWindDownReminderLeadTimeMinutes,
  });

  // Create a copy with updated values
  Settings copyWith({
    int? focusSessionMinutes,
    int? shortBreakMinutes,
    int? longBreakMinutes,
    int? sessionsBeforeLongBreak,
    bool? autoStartBreaks,
    bool? autoStartFocus,
    bool? enableSounds,
    String? soundTheme,
    bool? enableMotivationalMessages,
    bool? enableNotifications,
    bool? enableGrowingGarden,
    int? dailyFocusGoalMinutes,
    int? weeklyFocusGoalMinutes,
    bool? vibrate,
    String? customSoundPath,
    bool? useCustomCycle,
    List<SessionSegment>? customCycleSegments,
    String? selectedAmbientSound,
    double? ambientSoundVolume,
    bool? playAmbientSoundDuringFocus,
    bool? playAmbientSoundDuringBreaks,
    bool? enableNextTaskSuggestion,
    // Smarter Notifications & Reminders
    bool? enableAdaptiveBreakReminder,
    int? adaptiveBreakReminderThresholdMinutes,
    bool? enablePlannedSessionStartReminder,
    int? plannedSessionReminderLeadTimeMinutes,
    bool? enableWindDownReminder,
    int? windDownReminderLeadTimeMinutes,
  }) {
    return Settings(
      focusSessionMinutes: focusSessionMinutes ?? this.focusSessionMinutes,
      shortBreakMinutes: shortBreakMinutes ?? this.shortBreakMinutes,
      longBreakMinutes: longBreakMinutes ?? this.longBreakMinutes,
      sessionsBeforeLongBreak: sessionsBeforeLongBreak ?? this.sessionsBeforeLongBreak,
      autoStartBreaks: autoStartBreaks ?? this.autoStartBreaks,
      autoStartFocus: autoStartFocus ?? this.autoStartFocus,
      enableSounds: enableSounds ?? this.enableSounds,
      soundTheme: soundTheme ?? this.soundTheme,
      enableMotivationalMessages: enableMotivationalMessages ?? this.enableMotivationalMessages,
      enableNotifications: enableNotifications ?? this.enableNotifications,
      enableGrowingGarden: enableGrowingGarden ?? this.enableGrowingGarden,
      dailyFocusGoalMinutes: dailyFocusGoalMinutes ?? this.dailyFocusGoalMinutes,
      weeklyFocusGoalMinutes: weeklyFocusGoalMinutes ?? this.weeklyFocusGoalMinutes,
      vibrate: vibrate ?? this.vibrate,
      customSoundPath: customSoundPath ?? this.customSoundPath,
      useCustomCycle: useCustomCycle ?? this.useCustomCycle,
      customCycleSegments: customCycleSegments ?? this.customCycleSegments,
      selectedAmbientSound: selectedAmbientSound ?? this.selectedAmbientSound,
      ambientSoundVolume: ambientSoundVolume ?? this.ambientSoundVolume,
      playAmbientSoundDuringFocus: playAmbientSoundDuringFocus ?? this.playAmbientSoundDuringFocus,
      playAmbientSoundDuringBreaks: playAmbientSoundDuringBreaks ?? this.playAmbientSoundDuringBreaks,
      enableNextTaskSuggestion: enableNextTaskSuggestion ?? this.enableNextTaskSuggestion,
      // Smarter Notifications & Reminders
      enableAdaptiveBreakReminder: enableAdaptiveBreakReminder ?? this.enableAdaptiveBreakReminder,
      adaptiveBreakReminderThresholdMinutes: adaptiveBreakReminderThresholdMinutes ?? this.adaptiveBreakReminderThresholdMinutes,
      enablePlannedSessionStartReminder: enablePlannedSessionStartReminder ?? this.enablePlannedSessionStartReminder,
      plannedSessionReminderLeadTimeMinutes: plannedSessionReminderLeadTimeMinutes ?? this.plannedSessionReminderLeadTimeMinutes,
      enableWindDownReminder: enableWindDownReminder ?? this.enableWindDownReminder,
      windDownReminderLeadTimeMinutes: windDownReminderLeadTimeMinutes ?? this.windDownReminderLeadTimeMinutes,
    );
  }

  // Convert settings to Map
  Map<String, dynamic> toMap() {
    return {
      'focusSessionMinutes': focusSessionMinutes,
      'shortBreakMinutes': shortBreakMinutes,
      'longBreakMinutes': longBreakMinutes,
      'sessionsBeforeLongBreak': sessionsBeforeLongBreak,
      'autoStartBreaks': autoStartBreaks ? 1 : 0,
      'autoStartFocus': autoStartFocus ? 1 : 0,
      'enableSounds': enableSounds ? 1 : 0,
      'soundTheme': soundTheme,
      'enableMotivationalMessages': enableMotivationalMessages ? 1 : 0,
      'enableNotifications': enableNotifications ? 1 : 0,
      'enableGrowingGarden': enableGrowingGarden ? 1 : 0,
      'dailyFocusGoalMinutes': dailyFocusGoalMinutes,
      'weeklyFocusGoalMinutes': weeklyFocusGoalMinutes,
      'vibrate': vibrate,
      'customSoundPath': customSoundPath,
      'useCustomCycle': useCustomCycle,
      'customCycleSegments': customCycleSegments.map((segment) => segment.toMap()).toList(),
      'selectedAmbientSound': selectedAmbientSound,
      'ambientSoundVolume': ambientSoundVolume,
      'playAmbientSoundDuringFocus': playAmbientSoundDuringFocus,
      'playAmbientSoundDuringBreaks': playAmbientSoundDuringBreaks,
      'enableNextTaskSuggestion': enableNextTaskSuggestion,
      // Smarter Notifications & Reminders
      'enableAdaptiveBreakReminder': enableAdaptiveBreakReminder,
      'adaptiveBreakReminderThresholdMinutes': adaptiveBreakReminderThresholdMinutes,
      'enablePlannedSessionStartReminder': enablePlannedSessionStartReminder,
      'plannedSessionReminderLeadTimeMinutes': plannedSessionReminderLeadTimeMinutes,
      'enableWindDownReminder': enableWindDownReminder,
      'windDownReminderLeadTimeMinutes': windDownReminderLeadTimeMinutes,
    };
  }

  // Create settings from Map
  factory Settings.fromMap(Map<String, dynamic> map) {
    return Settings(
      focusSessionMinutes: map['focusSessionMinutes'] ?? defaultFocusSessionMinutes,
      shortBreakMinutes: map['shortBreakMinutes'] ?? defaultShortBreakMinutes,
      longBreakMinutes: map['longBreakMinutes'] ?? defaultLongBreakMinutes,
      sessionsBeforeLongBreak: map['sessionsBeforeLongBreak'] ?? defaultSessionsBeforeLongBreak,
      autoStartBreaks: (map['autoStartBreaks'] ?? 0) == 1,
      autoStartFocus: (map['autoStartFocus'] ?? 0) == 1,
      enableSounds: (map['enableSounds'] ?? 1) == 1,
      soundTheme: map['soundTheme'] ?? defaultSoundTheme,
      enableMotivationalMessages: (map['enableMotivationalMessages'] ?? 1) == 1,
      enableNotifications: (map['enableNotifications'] ?? 1) == 1,
      enableGrowingGarden: (map['enableGrowingGarden'] ?? 1) == 1,
      dailyFocusGoalMinutes: map['dailyFocusGoalMinutes'] ?? defaultDailyFocusGoalMinutes,
      weeklyFocusGoalMinutes: map['weeklyFocusGoalMinutes'] ?? defaultWeeklyFocusGoalMinutes,
      vibrate: map['vibrate'] ?? defaultVibrate,
      customSoundPath: map['customSoundPath'] as String?,
      useCustomCycle: map['useCustomCycle'] ?? defaultUseCustomCycle,
      customCycleSegments: map['customCycleSegments'] != null
          ? List<SessionSegment>.from(
              (map['customCycleSegments'] as List<dynamic>)
                  .map((segmentMap) => SessionSegment.fromMap(segmentMap as Map<String, dynamic>))
            )
          : [],
      selectedAmbientSound: map['selectedAmbientSound'] ?? defaultSelectedAmbientSound,
      ambientSoundVolume: (map['ambientSoundVolume'] as num?)?.toDouble() ?? defaultAmbientSoundVolume,
      playAmbientSoundDuringFocus: map['playAmbientSoundDuringFocus'] ?? defaultPlayAmbientSoundDuringFocus,
      playAmbientSoundDuringBreaks: map['playAmbientSoundDuringBreaks'] ?? defaultPlayAmbientSoundDuringBreaks,
      enableNextTaskSuggestion: map['enableNextTaskSuggestion'] ?? defaultEnableNextTaskSuggestion,
      // Smarter Notifications & Reminders
      enableAdaptiveBreakReminder: map['enableAdaptiveBreakReminder'] ?? defaultEnableAdaptiveBreakReminder,
      adaptiveBreakReminderThresholdMinutes: map['adaptiveBreakReminderThresholdMinutes'] ?? defaultAdaptiveBreakReminderThresholdMinutes,
      enablePlannedSessionStartReminder: map['enablePlannedSessionStartReminder'] ?? defaultEnablePlannedSessionStartReminder,
      plannedSessionReminderLeadTimeMinutes: map['plannedSessionReminderLeadTimeMinutes'] ?? defaultPlannedSessionReminderLeadTimeMinutes,
      enableWindDownReminder: map['enableWindDownReminder'] ?? defaultEnableWindDownReminder,
      windDownReminderLeadTimeMinutes: map['windDownReminderLeadTimeMinutes'] ?? defaultWindDownReminderLeadTimeMinutes,
    );
  }
}
