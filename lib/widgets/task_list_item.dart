import 'package:flutter/material.dart';
import 'package:focus_flow/models/task_model.dart';

class TaskListItem extends StatelessWidget {
  final Task task;
  final Function(TaskStatus) onStatusChanged;
  final VoidCallback onDeleteTask;

  const TaskListItem({
    super.key,
    required this.task,
    required this.onStatusChanged,
    required this.onDeleteTask,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Priority indicator
                Container(
                  width: 4,
                  height: 50,
                  decoration: BoxDecoration(
                    color: _getPriorityColor(task.priority),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
                const SizedBox(width: 12),
                
                // Task content
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        task.name,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          decoration: task.status == TaskStatus.completed 
                              ? TextDecoration.lineThrough 
                              : null,
                        ),
                      ),
                      if (task.description.isNotEmpty) ...[
                        const SizedBox(height: 4),
                        Text(
                          task.description,
                          style: TextStyle(
                            color: Colors.grey.shade700,
                            decoration: task.status == TaskStatus.completed 
                                ? TextDecoration.lineThrough 
                                : null,
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
                
                // Task actions
                _buildStatusIcon(context),
              ],
            ),
            
            // Task footer
            Padding(
              padding: const EdgeInsets.only(top: 12.0, left: 16.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  // Priority label
                  Row(
                    children: [
                      Icon(
                        Icons.flag,
                        size: 16,
                        color: _getPriorityColor(task.priority),
                      ),
                      const SizedBox(width: 4),
                      Text(
                        _getPriorityText(task.priority),
                        style: TextStyle(
                          color: Colors.grey.shade600,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                  
                  // Status actions
                  Row(
                    children: [
                      if (task.status != TaskStatus.completed) ...[
                        _buildStatusButton(
                          context,
                          'Complete',
                          Icons.check_circle_outline,
                          TaskStatus.completed,
                        ),
                      ],
                      if (task.status == TaskStatus.todo) ...[
                        const SizedBox(width: 8),
                        _buildStatusButton(
                          context,
                          'Start',
                          Icons.play_circle_outline,
                          TaskStatus.inProgress,
                        ),
                      ],
                      if (task.status == TaskStatus.inProgress) ...[
                        const SizedBox(width: 8),
                        _buildStatusButton(
                          context,
                          'Pause',
                          Icons.pause_circle_outline,
                          TaskStatus.todo,
                        ),
                      ],
                      if (task.status == TaskStatus.completed) ...[
                        _buildStatusButton(
                          context,
                          'Reopen',
                          Icons.replay,
                          TaskStatus.todo,
                        ),
                      ],
                      const SizedBox(width: 8),
                      IconButton(
                        icon: const Icon(Icons.delete_outline, color: Colors.red),
                        onPressed: onDeleteTask,
                        tooltip: 'Delete task',
                        iconSize: 20,
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildStatusIcon(BuildContext context) {
    IconData icon;
    Color color;
    
    switch (task.status) {
      case TaskStatus.todo:
        icon = Icons.circle_outlined;
        color = Colors.grey;
        break;
      case TaskStatus.inProgress:
        icon = Icons.play_circle;
        color = Colors.blue;
        break;
      case TaskStatus.completed:
        icon = Icons.check_circle;
        color = Colors.green;
        break;
    }
    
    return Icon(
      icon,
      color: color,
      size: 24,
    );
  }
  
  Widget _buildStatusButton(
    BuildContext context,
    String label,
    IconData icon,
    TaskStatus newStatus,
  ) {
    return InkWell(
      onTap: () => onStatusChanged(newStatus),
      child: Row(
        children: [
          Icon(
            icon,
            size: 16,
            color: Theme.of(context).colorScheme.primary,
          ),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              color: Theme.of(context).colorScheme.primary,
              fontSize: 12,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }
  
  Color _getPriorityColor(int priority) {
    switch (priority) {
      case 1:
        return Colors.green;
      case 2:
        return Colors.orange;
      case 3:
        return Colors.red;
      default:
        return Colors.grey;
    }
  }
  
  String _getPriorityText(int priority) {
    switch (priority) {
      case 1:
        return 'Low';
      case 2:
        return 'Medium';
      case 3:
        return 'High';
      default:
        return 'Unknown';
    }
  }
}
