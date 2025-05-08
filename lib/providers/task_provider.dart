import 'package:flutter/foundation.dart';
import 'package:focus_flow/models/task_model.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'package:focus_flow/providers/stats_provider.dart';

class TaskProvider with ChangeNotifier {
  List<Task> _tasks = [];
  static const String _tasksKey = 'tasks_list_v3'; // Incremented for new structure

  StatsProvider? _statsProvider; // New field

  TaskProvider() {
    loadTasks();
  }

  // Method to set StatsProvider, will be called by ChangeNotifierProxyProvider
  void setStatsProvider(StatsProvider provider) {
    _statsProvider = provider;
  }

  // Getters
  List<Task> get tasks => List.unmodifiable(_tasks);

  Task? getTaskById(String taskId) {
    try {
      return _tasks.firstWhere((task) => task.id == taskId);
    } catch (e) {
      return null; // Return null if no task is found with that id
    }
  }
  
  // Get tasks by status
  List<Task> getTasks(TaskStatus status) {
    return _tasks
        .where((task) => task.status == status)
        .toList()
        ..sort((a, b) {
          // Sort by priority (high to low), then by creation date (newest first)
          final priorityCompare = b.priority.compareTo(a.priority);
          if (priorityCompare != 0) return priorityCompare;
          return b.createdAt.compareTo(a.createdAt);
        });
  }
  
  // Get next suggested task
  Task? getNextSuggestedTask({String? currentProjectId}) {
    List<Task> todoTasks = _tasks.where((task) => task.status == TaskStatus.todo).toList();

    if (currentProjectId != null) {
      todoTasks = todoTasks.where((task) => task.projectId == currentProjectId).toList();
    }

    if (todoTasks.isEmpty) {
      return null;
    }

    // Sort by priority (highest first), then by creation date (oldest first)
    todoTasks.sort((a, b) {
      final priorityCompare = b.priority.compareTo(a.priority); // Higher priority value means higher priority
      if (priorityCompare != 0) return priorityCompare;
      return a.createdAt.compareTo(b.createdAt); // Oldest (earlier createdAt) comes first
    });

    return todoTasks.first;
  }
  
  // Add a new task
  Future<void> addTask({
    required String name,
    String description = '',
    int priority = 2, // Medium priority by default
    int estimatedPomodoros = 1, // New parameter with default
  }) async {
    final task = Task(
      name: name,
      description: description,
      priority: priority,
      estimatedPomodoros: estimatedPomodoros, // Use the new parameter
    );
    
    _tasks.add(task);
    await _saveTasks();
    notifyListeners();
  }
  
  // Update task status
  void updateTaskStatus(String taskId, TaskStatus newStatus) {
    final taskIndex = _tasks.indexWhere((task) => task.id == taskId);
    if (taskIndex != -1) {
      Task task = _tasks[taskIndex];
      TaskStatus oldStatus = task.status; // Store old status

      task = task.copyWith(
        status: newStatus,
        completedAt: newStatus == TaskStatus.completed ? DateTime.now() : null,
      );
      _tasks[taskIndex] = task;
      _saveTasks();
      notifyListeners();

      // If task is newly completed, increment stats
      if (newStatus == TaskStatus.completed && oldStatus != TaskStatus.completed) {
        _statsProvider?.incrementTasksCompleted();
      }
    } else {
      print('Task with ID $taskId not found for status update.');
    }
  }
  
  // Update task priority
  void updateTaskPriority(String taskId, int newPriority) {
    final index = _tasks.indexWhere((task) => task.id == taskId);
    
    if (index != -1) {
      _tasks[index] = _tasks[index].copyWith(priority: newPriority);
      _saveTasks();
      notifyListeners();
    }
  }
  
  // Update task details, including pomodoro estimates
  void updateTaskDetails(String taskId, {String? name, String? description, int? estimatedPomodoros}) {
    final index = _tasks.indexWhere((task) => task.id == taskId);
    
    if (index != -1) {
      _tasks[index] = _tasks[index].copyWith(
        name: name,
        description: description,
        estimatedPomodoros: estimatedPomodoros,
      );
      _saveTasks();
      notifyListeners();
    }
  }
  
  // Increment completed pomodoros for a task
  void incrementCompletedPomodoros(String taskId) {
    final index = _tasks.indexWhere((task) => task.id == taskId);
    if (index != -1) {
      final task = _tasks[index];
      // Ensure we don't increment beyond estimated, though this might be a flexible rule
      final newCompletedPomodoros = task.completedPomodoros + 1;
      // Optional: Check if newCompletedPomodoros exceeds estimatedPomodoros and handle if needed
      // e.g., if (newCompletedPomodoros > task.estimatedPomodoros && task.estimatedPomodoros > 0) { ... }

      final updatedTask = task.copyWith(completedPomodoros: newCompletedPomodoros);
      _tasks[index] = updatedTask;
      _saveTasks();
      notifyListeners();
    }
  }
  
  // Delete a task
  void deleteTask(String taskId) {
    _tasks.removeWhere((task) => task.id == taskId);
    _saveTasks();
    notifyListeners();
  }
  
  // Load tasks from SharedPreferences
  Future<void> loadTasks() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final tasksJson = prefs.getStringList(_tasksKey) ?? [];
      
      _tasks = tasksJson
          .map((json) => Task.fromMap(jsonDecode(json)))
          .toList();
      
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading tasks: $e');
    }
  }
  
  // Save tasks to SharedPreferences
  Future<void> _saveTasks() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final tasksJson = _tasks
          .map((task) => jsonEncode(task.toMap()))
          .toList();
      
      await prefs.setStringList(_tasksKey, tasksJson);
    } catch (e) {
      debugPrint('Error saving tasks: $e');
    }
  }
}
