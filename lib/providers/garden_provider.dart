import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/plant_model.dart';
import './achievement_provider.dart';

class GardenProvider with ChangeNotifier {
  final AchievementProvider _achievementProvider;
  List<Plant> _availablePlants = [];

  static final List<Plant> _allPossiblePlants = [
    Plant(
      id: 'plant_rose_bud',
      name: 'Rose Bud',
      description: 'A lovely rose, a sign of new beginnings.',
      imageUrl: 'assets/images/plants/rose_bud.png',
      requiredAchievementId: null,
    ),
    Plant(
      id: 'plant_sunflower_seedling',
      name: 'Sunflower Seedling',
      description: 'Represents your first focused effort!',
      imageUrl: 'assets/images/plants/sunflower_seedling.png',
      requiredAchievementId: 'focus_sessions_1',
    ),
    Plant(
      id: 'plant_cactus_small',
      name: 'Small Cactus',
      description: 'Thriving with consistency, like your task completion!',
      imageUrl: 'assets/images/plants/cactus_small.png',
      requiredAchievementId: 'tasks_completed_5',
    ),
    Plant(
      id: 'plant_bonsai_sapling',
      name: 'Bonsai Sapling',
      description: 'A symbol of patience and dedication, earned through streaks.',
      imageUrl: 'assets/images/plants/bonsai_sapling.png',
      requiredAchievementId: 'streak_3_days',
    ),
  ];

  List<Plant> get availablePlants => _availablePlants;

  GardenProvider(this._achievementProvider) {
    _achievementProvider.addListener(_updateAvailablePlants);
    _updateAvailablePlants();
  }

  @override
  void dispose() {
    _achievementProvider.removeListener(_updateAvailablePlants);
    super.dispose();
  }

  void _updateAvailablePlants() {
    final unlockedAchievementIds = _achievementProvider.unlockedAchievements.map((a) => a.id).toSet();
    
    _availablePlants = _allPossiblePlants.where((plant) {
      if (plant.requiredAchievementId == null) {
        return true;
      }
      return unlockedAchievementIds.contains(plant.requiredAchievementId);
    }).toList();
    
    notifyListeners();
  }

  Future<void> resetGarden() async {
    _updateAvailablePlants();
  }
}
