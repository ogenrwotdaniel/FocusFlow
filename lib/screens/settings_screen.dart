import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:focus_flow/providers/settings_provider.dart';
import 'package:focus_flow/providers/stats_provider.dart';
import 'package:focus_flow/providers/garden_provider.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:focus_flow/screens/achievements_screen.dart';
import 'package:focus_flow/models/session_model.dart'; // For SessionType
import 'package:focus_flow/models/session_segment_model.dart'; // For SessionSegment
import 'package:flutter/services.dart'; // For FilteringTextInputFormatter
import 'package:share_plus/share_plus.dart'; // For Share.share

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

  // Method to show dialog for adding/editing a session segment
  Future<void> _showAddEditSegmentDialog(BuildContext context, SettingsProvider settingsProvider, {SessionSegment? segment, int? index}) async {
    final _formKey = GlobalKey<FormState>();
    SessionType type = segment?.type ?? SessionType.focus;
    int durationMinutes = segment?.durationMinutes ?? 25;

    return showDialog<void>(
      context: context,
      barrierDismissible: false, // User must tap button!
      builder: (BuildContext dialogContext) {
        return AlertDialog(
          title: Text(segment == null ? 'Add Segment' : 'Edit Segment'),
          content: StatefulBuilder( // To update dialog state for dropdown
            builder: (BuildContext context, StateSetter setState) {
              return Form(
                key: _formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    DropdownButtonFormField<SessionType>(
                      value: type,
                      decoration: const InputDecoration(labelText: 'Segment Type'),
                      items: SessionType.values.map((SessionType value) {
                        return DropdownMenuItem<SessionType>(
                          value: value,
                          child: Text(value.toString().split('.').last),
                        );
                      }).toList(),
                      onChanged: (SessionType? newValue) {
                        if (newValue != null) {
                          setState(() {
                            type = newValue;
                          });
                        }
                      },
                    ),
                    TextFormField(
                      initialValue: durationMinutes.toString(),
                      decoration: const InputDecoration(labelText: 'Duration (minutes)'),
                      keyboardType: TextInputType.number,
                      inputFormatters: <TextInputFormatter>[
                        FilteringTextInputFormatter.digitsOnly
                      ],
                      validator: (value) {
                        if (value == null || value.isEmpty) {
                          return 'Please enter a duration';
                        }
                        final n = int.tryParse(value);
                        if (n == null) {
                          return 'Please enter a valid number';
                        }
                        if (n <= 0) {
                          return 'Duration must be positive';
                        }
                        return null;
                      },
                      onSaved: (value) {
                        durationMinutes = int.parse(value!);
                      },
                    ),
                  ],
                ),
              );
            },
          ),
          actions: <Widget>[
            TextButton(
              child: const Text('Cancel'),
              onPressed: () {
                Navigator.of(dialogContext).pop();
              },
            ),
            TextButton(
              child: const Text('Save'),
              onPressed: () {
                if (_formKey.currentState!.validate()) {
                  _formKey.currentState!.save();
                  final newSegment = SessionSegment(type: type, durationMinutes: durationMinutes);
                  List<SessionSegment> currentSegments = List.from(settingsProvider.settings.customCycleSegments);
                  if (segment == null) { // Add new
                    currentSegments.add(newSegment);
                  } else { // Edit existing
                    if (index != null) {
                      currentSegments[index] = newSegment;
                    }
                  }
                  settingsProvider.updateCustomCycleSegments(currentSegments);
                  Navigator.of(dialogContext).pop();
                }
              },
            ),
          ],
        );
      },
    );
  }

  // Method to show delete confirmation dialog
  Future<void> _showDeleteConfirmationDialog(BuildContext context, String title, String message, VoidCallback onConfirm) async {
    return showDialog<void>(
      context: context,
      barrierDismissible: false, // User must tap button!
      builder: (BuildContext dialogContext) {
        return AlertDialog(
          title: Text(title),
          content: SingleChildScrollView(
            child: ListBody(
              children: <Widget>[
                Text(message),
              ],
            ),
          ),
          actions: <Widget>[
            TextButton(
              child: const Text('Cancel'),
              onPressed: () {
                Navigator.of(dialogContext).pop();
              },
            ),
            TextButton(
              child: const Text('Delete', style: TextStyle(color: Colors.red)),
              onPressed: () {
                onConfirm();
                Navigator.of(dialogContext).pop();
              },
            ),
          ],
        );
      },
    );
  }

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
            
            const SettingsHeader('Automation & Custom Cycles'),
            SwitchSetting(
              title: 'Automatically start breaks',
              subtitle: 'Automatically transition to a break after a focus session ends.',
              value: settingsProvider.settings.autoStartBreaks,
              onChanged: (value) {
                settingsProvider.updateAutomationSettings(
                  autoStartBreaks: value,
                  autoStartFocus: settingsProvider.settings.autoStartFocus,
                );
              },
            ),
            SwitchSetting(
              title: 'Automatically start focus sessions',
              subtitle: 'Automatically transition to a focus session after a break ends.',
              value: settingsProvider.settings.autoStartFocus,
              onChanged: (value) {
                settingsProvider.updateAutomationSettings(
                  autoStartBreaks: settingsProvider.settings.autoStartBreaks,
                  autoStartFocus: value,
                );
              },
            ),
            SwitchSetting(
              title: 'Use custom work/break cycle',
              subtitle: 'Define your own sequence of work and break durations.',
              value: settingsProvider.settings.useCustomCycle,
              onChanged: (value) {
                settingsProvider.updateUseCustomCycle(value);
              },
            ),
            if (settingsProvider.settings.useCustomCycle)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
                child: Card(
                  elevation: 2.0,
                  margin: const EdgeInsets.symmetric(vertical: 8.0),
                  child: Padding(
                    padding: const EdgeInsets.all(12.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Custom Cycle Segments:',
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(color: Theme.of(context).colorScheme.primary),
                        ),
                        const SizedBox(height: 10),
                        if (settingsProvider.settings.customCycleSegments.isEmpty)
                          const Center(child: Padding(
                            padding: EdgeInsets.symmetric(vertical: 16.0),
                            child: Text('No custom segments defined. Tap "Add Segment" to create your cycle.'),
                          ))
                        else
                          ListView.builder(
                            shrinkWrap: true,
                            physics: const NeverScrollableScrollPhysics(),
                            itemCount: settingsProvider.settings.customCycleSegments.length,
                            itemBuilder: (context, index) {
                              final segment = settingsProvider.settings.customCycleSegments[index];
                              return ListTile(
                                title: Text('${segment.type.toString().split('.').last} (${segment.durationMinutes} min)'),
                                trailing: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    IconButton(
                                      icon: Icon(Icons.edit, color: Theme.of(context).colorScheme.secondary),
                                      onPressed: () {
                                        _showAddEditSegmentDialog(context, settingsProvider, segment: segment, index: index);
                                      },
                                    ),
                                    IconButton(
                                      icon: Icon(Icons.delete, color: Theme.of(context).colorScheme.error),
                                      onPressed: () {
                                        _showDeleteConfirmationDialog(
                                          context,
                                          'Delete Segment',
                                          'Are you sure you want to delete this ${segment.type.toString().split('.').last} segment?',
                                          () {
                                            List<SessionSegment> currentSegments = List.from(settingsProvider.settings.customCycleSegments);
                                            currentSegments.removeAt(index);
                                            settingsProvider.updateCustomCycleSegments(currentSegments);
                                          }
                                        );
                                      },
                                    ),
                                  ],
                                ),
                              );
                            },
                          ),
                        const SizedBox(height: 10),
                        Center(
                          child: ElevatedButton.icon(
                            icon: const Icon(Icons.add_circle_outline),
                            label: const Text('Add Segment'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Theme.of(context).colorScheme.primary,
                              foregroundColor: Theme.of(context).colorScheme.onPrimary,
                            ),
                            onPressed: () {
                              _showAddEditSegmentDialog(context, settingsProvider);
                            },
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            
            const SettingsHeader('Ambient Sounds'),
            ListTile(
              title: const Text('Select Sound'),
              trailing: DropdownButton<String>(
                value: settingsProvider.settings.selectedAmbientSound,
                underline: Container(), // Remove underline
                items: <DropdownMenuItem<String>>[
                  const DropdownMenuItem(value: 'none', child: Text('None')),
                  // Assuming assets are directly in 'assets/sounds/'
                  // USER: Please verify these paths match your pubspec.yaml and actual file locations
                  const DropdownMenuItem(value: 'sounds/Music asset 001.mp3', child: Text('Music 1 (Rain)')), // Example: give them nice names
                  const DropdownMenuItem(value: 'sounds/Music asset 002.mp3', child: Text('Music 2 (Forest)')),
                  const DropdownMenuItem(value: 'sounds/Music asset 004.mp3', child: Text('Music 4 (Cafe)')),
                  const DropdownMenuItem(value: 'sounds/Music asset 005.mp3', child: Text('Music 5 (White Noise)')),
                  const DropdownMenuItem(value: 'sounds/Music asset 006.mp3', child: Text('Music 6 (Binaural)')),
                  // Add more sounds from your assets folder as needed, e.g.:
                  // const DropdownMenuItem(value: 'sounds/your_sound_file.mp3', child: Text('Your Sound Name')),
                ],
                onChanged: (String? newValue) {
                  if (newValue != null) {
                    settingsProvider.updateSelectedAmbientSound(newValue);
                  }
                },
              ),
            ),
            SliderSetting(
              title: 'Ambient Sound Volume',
              value: settingsProvider.settings.ambientSoundVolume,
              min: 0.0,
              max: 1.0,
              divisions: 20, // for 0.05 increments
              valueLabel: (settingsProvider.settings.ambientSoundVolume * 100).toStringAsFixed(0) + '%',
              onChanged: (value) {
                settingsProvider.updateAmbientSoundVolume(value);
              },
            ),
            SwitchSetting(
              title: 'Play during Focus Sessions',
              subtitle: 'Play selected sound while focusing.',
              value: settingsProvider.settings.playAmbientSoundDuringFocus,
              onChanged: (value) {
                settingsProvider.updateAmbientSoundPlaybackSettings(playDuringFocus: value);
              },
            ),
            SwitchSetting(
              title: 'Play during Breaks',
              subtitle: 'Play selected sound during break times.',
              value: settingsProvider.settings.playAmbientSoundDuringBreaks,
              onChanged: (value) {
                settingsProvider.updateAmbientSoundPlaybackSettings(playDuringBreaks: value);
              },
            ),
            
            const SettingsHeader('Focus Goals'),
            SliderSetting(
              title: 'Daily Focus Goal',
              value: settingsProvider.settings.dailyFocusGoalMinutes.toDouble(),
              min: 30,
              max: 480,
              divisions: (480 - 30) ~/ 15,
              valueLabel: '${(settingsProvider.settings.dailyFocusGoalMinutes / 60).toStringAsFixed(1)} hours',
              onChanged: (value) {
                settingsProvider.updateFocusGoals(dailyGoal: value.toInt());
              },
            ),
            SliderSetting(
              title: 'Weekly Focus Goal',
              value: settingsProvider.settings.weeklyFocusGoalMinutes.toDouble(),
              min: 120,
              max: 3000,
              divisions: (3000 - 120) ~/ 60,
              valueLabel: '${(settingsProvider.settings.weeklyFocusGoalMinutes / 60).toStringAsFixed(1)} hours',
              onChanged: (value) {
                settingsProvider.updateFocusGoals(weeklyGoal: value.toInt());
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
    final settingsProvider = Provider.of<SettingsProvider>(context);
    final settings = settingsProvider.settings;

    return ListView(
      children: [
        const SettingsHeader('Notifications'),
        SwitchSetting(
          title: 'Enable Notifications',
          subtitle: 'Show timer notifications and reminders',
          value: settings.enableNotifications,
          onChanged: (value) {
            settingsProvider.updateNotificationSettings(
              enableNotifications: value,
              enableMotivationalMessages: settings.enableMotivationalMessages,
            );
          },
        ),
        
        if (settings.enableNotifications)
          SwitchSetting(
            title: 'Motivational Messages',
            subtitle: 'Receive occasional motivational messages',
            value: settings.enableMotivationalMessages,
            onChanged: (value) {
              settingsProvider.updateNotificationSettings(
                enableNotifications: settings.enableNotifications,
                enableMotivationalMessages: value,
              );
            },
          ),
        
        const SettingsHeader('Smarter Reminders'),
        SwitchSetting(
          title: 'Adaptive Break Reminder',
          subtitle: 'Get reminded to take a break after prolonged focus.',
          value: settings.enableAdaptiveBreakReminder,
          onChanged: (value) {
            settingsProvider.updateAdaptiveBreakReminderSettings(
              enable: value,
              thresholdMinutes: settings.adaptiveBreakReminderThresholdMinutes, // Keep current threshold
            );
          },
        ),
        if (settings.enableAdaptiveBreakReminder)
          SliderSetting(
            title: 'Break Reminder Threshold',
            value: settings.adaptiveBreakReminderThresholdMinutes.toDouble(),
            min: 30,
            max: 180,
            divisions: (180 - 30) ~/ 5, // 5 minute increments
            valueLabel: '${settings.adaptiveBreakReminderThresholdMinutes} min',
            onChanged: (value) {
              settingsProvider.updateAdaptiveBreakReminderSettings(
                enable: settings.enableAdaptiveBreakReminder, // Keep current enable state
                thresholdMinutes: value.round(),
              );
            },
          ),
        
        SwitchSetting(
          title: 'Planned Session Start Reminder',
          subtitle: 'Get reminded to start a session you might have planned.',
          value: settings.enablePlannedSessionStartReminder,
          onChanged: (value) {
            settingsProvider.updatePlannedSessionReminderSettings(
              enable: value,
              leadTimeMinutes: settings.plannedSessionReminderLeadTimeMinutes, // Keep current lead time
            );
          },
        ),
        if (settings.enablePlannedSessionStartReminder)
          SliderSetting(
            title: 'Planned Session Lead Time',
            value: settings.plannedSessionReminderLeadTimeMinutes.toDouble(),
            min: 5,
            max: 60,
            divisions: (60 - 5) ~/ 5, // 5 minute increments
            valueLabel: '${settings.plannedSessionReminderLeadTimeMinutes} min before',
            onChanged: (value) {
              settingsProvider.updatePlannedSessionReminderSettings(
                enable: settings.enablePlannedSessionStartReminder, // Keep current enable state
                leadTimeMinutes: value.round(),
              );
            },
          ),

        SwitchSetting(
          title: 'Wind Down Reminder',
          subtitle: 'Gentle reminder before a scheduled session starts.',
          value: settings.enableWindDownReminder,
          onChanged: (value) {
            settingsProvider.updateWindDownReminderSettings(
              enable: value,
              leadTimeMinutes: settings.windDownReminderLeadTimeMinutes, // Keep current lead time
            );
          },
        ),
        if (settings.enableWindDownReminder)
          SliderSetting(
            title: 'Wind Down Lead Time',
            value: settings.windDownReminderLeadTimeMinutes.toDouble(),
            min: 1,
            max: 15,
            divisions: (15 - 1) ~/ 1, // 1 minute increments
            valueLabel: '${settings.windDownReminderLeadTimeMinutes} min before',
            onChanged: (value) {
              settingsProvider.updateWindDownReminderSettings(
                enable: settings.enableWindDownReminder, // Keep current enable state
                leadTimeMinutes: value.round(),
              );
            },
          ),
        
        const SettingsHeader('Garden'),
        SwitchSetting(
          title: 'Enable Growing Garden',
          subtitle: 'Grow virtual trees for completed focus sessions',
          value: settings.enableGrowingGarden,
          onChanged: (value) {
            settingsProvider.updateGardenSettings(
              enableGrowingGarden: value,
            );
          },
        ),
        
        if (settings.enableGrowingGarden)
          const ListTile(
            title: Text('Garden Points'),
            subtitle: Text('Each minute of focus = 1 point for your garden'),
          ),
        const SettingsHeader('Task Management'),
        SwitchListTile(
          title: const Text('Suggest Next Task'),
          subtitle: const Text('After completing a task, suggest the next available one.'),
          value: settings.enableNextTaskSuggestion,
          onChanged: (bool value) {
            settingsProvider.updateEnableNextTaskSuggestion(value);
          },
          secondary: const Icon(Icons.lightbulb_outline),
        ),
      ],
    );
  }
}

class AboutTab extends StatelessWidget {
  const AboutTab({super.key});

  Future<void> _launchURL(String url) async {
    if (await canLaunchUrl(Uri.parse(url))) {
      await launchUrl(Uri.parse(url));
    } else {
      throw 'Could not launch $url';
    }
  }

  @override
  Widget build(BuildContext context) {
    final statsProvider = Provider.of<StatsProvider>(context, listen: false);
    final gardenProvider = Provider.of<GardenProvider>(context, listen: false);
    final settingsProvider = Provider.of<SettingsProvider>(context, listen: false);

    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      children: [
        const SettingsHeader('App Information'),
        ListTile(
          leading: const Icon(Icons.info_outline),
          title: const Text('App Version'),
          subtitle: const Text('1.0.0+1'), // Replace with dynamic version later if needed
          onTap: () { /* Maybe show detailed app info */ },
        ),
        ListTile(
          leading: const Icon(Icons.description_outlined),
          title: const Text('Privacy Policy'),
          onTap: () => _launchURL('https://www.example.com/privacy'), // Replace with your URL
        ),
        ListTile(
          leading: const Icon(Icons.gavel_outlined),
          title: const Text('Terms of Service'),
          onTap: () => _launchURL('https://www.example.com/terms'), // Replace with your URL
        ),

        const SettingsHeader('Data Management'),
        ListTile(
          leading: const Icon(Icons.download_for_offline_outlined),
          title: const Text('Export Session History'),
          subtitle: const Text('Save your session logs as a CSV file.'),
          onTap: () async {
            final scaffoldMessenger = ScaffoldMessenger.of(context);
            final success = await statsProvider.exportSessionData();
            if (success) {
              scaffoldMessenger.showSnackBar(
                const SnackBar(content: Text('Session history export started.'), backgroundColor: Colors.green)
              );
            } else {
              scaffoldMessenger.showSnackBar(
                const SnackBar(content: Text('Failed to export session history or no data.'), backgroundColor: Colors.red)
              );
            }
          },
        ),
        ListTile(
          leading: const Icon(Icons.settings_backup_restore_outlined, color: Colors.orangeAccent),
          title: const Text('Reset All App Data', style: TextStyle(color: Colors.orangeAccent)),
          onTap: () {
            // TODO: Implement reset confirmation dialog and logic
            print("Reset All App Data tapped - implement dialog");
            // Example: _showResetConfirmationDialog(context);
          },
        ),
        const SettingsHeader('Contact & Support'),
        ListTile(
          leading: const Icon(Icons.email_outlined),
          title: const Text('Rate the App'),
          onTap: () => _launchURL('market://details?id=com.example.focus_flow'), // Replace with your app ID
        ),
        ListTile(
          leading: const Icon(Icons.share_outlined),
          title: const Text('Share FocusFlow'),
          onTap: () {
            Share.share('Check out FocusFlow, a great app for productivity and focus! Download here: [Your App Store Link]'); // Replace link
          },
        ),

        const SettingsHeader('Advanced'),
        ListTile(
          leading: Icon(Icons.developer_mode_outlined, color: Colors.blueGrey.shade300),
          title: const Text('Developer Options'),
          subtitle: const Text('Unlock achievements, reset garden etc.'),
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
