import 'package:flutter/material.dart';
import 'package:focus_flow/models/task_model.dart';
import 'package:focus_flow/widgets/task_list_item.dart';
import 'package:provider/provider.dart';
import 'package:focus_flow/providers/task_provider.dart';

class TasksScreen extends StatefulWidget {
  const TasksScreen({super.key});

  @override
  State<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends State<TasksScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final TextEditingController _taskNameController = TextEditingController();
  final TextEditingController _taskDescriptionController = TextEditingController();
  final TextEditingController _estimatedPomodorosController = TextEditingController();
  int _taskPriority = 2; // Medium priority by default
  
  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    
    // Load tasks from storage
    WidgetsBinding.instance.addPostFrameCallback((_) {
      Provider.of<TaskProvider>(context, listen: false).loadTasks();
    });
  }
  
  @override
  void dispose() {
    _tabController.dispose();
    _taskNameController.dispose();
    _taskDescriptionController.dispose();
    _estimatedPomodorosController.dispose();
    super.dispose();
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          TabBar(
            controller: _tabController,
            labelColor: Theme.of(context).colorScheme.primary,
            unselectedLabelColor: Colors.grey,
            tabs: const [
              Tab(text: 'To Do'),
              Tab(text: 'In Progress'),
              Tab(text: 'Completed'),
            ],
            indicatorColor: Theme.of(context).colorScheme.primary,
          ),
          Expanded(
            child: Consumer<TaskProvider>(
              builder: (context, taskProvider, child) {
                return TabBarView(
                  controller: _tabController,
                  children: [
                    // To Do tasks
                    _buildTaskList(
                      taskProvider.getTasks(TaskStatus.todo),
                      taskProvider,
                      TaskStatus.todo,
                    ),
                    
                    // In Progress tasks
                    _buildTaskList(
                      taskProvider.getTasks(TaskStatus.inProgress),
                      taskProvider,
                      TaskStatus.inProgress,
                    ),
                    
                    // Completed tasks
                    _buildTaskList(
                      taskProvider.getTasks(TaskStatus.completed),
                      taskProvider,
                      TaskStatus.completed,
                    ),
                  ],
                );
              },
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddTaskDialog,
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
        child: const Icon(Icons.add),
      ),
    );
  }
  
  Widget _buildTaskList(List<Task> tasks, TaskProvider taskProvider, TaskStatus status) {
    if (tasks.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              _getEmptyStateIcon(status),
              size: 64,
              color: Colors.grey.shade300,
            ),
            const SizedBox(height: 16),
            Text(
              _getEmptyStateMessage(status),
              style: TextStyle(
                color: Colors.grey.shade600,
                fontSize: 16,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      );
    }
    
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: tasks.length,
      itemBuilder: (context, index) {
        final task = tasks[index];
        return TaskListItem(
          task: task,
          onStatusChanged: (newStatus) {
            taskProvider.updateTaskStatus(task.id, newStatus);
          },
          onDeleteTask: () {
            taskProvider.deleteTask(task.id);
          },
          onEditTask: () {
            _showEditTaskDialog(context, task);
          },
          onLinkTask: (String taskId) {
            final timerProvider = Provider.of<TimerProvider>(context, listen: false);
            timerProvider.linkTaskToSession(taskId);
            // Optionally, navigate to the Timer tab (assuming it's the first tab)
            // This depends on how your HomeScreen navigation is set up.
            // If HomeScreen has a TabController accessible via a Provider or static method:
            // DefaultTabController.of(context)?.animateTo(0);
            // Or if using a custom navigation service:
            // navigationService.navigateTo(AppRoutes.timerScreen);
            
            // For now, let's show a snackbar as confirmation
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Task "${task.name}" linked to focus session.'),
                duration: const Duration(seconds: 2),
              ),
            );
          },
        );
      },
    );
  }
  
  IconData _getEmptyStateIcon(TaskStatus status) {
    switch (status) {
      case TaskStatus.todo:
        return Icons.assignment;
      case TaskStatus.inProgress:
        return Icons.play_circle_outline;
      case TaskStatus.completed:
        return Icons.task_alt;
    }
  }
  
  String _getEmptyStateMessage(TaskStatus status) {
    switch (status) {
      case TaskStatus.todo:
        return 'No tasks to do yet.\nAdd a new task to get started!';
      case TaskStatus.inProgress:
        return 'No tasks in progress.\nStart working on a task from your To Do list!';
      case TaskStatus.completed:
        return 'No completed tasks yet.\nComplete tasks to see them here!';
    }
  }
  
  void _showAddTaskDialog() {
    // Reset fields for new task
    _taskNameController.clear();
    _taskDescriptionController.clear();
    _estimatedPomodorosController.text = '1'; // Default to 1 pomodoro
    _taskPriority = 2; // Reset to medium priority

    showDialog(
      context: context,
      builder: (BuildContext dialogContext) {
        // Use a StatefulWidget for the dialog content if complex state is needed inside the dialog
        // For now, _taskPriority is managed in _TasksScreenState, which is fine for this dialog's lifecycle.
        // If we had a _selectedPriorityInDialog, it would need a StatefulWidget or pass setState.
        return StatefulBuilder( // To manage priority selection within the dialog if needed, or just use a simpler dialog
          builder: (context, setStateDialog) {
            return AlertDialog(
              title: const Text('Add New Task'),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TextField(
                      controller: _taskNameController,
                      decoration: const InputDecoration(
                        labelText: 'Task Name',
                        hintText: 'Enter task name',
                      ),
                      maxLength: 50,
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _taskDescriptionController,
                      decoration: const InputDecoration(
                        labelText: 'Description (Optional)',
                        hintText: 'Enter task description',
                      ),
                      maxLines: 3,
                      maxLength: 200,
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _estimatedPomodorosController,
                      decoration: const InputDecoration(
                        labelText: 'Estimated Pomodoros',
                        hintText: 'e.g., 1, 2, 3...',
                      ),
                      keyboardType: TextInputType.number,
                    ),
                    const SizedBox(height: 16),
                    const Text('Priority'),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        _buildPriorityButton(
                          1, 
                          'Low', 
                          Colors.green, 
                          setStateDialog, // Use dialog's setState for priority buttons
                        ),
                        _buildPriorityButton(
                          2, 
                          'Medium', 
                          Colors.orange, 
                          setStateDialog,
                        ),
                        _buildPriorityButton(
                          3, 
                          'High', 
                          Colors.red, 
                          setStateDialog,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(dialogContext), // Use dialogContext
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () {
                    if (_taskNameController.text.trim().isNotEmpty) {
                      final taskProvider = Provider.of<TaskProvider>(
                        context, // Use the main build context for Provider
                        listen: false,
                      );
                      final int estimatedPomodoros = int.tryParse(_estimatedPomodorosController.text) ?? 1;
                      
                      taskProvider.addTask(
                        name: _taskNameController.text.trim(),
                        description: _taskDescriptionController.text.trim(),
                        priority: _taskPriority, // This _taskPriority is from _TasksScreenState
                        estimatedPomodoros: estimatedPomodoros > 0 ? estimatedPomodoros : 1, // Ensure positive
                      );
                      
                      Navigator.pop(dialogContext); // Use dialogContext
                    }
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.primary,
                    foregroundColor: Colors.white,
                  ),
                  child: const Text('Add Task'),
                ),
              ],
            );
          }
        );
      },
    );
  }
  
  void _showEditTaskDialog(BuildContext context, Task taskToEdit) {
    // Initialize controllers and priority with the task's current values
    _taskNameController.text = taskToEdit.name;
    _taskDescriptionController.text = taskToEdit.description;
    _estimatedPomodorosController.text = taskToEdit.estimatedPomodoros.toString();
    _taskPriority = taskToEdit.priority;

    showDialog(
      context: context,
      builder: (BuildContext dialogContext) {
        return StatefulBuilder(
          builder: (context, setStateDialog) {
            return AlertDialog(
              title: const Text('Edit Task'),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TextField(
                      controller: _taskNameController,
                      decoration: const InputDecoration(
                        labelText: 'Task Name',
                        hintText: 'Enter task name',
                      ),
                      maxLength: 50,
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _taskDescriptionController,
                      decoration: const InputDecoration(
                        labelText: 'Description (Optional)',
                        hintText: 'Enter task description',
                      ),
                      maxLines: 3,
                      maxLength: 200,
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _estimatedPomodorosController,
                      decoration: const InputDecoration(
                        labelText: 'Estimated Pomodoros',
                        hintText: 'e.g., 1, 2, 3...',
                      ),
                      keyboardType: TextInputType.number,
                    ),
                    const SizedBox(height: 16),
                    const Text('Priority'),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        _buildPriorityButton(
                          1,
                          'Low',
                          Colors.green,
                          setStateDialog,
                        ),
                        _buildPriorityButton(
                          2,
                          'Medium',
                          Colors.orange,
                          setStateDialog,
                        ),
                        _buildPriorityButton(
                          3,
                          'High',
                          Colors.red,
                          setStateDialog,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(dialogContext),
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () {
                    if (_taskNameController.text.trim().isNotEmpty) {
                      final taskProvider = Provider.of<TaskProvider>(
                        context, // Use the main build context for Provider
                        listen: false,
                      );
                      final int estimatedPomodoros = int.tryParse(_estimatedPomodorosController.text) ?? taskToEdit.estimatedPomodoros;

                      taskProvider.updateTaskDetails(
                        taskToEdit.id, // Pass the ID of the task being edited
                        name: _taskNameController.text.trim(),
                        description: _taskDescriptionController.text.trim(),
                        priority: _taskPriority, // _taskPriority from _TasksScreenState
                        estimatedPomodoros: estimatedPomodoros > 0 ? estimatedPomodoros : 1, // Ensure positive
                      );

                      Navigator.pop(dialogContext);
                    }
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.primary,
                    foregroundColor: Colors.white,
                  ),
                  child: const Text('Save Changes'),
                ),
              ],
            );
          },
        );
      },
    );
  }
  
  Widget _buildPriorityButton(
    int priority, 
    String label, 
    Color color, 
    Function(Function()) setStateCallback, // Changed to Function(Function())
  ) {
    return InkWell(
      onTap: () {
        // This setStateCallback is crucial. It should be the setState of the Dialog if state is local to dialog,
        // or _TasksScreenState.setState if managing priority selection state at screen level.
        // Current setup: _taskPriority is in _TasksScreenState. 
        // For the dialog's priority buttons to reflect changes immediately *within the dialog* before submit,
        // the dialog itself needs to be stateful or we pass the setState of a StatefulBuilder.
        setStateCallback(() { // Call the passed setState
          _taskPriority = priority; // This updates the _taskPriority in _TasksScreenState
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: _taskPriority == priority ? color : Colors.transparent,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: color,
            width: 1,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: _taskPriority == priority ? Colors.white : color,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }
}
