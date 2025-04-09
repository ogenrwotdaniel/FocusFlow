import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'package:uuid/uuid.dart';

class GardenProvider with ChangeNotifier {
  List<Tree> _trees = [];
  int _totalPointsEarned = 0;
  final Uuid _uuid = const Uuid();

  // Getters
  List<Tree> get trees => _trees;
  List<Tree> get recentTrees => _trees.take(3).toList();
  int get totalPointsEarned => _totalPointsEarned;
  int get unspentPoints => _totalPointsEarned - _trees.fold(0, (sum, tree) => sum + tree.pointsCost);

  GardenProvider() {
    loadGarden();
  }

  // Add focus points based on completed session
  Future<void> addFocusPoints(int focusMinutes) async {
    // Each focus minute = 1 point
    final pointsEarned = focusMinutes;
    _totalPointsEarned += pointsEarned;
    
    await _saveGarden();
    notifyListeners();
  }

  // Plant a new tree
  Future<Tree?> plantTree(TreeType type) async {
    final pointsCost = _getTreeCost(type);
    
    if (unspentPoints < pointsCost) {
      return null; // Not enough points
    }
    
    final tree = Tree(
      id: _uuid.v4(),
      type: type,
      pointsCost: pointsCost,
      plantedDate: DateTime.now(),
      growthStage: 1,
    );
    
    _trees.add(tree);
    await _saveGarden();
    notifyListeners();
    
    return tree;
  }

  // Grow a tree to next stage
  Future<bool> growTree(String treeId) async {
    final treeIndex = _trees.indexWhere((tree) => tree.id == treeId);
    
    if (treeIndex == -1) {
      return false; // Tree not found
    }
    
    final tree = _trees[treeIndex];
    final growthCost = _getGrowthCost(tree.type, tree.growthStage);
    
    if (unspentPoints < growthCost) {
      return false; // Not enough points
    }
    
    if (tree.growthStage >= 3) {
      return false; // Already fully grown
    }
    
    _trees[treeIndex] = tree.copyWith(
      growthStage: tree.growthStage + 1,
    );
    
    await _saveGarden();
    notifyListeners();
    
    return true;
  }

  // Get the cost of a tree based on type
  int _getTreeCost(TreeType type) {
    switch (type) {
      case TreeType.oak:
        return 30;
      case TreeType.pine:
        return 25;
      case TreeType.cherry:
        return 40;
      case TreeType.maple:
        return 35;
      default:
        return 25;
    }
  }

  // Get the cost to grow a tree to the next stage
  int _getGrowthCost(TreeType type, int currentStage) {
    final baseCost = _getTreeCost(type) / 2;
    return (baseCost * currentStage).round();
  }

  // Load garden from shared preferences
  Future<void> loadGarden() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      _totalPointsEarned = prefs.getInt('totalPointsEarned') ?? 0;
      
      final treesJson = prefs.getStringList('trees') ?? [];
      _trees = treesJson
          .map((json) => Tree.fromMap(jsonDecode(json)))
          .toList();
      
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading garden: $e');
    }
  }

  // Save garden to shared preferences
  Future<void> _saveGarden() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      await prefs.setInt('totalPointsEarned', _totalPointsEarned);
      
      final treesJson = _trees
          .map((tree) => jsonEncode(tree.toMap()))
          .toList();
      
      await prefs.setStringList('trees', treesJson);
    } catch (e) {
      debugPrint('Error saving garden: $e');
    }
  }

  // Reset garden
  Future<void> resetGarden() async {
    _trees = [];
    _totalPointsEarned = 0;
    
    await _saveGarden();
    notifyListeners();
  }
}

// Tree model
class Tree {
  final String id;
  final TreeType type;
  final int pointsCost;
  final DateTime plantedDate;
  final int growthStage; // 1-3

  Tree({
    required this.id,
    required this.type,
    required this.pointsCost,
    required this.plantedDate,
    this.growthStage = 1,
  });

  // Copy with
  Tree copyWith({
    String? id,
    TreeType? type,
    int? pointsCost,
    DateTime? plantedDate,
    int? growthStage,
  }) {
    return Tree(
      id: id ?? this.id,
      type: type ?? this.type,
      pointsCost: pointsCost ?? this.pointsCost,
      plantedDate: plantedDate ?? this.plantedDate,
      growthStage: growthStage ?? this.growthStage,
    );
  }

  // To map
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'type': type.index,
      'pointsCost': pointsCost,
      'plantedDate': plantedDate.millisecondsSinceEpoch,
      'growthStage': growthStage,
    };
  }

  // From map
  factory Tree.fromMap(Map<String, dynamic> map) {
    return Tree(
      id: map['id'],
      type: TreeType.values[map['type']],
      pointsCost: map['pointsCost'],
      plantedDate: DateTime.fromMillisecondsSinceEpoch(map['plantedDate']),
      growthStage: map['growthStage'],
    );
  }
}

// Tree types
enum TreeType {
  oak,
  pine,
  cherry,
  maple,
}
