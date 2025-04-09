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

  Settings({
    this.focusSessionMinutes = 25,
    this.shortBreakMinutes = 5,
    this.longBreakMinutes = 15,
    this.sessionsBeforeLongBreak = 4,
    this.autoStartBreaks = false,
    this.autoStartFocus = false,
    this.enableSounds = true,
    this.soundTheme = 'nature',
    this.enableMotivationalMessages = true,
    this.enableNotifications = true,
    this.enableGrowingGarden = true,
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
    };
  }

  // Create settings from Map
  factory Settings.fromMap(Map<String, dynamic> map) {
    return Settings(
      focusSessionMinutes: map['focusSessionMinutes'],
      shortBreakMinutes: map['shortBreakMinutes'],
      longBreakMinutes: map['longBreakMinutes'],
      sessionsBeforeLongBreak: map['sessionsBeforeLongBreak'],
      autoStartBreaks: map['autoStartBreaks'] == 1,
      autoStartFocus: map['autoStartFocus'] == 1,
      enableSounds: map['enableSounds'] == 1,
      soundTheme: map['soundTheme'],
      enableMotivationalMessages: map['enableMotivationalMessages'] == 1,
      enableNotifications: map['enableNotifications'] == 1,
      enableGrowingGarden: map['enableGrowingGarden'] == 1,
    );
  }
}
