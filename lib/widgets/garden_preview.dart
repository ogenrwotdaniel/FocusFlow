import 'package:flutter/material.dart';
import 'package:focus_flow/providers/garden_provider.dart'; 
import 'package:focus_flow/models/plant_model.dart'; 

/// Simple placeholder widget that shows a horizontal list of the most
/// recently planted/unlocked plants and the number of un-spent points (concept).
class GardenPreview extends StatelessWidget {
  final List<Plant> plants; 
  // final int availablePoints; 

  const GardenPreview({
    super.key,
    required this.plants,
    // required this.availablePoints,
  });

  @override
  Widget build(BuildContext context) {
    if (plants.isEmpty) {
      return const Center(
        child: Text('Your garden is waiting to grow!'),
      );
    }
    return SizedBox(
      height: 120, 
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        itemCount: plants.length,
        itemBuilder: (context, index) {
          return _GardenItem(plant: plants[index]); 
        },
      ),
    );
  }
}

class _GardenItem extends StatelessWidget {
  final Plant plant; 

  const _GardenItem({required this.plant});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 4.0),
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            plant.imageUrl.isNotEmpty && plant.imageUrl.startsWith('assets/')
                ? Image.asset(plant.imageUrl, width: 40, height: 40, fit: BoxFit.contain, errorBuilder: (context, error, stackTrace) => const Icon(Icons.eco, size: 30))
                : const Icon(Icons.eco, size: 30), 
            const SizedBox(height: 4),
            Text(
              plant.name,
              style: Theme.of(context).textTheme.bodySmall,
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}
