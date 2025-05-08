import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/garden_provider.dart';
import '../models/plant_model.dart';

class GardenScreen extends StatelessWidget {
  const GardenScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Virtual Garden'),
      ),
      body: Consumer<GardenProvider>(
        builder: (context, gardenProvider, child) {
          final plants = gardenProvider.availablePlants;

          if (plants.isEmpty) {
            return const Center(
              child: Text('Your garden is looking a bit empty. Keep focusing to unlock new plants!'),
            );
          }

          return ListView.builder(
            itemCount: plants.length,
            itemBuilder: (context, index) {
              final plant = plants[index];
              return Card(
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: ListTile(
                  leading: plant.imageUrl.isNotEmpty 
                      ? Image.asset(plant.imageUrl, width: 50, height: 50, fit: BoxFit.cover)
                      : const Icon(Icons.local_florist, size: 40),
                  title: Text(plant.name),
                  subtitle: Text(plant.description),
                  // You could add a trailing widget or onTap later for more interactivity
                ),
              );
            },
          );
        },
      ),
    );
  }
}
