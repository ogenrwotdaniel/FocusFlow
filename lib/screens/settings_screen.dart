import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:focus_flow/providers/settings_provider.dart';
import 'package:focus_flow/providers/stats_provider.dart';
import 'package:focus_flow/providers/garden_provider.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          toolbarHeight: 0,
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Timer'),
              Tab(text: 'Notifications'),
              Tab(text: 'About'),
            ],
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white70,
            indicatorColor: Colors.white,
          ),
        ),
        body: const TabBarView(
          children: [
            TimerSettingsTab(),
            NotificationSettingsTab(),
            AboutTab(),
          ],
        ),
      ),
    );
  }
}

class TimerSettingsTab extends StatelessWidget {
  const TimerSettingsTab({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<SettingsProvider>(
      builder: (context, settingsProvider, child) {
        return ListView(
          children: [
            const SettingsHeader('Session Durations'),
            SliderSetting(
              title: 'Focus Session Duration',
              value: settingsProvider.settings.focusSessionMinutes.toDouble(),
              min: 5,
              max: 60,
              divisions: 11,
              valueLabel: '${settingsProvider.settings.focusSessionMinutes} minutes',
              onChanged: (value) {
                settingsProvider.updateTimerDurations(
                  focusMinutes: value.toInt(),
                  shortBreakMinutes: settingsProvider.settings.shortBreakMinutes,
                  longBreakMinutes: settingsProvider.settings.longBreakMinutes,
                  sessionsBeforeLongBreak: settingsProvider.settings.sessionsBeforeLongBreak,
                );
              },
            ),
            SliderSetting(
              title: 'Short Break Duration',
              value: settingsProvider.settings.shortBreakMinutes.toDouble(),
              min: 1,
              max: 15,
              divisions: 14,
              valueLabel: '${settingsProvider.settings.shortBreakMinutes} minutes',
              onChanged: (value) {
                settingsProvider.updateTimerDurations(
                  focusMinutes: settingsProvider.settings.focusSessionMinutes,
                  shortBreakMinutes: value.toInt(),
                  longBreakMinutes: settingsProvider.settings.longBreakMinutes,
                  sessionsBeforeLongBreak: settingsProvider.settings.sessionsBeforeLongBreak,
                );
              },
            ),
            SliderSetting(
              title: 'Long Break Duration',
              value: settingsProvider.settings.longBreakMinutes.toDouble(),
              min: 5,
              max: 30,
              divisions: 5,
              valueLabel: '${settingsProvider.settings.longBreakMinutes} minutes',
              onChanged: (value) {
                settingsProvider.updateTimerDurations(
                  focusMinutes: settingsProvider.settings.focusSessionMinutes,
                  shortBreakMinutes: settingsProvider.settings.shortBreakMinutes,
                  longBreakMinutes: value.toInt(),
                  sessionsBeforeLongBreak: settingsProvider.settings.sessionsBeforeLongBreak,
                );
              },
            ),
            SliderSetting(
              title: 'Sessions Before Long Break',
              value: settingsProvider.settings.sessionsBeforeLongBreak.toDouble(),
              min: 2,
              max: 6,
              divisions: 4,
              valueLabel: '${settingsProvider.settings.sessionsBeforeLongBreak} sessions',
              onChanged: (value) {
                settingsProvider.updateTimerDurations(
                  focusMinutes: settingsProvider.settings.focusSessionMinutes,
                  shortBreakMinutes: settingsProvider.settings.shortBreakMinutes,
                  longBreakMinutes: settingsProvider.settings.longBreakMinutes,
                  sessionsBeforeLongBreak: value.toInt(),
                );
              },
            ),
            
            const SettingsHeader('Auto Start'),
            SwitchSetting(
              title: 'Auto Start Breaks',
              subtitle: 'Automatically start break sessions after focus session',
              value: settingsProvider.settings.autoStartBreaks,
              onChanged: (value) {
                settingsProvider.updateAutomationSettings(
                  autoStartBreaks: value,
                  autoStartFocus: settingsProvider.settings.autoStartFocus,
                );
              },
            ),
            SwitchSetting(
              title: 'Auto Start Focus',
              subtitle: 'Automatically start focus sessions after breaks',
              value: settingsProvider.settings.autoStartFocus,
              onChanged: (value) {
                settingsProvider.updateAutomationSettings(
                  autoStartBreaks: settingsProvider.settings.autoStartBreaks,
                  autoStartFocus: value,
                );
              },
            ),
            
            const SettingsHeader('Sound'),
            SwitchSetting(
              title: 'Enable Sounds',
              subtitle: 'Play sounds for notifications and timer completion',
              value: settingsProvider.settings.enableSounds,
              onChanged: (value) {
                settingsProvider.updateSoundSettings(
                  enableSounds: value,
                  soundTheme: settingsProvider.settings.soundTheme,
                );
              },
            ),
            
            if (settingsProvider.settings.enableSounds)
              DropdownSetting(
                title: 'Sound Theme',
                value: settingsProvider.settings.soundTheme,
                items: const [
                  DropdownMenuItem(value: 'nature', child: Text('Nature')),
                  DropdownMenuItem(value: 'minimal', child: Text('Minimal')),
                  DropdownMenuItem(value: 'classic', child: Text('Classic')),
                ],
                onChanged: (value) {
                  if (value != null) {
                    settingsProvider.updateSoundSettings(
                      enableSounds: settingsProvider.settings.enableSounds,
                      soundTheme: value,
                    );
                  }
                },
              ),
            
            const SettingsHeader('Data'),
            ListTile(
              title: const Text('Reset All Statistics'),
              subtitle: const Text('Clear all focus history and statistics'),
              trailing: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red,
                  foregroundColor: Colors.white,
                ),
                onPressed: () {
                  _showResetConfirmationDialog(
                    context, 
                    'Reset Statistics', 
                    'Are you sure you want to reset all your focus statistics? This action cannot be undone.',
                    () {
                      Provider.of<StatsProvider>(context, listen: false).resetStats();
                      Navigator.pop(context);
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Statistics have been reset')),
                      );
                    },
                  );
                },
                child: const Text('Reset'),
              ),
            ),
            
            ListTile(
              title: const Text('Reset Garden'),
              subtitle: const Text('Clear all trees and earned points'),
              trailing: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red,
                  foregroundColor: Colors.white,
                ),
                onPressed: () {
                  _showResetConfirmationDialog(
                    context, 
                    'Reset Garden', 
                    'Are you sure you want to reset your garden? All trees and points will be lost. This action cannot be undone.',
                    () {
                      Provider.of<GardenProvider>(context, listen: false).resetGarden();
                      Navigator.pop(context);
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Garden has been reset')),
                      );
                    },
                  );
                },
                child: const Text('Reset'),
              ),
            ),
          ],
        );
      },
    );
  }
  
  void _showResetConfirmationDialog(BuildContext context, String title, String message, Function onConfirm) {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(title),
          content: Text(message),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () => onConfirm(),
              child: const Text('Reset'),
            ),
          ],
        );
      },
    );
  }
}

class NotificationSettingsTab extends StatelessWidget {
  const NotificationSettingsTab({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<SettingsProvider>(
      builder: (context, settingsProvider, child) {
        return ListView(
          children: [
            const SettingsHeader('Notifications'),
            SwitchSetting(
              title: 'Enable Notifications',
              subtitle: 'Show timer notifications and reminders',
              value: settingsProvider.settings.enableNotifications,
              onChanged: (value) {
                settingsProvider.updateNotificationSettings(
                  enableNotifications: value,
                  enableMotivationalMessages: settingsProvider.settings.enableMotivationalMessages,
                );
              },
            ),
            
            if (settingsProvider.settings.enableNotifications)
              SwitchSetting(
                title: 'Motivational Messages',
                subtitle: 'Receive occasional motivational messages',
                value: settingsProvider.settings.enableMotivationalMessages,
                onChanged: (value) {
                  settingsProvider.updateNotificationSettings(
                    enableNotifications: settingsProvider.settings.enableNotifications,
                    enableMotivationalMessages: value,
                  );
                },
              ),
            
            const SettingsHeader('Garden'),
            SwitchSetting(
              title: 'Enable Growing Garden',
              subtitle: 'Grow virtual trees for completed focus sessions',
              value: settingsProvider.settings.enableGrowingGarden,
              onChanged: (value) {
                settingsProvider.updateGardenSettings(
                  enableGrowingGarden: value,
                );
              },
            ),
            
            if (settingsProvider.settings.enableGrowingGarden)
              const ListTile(
                title: Text('Garden Points'),
                subtitle: Text('Each minute of focus = 1 point for your garden'),
              ),
          ],
        );
      },
    );
  }
}

class AboutTab extends StatelessWidget {
  const AboutTab({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 24.0),
          child: Column(
            children: [
              Icon(
                Icons.timer,
                size: 80,
                color: Theme.of(context).colorScheme.primary,
              ),
              const SizedBox(height: 16),
              const Text(
                'Focus Flow',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Version 1.0.0',
                style: TextStyle(
                  color: Colors.grey,
                ),
              ),
            ],
          ),
        ),
        
        const Divider(),
        
        ListTile(
          leading: const Icon(Icons.info_outline),
          title: const Text('About Focus Flow'),
          onTap: () {
            showDialog(
              context: context,
              builder: (context) {
                return AlertDialog(
                  title: const Text('About Focus Flow'),
                  content: const SingleChildScrollView(
                    child: Text(
                      'Focus Flow is a productivity app designed to help you stay focused and productive using the Pomodoro Technique. '
                      'Features include focus timer, statistics tracking, and a virtual garden that grows as you focus.\n\n'
                      'The Pomodoro Technique is a time management method that uses a timer to break work into intervals, '
                      'traditionally 25 minutes in length, separated by short breaks.\n\n'
                      'Focus Flow helps you implement this technique with customizable focus and break durations.'
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('Close'),
                    ),
                  ],
                );
              },
            );
          },
        ),
        
        ListTile(
          leading: const Icon(Icons.help_outline),
          title: const Text('How to Use'),
          onTap: () {
            showDialog(
              context: context,
              builder: (context) {
                return AlertDialog(
                  title: const Text('How to Use Focus Flow'),
                  content: const SingleChildScrollView(
                    child: Text(
                      '1. Start a focus session from the Timer screen\n\n'
                      '2. Work on your task until the timer ends\n\n'
                      '3. Take a short break when prompted\n\n'
                      '4. After completing several focus sessions, enjoy a longer break\n\n'
                      '5. Track your progress in the Stats screen\n\n'
                      '6. Watch your virtual garden grow as you accumulate focus time\n\n'
                      '7. Customize durations and settings to suit your preferences'
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('Close'),
                    ),
                  ],
                );
              },
            );
          },
        ),
        
        ListTile(
          leading: const Icon(Icons.privacy_tip_outlined),
          title: const Text('Privacy Policy'),
          onTap: () {
            showDialog(
              context: context,
              builder: (context) {
                return AlertDialog(
                  title: const Text('Privacy Policy'),
                  content: const SingleChildScrollView(
                    child: Text(
                      'Focus Flow respects your privacy. All your data is stored locally on your device '
                      'and is not shared with any third parties.\n\n'
                      'Usage statistics are only used to improve your experience and '
                      'help you track your productivity.\n\n'
                      'No personal data is collected or transmitted to external servers.'
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('Close'),
                    ),
                  ],
                );
              },
            );
          },
        ),
        
        ListTile(
          leading: const Icon(Icons.contact_support_outlined),
          title: const Text('Contact & Support'),
          onTap: () {
            showDialog(
              context: context,
              builder: (context) {
                return AlertDialog(
                  title: const Text('Contact & Support'),
                  content: const SingleChildScrollView(
                    child: Text(
                      'For support or feedback, please email:\n'
                      'support@focusflow.app\n\n'
                      'We appreciate your feedback and suggestions for improving the app!'
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('Close'),
                    ),
                  ],
                );
              },
            );
          },
        ),
        
        const Divider(),
        
        const Padding(
          padding: EdgeInsets.all(16.0),
          child: Text(
            '© 2025 Focus Flow\nAll rights reserved.',
            style: TextStyle(
              color: Colors.grey,
              fontSize: 12,
            ),
            textAlign: TextAlign.center,
          ),
        ),
      ],
    );
  }
}

class SettingsHeader extends StatelessWidget {
  final String title;
  
  const SettingsHeader(this.title, {super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
      child: Text(
        title,
        style: TextStyle(
          color: Theme.of(context).colorScheme.primary,
          fontSize: 14,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

class SwitchSetting extends StatelessWidget {
  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;
  
  const SwitchSetting({
    super.key,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      title: Text(title),
      subtitle: Text(subtitle),
      value: value,
      onChanged: onChanged,
      activeColor: Theme.of(context).colorScheme.primary,
    );
  }
}

class SliderSetting extends StatelessWidget {
  final String title;
  final double value;
  final double min;
  final double max;
  final int divisions;
  final String valueLabel;
  final ValueChanged<double> onChanged;
  
  const SliderSetting({
    super.key,
    required this.title,
    required this.value,
    required this.min,
    required this.max,
    required this.divisions,
    required this.valueLabel,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(title),
      subtitle: Row(
        children: [
          Expanded(
            child: Slider(
              value: value,
              min: min,
              max: max,
              divisions: divisions,
              activeColor: Theme.of(context).colorScheme.primary,
              onChanged: onChanged,
            ),
          ),
          SizedBox(
            width: 60,
            child: Text(
              valueLabel,
              style: const TextStyle(
                color: Colors.grey,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class DropdownSetting<T> extends StatelessWidget {
  final String title;
  final T value;
  final List<DropdownMenuItem<T>> items;
  final ValueChanged<T?> onChanged;
  
  const DropdownSetting({
    super.key,
    required this.title,
    required this.value,
    required this.items,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(title),
      trailing: DropdownButton<T>(
        value: value,
        items: items,
        onChanged: onChanged,
        underline: Container(),
      ),
    );
  }
}
