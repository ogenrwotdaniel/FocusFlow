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
    // Reset form fields
    _taskNameController.clear();
    _taskDescriptionController.clear();
    _taskPriority = 2;
    
    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setState) {
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
                    const Text('Priority'),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        _buildPriorityButton(
                          1, 
                          'Low', 
                          Colors.green, 
                          setState,
                        ),
                        _buildPriorityButton(
                          2, 
                          'Medium', 
                          Colors.orange, 
                          setState,
                        ),
                        _buildPriorityButton(
                          3, 
                          'High', 
                          Colors.red, 
                          setState,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () {
                    if (_taskNameController.text.trim().isNotEmpty) {
                      final taskProvider = Provider.of<TaskProvider>(
                        context, 
                        listen: false,
                      );
                      
                      taskProvider.addTask(
                        name: _taskNameController.text.trim(),
                        description: _taskDescriptionController.text.trim(),
                        priority: _taskPriority,
                      );
                      
                      Navigator.pop(context);
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
          },
        );
      },
    );
  }
  
  Widget _buildPriorityButton(
    int priority, 
    String label, 
    Color color, 
    Function(Function) setState,
  ) {
    return InkWell(
      onTap: () {
        setState(() {
          _taskPriority = priority;
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
