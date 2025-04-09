import 'package:flutter/material.dart';
import 'package:focus_flow/models/session_model.dart';
import 'package:intl/intl.dart';

class SessionListItem extends StatelessWidget {
  final FocusSession session;
  
  const SessionListItem({
    super.key,
    required this.session,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Row(
          children: [
            // Session type icon
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: _getSessionTypeColor(session.type).withOpacity(0.2),
                shape: BoxShape.circle,
              ),
              child: Icon(
                _getSessionTypeIcon(session.type),
                color: _getSessionTypeColor(session.type),
              ),
            ),
            const SizedBox(width: 12),
            
            // Session details
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _getSessionTypeText(session.type),
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _formatSessionTime(session),
                    style: TextStyle(
                      color: Colors.grey.shade600,
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
            
            // Duration
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  '${session.completed ? session.actualDurationMinutes : session.durationMinutes} min',
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  session.completed ? 'Completed' : 'Planned',
                  style: TextStyle(
                    color: session.completed ? Colors.green : Colors.orange,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
  
  Color _getSessionTypeColor(SessionType type) {
    switch (type) {
      case SessionType.focus:
        return Colors.blue;
      case SessionType.shortBreak:
        return Colors.orange;
      case SessionType.longBreak:
        return Colors.green;
    }
  }
  
  IconData _getSessionTypeIcon(SessionType type) {
    switch (type) {
      case SessionType.focus:
        return Icons.timer;
      case SessionType.shortBreak:
        return Icons.coffee;
      case SessionType.longBreak:
        return Icons.weekend;
    }
  }
  
  String _getSessionTypeText(SessionType type) {
    switch (type) {
      case SessionType.focus:
        return 'Focus Session';
      case SessionType.shortBreak:
        return 'Short Break';
      case SessionType.longBreak:
        return 'Long Break';
    }
  }
  
  String _formatSessionTime(FocusSession session) {
    final dateFormat = DateFormat('MMM d, yyyy');
    final timeFormat = DateFormat('h:mm a');
    
    final date = dateFormat.format(session.startTime);
    final startTime = timeFormat.format(session.startTime);
    
    if (session.endTime != null) {
      final endTime = timeFormat.format(session.endTime!);
      return '$date · $startTime - $endTime';
    } else {
      return '$date · $startTime';
    }
  }
}
