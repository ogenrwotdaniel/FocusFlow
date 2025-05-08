import 'package:flutter/material.dart';
import 'package:focus_flow/models/scheduled_session_model.dart';
import 'package:focus_flow/models/session_model.dart';
import 'package:focus_flow/providers/schedule_provider.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import 'add_scheduled_session_screen.dart'; 
import 'timer_screen.dart'; 

class ScheduleScreen extends StatelessWidget {
  const ScheduleScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final scheduleProvider = Provider.of<ScheduleProvider>(context);
    final scheduledSessions = scheduleProvider.scheduledSessions;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Planned Sessions'),
        // We might add actions here later, e.g., filter, sort
      ),
      body: scheduledSessions.isEmpty
          ? const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.event_note_outlined, size: 80, color: Colors.grey),
                  SizedBox(height: 16),
                  Text(
                    'No sessions planned yet.',
                    style: TextStyle(fontSize: 18, color: Colors.grey),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'Tap the "+" button to schedule a new session.',
                    style: TextStyle(color: Colors.grey),
                  ),
                ],
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.all(8.0),
              itemCount: scheduledSessions.length,
              itemBuilder: (context, index) {
                final session = scheduledSessions[index];
                return Card(
                  margin: const EdgeInsets.symmetric(vertical: 8.0),
                  child: ListTile(
                    leading: Icon(
                      session.sessionType == SessionType.focus
                          ? Icons.timer_outlined
                          : session.sessionType == SessionType.shortBreak
                              ? Icons.coffee_outlined
                              : Icons.self_improvement_outlined,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    title: Text(
                      session.name ?? 'Scheduled ${session.sessionType.name.toLowerCase()} Session',
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                            'Starts: ${DateFormat.yMd().add_jm().format(session.plannedStartTime)}'),
                        Text('Duration: ${session.plannedDurationMinutes} min'),
                        if (session.isStarted)
                           const Text('Status: Started', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
                      ],
                    ),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (!session.isStarted && session.plannedStartTime.isBefore(DateTime.now().add(const Duration(minutes: 15)))) // Show start if not started and near/past time
                          IconButton(
                            icon: const Icon(Icons.play_circle_outline, color: Colors.green),
                            tooltip: 'Start Session',
                            onPressed: () {
                              scheduleProvider.markSessionAsStarted(session.id); // Mark session as started
                              Navigator.of(context).push(MaterialPageRoute(
                                builder: (_) => TimerScreen(scheduledSession: session), // Navigate to TimerScreen with session
                              ));
                            },
                          ),
                        IconButton(
                          icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                          tooltip: 'Delete Session',
                          onPressed: () {
                            showDialog(
                              context: context,
                              builder: (ctx) => AlertDialog(
                                title: const Text('Delete Session?'),
                                content: const Text('Are you sure you want to delete this scheduled session?'),
                                actions: [
                                  TextButton(
                                    child: const Text('Cancel'),
                                    onPressed: () => Navigator.of(ctx).pop(),
                                  ),
                                  TextButton(
                                    child: const Text('Delete', style: TextStyle(color: Colors.red)),
                                    onPressed: () {
                                      scheduleProvider.removeScheduledSession(session.id);
                                      Navigator.of(ctx).pop();
                                      ScaffoldMessenger.of(context).showSnackBar(
                                        const SnackBar(content: Text('Scheduled session deleted.'), duration: Duration(seconds: 2))
                                      );
                                    },
                                  ),
                                ],
                              ),
                            );
                          },
                        ),
                      ],
                    ),
                    isThreeLine: true, // isThreeLine should be true if subtitle has multiple lines or is long
                    onTap: () {
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => AddScheduledSessionScreen(sessionToEdit: session),
                      ));
                    },
                  ),
                );
              },
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.of(context).push(MaterialPageRoute(
            builder: (_) => const AddScheduledSessionScreen(),
          ));
        },
        tooltip: 'Plan New Session',
        child: const Icon(Icons.add),
      ),
    );
  }
}
