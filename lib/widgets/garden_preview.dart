import 'package:flutter/material.dart';
import 'package:focus_flow/providers/garden_provider.dart';

/// Simple placeholder widget that shows a horizontal list of the most
/// recently planted trees and the number of un‑spent points.
///
/// This keeps the build working; you can replace it later with a fancier
/// graphic garden representation.
class GardenPreview extends StatelessWidget {
  final List<Tree> trees;
  final int availablePoints;

  const GardenPreview({
    super.key,
    required this.trees,
    required this.availablePoints,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 1,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            Expanded(
              child: SizedBox(
                height: 80,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: trees.length,
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemBuilder: (context, index) {
                    final tree = trees[index];
                    return _TreeAvatar(tree: tree);
                  },
                ),
              ),
            ),
            const SizedBox(width: 12),
            Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.local_florist, color: Colors.green),
                Text('$availablePoints pts',
                    style: Theme.of(context).textTheme.labelMedium),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _TreeAvatar extends StatelessWidget {
  final Tree tree;
  const _TreeAvatar({required this.tree});

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        CircleAvatar(
          radius: 22,
          backgroundColor: Colors.green.shade400,
          child: Text(
            _typeToEmoji(tree.type),
            style: const TextStyle(fontSize: 20),
          ),
        ),
        const SizedBox(height: 4),
        Text('Lv.${tree.growthStage}',
            style: const TextStyle(fontSize: 10, color: Colors.grey)),
      ],
    );
  }

  String _typeToEmoji(TreeType type) {
    switch (type) {
      case TreeType.oak:
        return '🌳';
      case TreeType.pine:
        return '🌲';
      case TreeType.cherry:
        return '🌸';
      case TreeType.maple:
        return '🍁';
    }
  }
}
