import 'package:flutter/foundation.dart';
import 'package:focus_flow/models/task_model.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';

class TaskProvider with ChangeNotifier {
  List<Task> _tasks = [];
  
  // Getters
  List<Task> get tasks => _tasks;
  
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
  
  // Add a new task
  void addTask({
    required String name,
    String description = '',
    int priority = 2, // Medium priority by default
  }) {
    final task = Task(
      name: name,
      description: description,
      priority: priority,
    );
    
    _tasks.add(task);
    _saveTasks();
    notifyListeners();
  }
  
  // Update task status
  void updateTaskStatus(String taskId, TaskStatus newStatus) {
    final index = _tasks.indexWhere((task) => task.id == taskId);
    
    if (index != -1) {
      _tasks[index] = _tasks[index].copyWith(status: newStatus);
      _saveTasks();
      notifyListeners();
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
  
  // Update task details
  void updateTaskDetails(String taskId, {String? name, String? description}) {
    final index = _tasks.indexWhere((task) => task.id == taskId);
    
    if (index != -1) {
      _tasks[index] = _tasks[index].copyWith(
        name: name,
        description: description,
      );
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
      final tasksJson = prefs.getStringList('tasks') ?? [];
      
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
      
      await prefs.setStringList('tasks', tasksJson);
    } catch (e) {
      debugPrint('Error saving tasks: $e');
    }
  }
}
