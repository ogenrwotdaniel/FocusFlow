import 'package:flutter/material.dart';
import 'package:focus_flow/models/achievement_model.dart';
import 'package:focus_flow/providers/achievement_provider.dart';
import 'package:focus_flow/providers/stats_provider.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';

class AchievementsScreen extends StatelessWidget {
  const AchievementsScreen({Key? key}) : super(key: key);
  static const String routeName = '/achievements';

  // Helper to map iconName string to actual IconData
  IconData _getIconForName(String iconName) {
    switch (iconName) {
      case 'star_border':
        return Icons.star_border;
      case 'star_half':
        return Icons.star_half;
      case 'checklist_rtl':
        return Icons.checklist_rtl_outlined;
      case 'flame_on':
        return Icons.local_fire_department;
      // Add more mappings as you define more achievements
      default:
        return Icons.emoji_events;
    }
  }

  @override
  Widget build(BuildContext context) {
    final achievementProvider = Provider.of<AchievementProvider>(context);
    final statsProvider = Provider.of<StatsProvider>(context);

    final allAchievements = achievementProvider.allAchievements;
    allAchievements.sort((a, b) {
      if (a.isUnlocked && !b.isUnlocked) return -1;
      if (!a.isUnlocked && b.isUnlocked) return 1;
      return a.id.compareTo(b.id);
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Achievements & Streaks'),
        elevation: 0,
        backgroundColor: Theme.of(context).scaffoldBackgroundColor,
        foregroundColor: Theme.of(context).textTheme.titleLarge?.color,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Reset All Stats & Achievements (Dev)',
            onPressed: () {
              Provider.of<AchievementProvider>(context, listen: false).resetAllAchievements();
              Provider.of<StatsProvider>(context, listen: false).resetStats();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('All achievements and stats reset.')),
              );
            },
          )
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildStreakSection(context, statsProvider),
            const SizedBox(height: 24),
            Text(
              'Your Achievements',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            if (allAchievements.isEmpty)
              const Center(child: Text('No achievements defined yet.'))
            else
              ListView.separated(
                physics: const NeverScrollableScrollPhysics(),
                shrinkWrap: true,
                itemCount: allAchievements.length,
                itemBuilder: (context, index) {
                  return _AchievementListItem(achievement: allAchievements[index], getIconForName: _getIconForName);
                },
                separatorBuilder: (context, index) => const SizedBox(height: 12),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildStreakSection(BuildContext context, StatsProvider statsProvider) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            _StreakInfo(
              label: 'Current Streak',
              value: '${statsProvider.currentStreak} Days',
              icon: Icons.local_fire_department_rounded,
              color: Colors.orangeAccent,
            ),
            _StreakInfo(
              label: 'Longest Streak',
              value: '${statsProvider.longestStreak} Days',
              icon: Icons.star_rate_rounded,
              color: Colors.amber,
            ),
          ],
        ),
      ),
    );
  }
}

class _StreakInfo extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final Color color;

  const _StreakInfo({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Icon(icon, size: 36, color: color),
        const SizedBox(height: 8),
        Text(
          value,
          style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold, color: color),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}

class _AchievementListItem extends StatelessWidget {
  final Achievement achievement;
  final IconData Function(String) getIconForName;

  const _AchievementListItem({required this.achievement, required this.getIconForName});

  @override
  Widget build(BuildContext context) {
    final bool isUnlocked = achievement.isUnlocked;
    final IconData displayIconData = getIconForName(achievement.iconName);

    final Color iconColor = isUnlocked
        ? Theme.of(context).colorScheme.primary
        : Colors.grey.shade500;

    final Color titleColor = isUnlocked
        ? Theme.of(context).textTheme.bodyLarge!.color!
        : Colors.grey.shade700;

    final Color subtitleColor = isUnlocked
        ? Theme.of(context).textTheme.bodyMedium!.color!.withOpacity(0.8)
        : Colors.grey.shade500;

    final Color tileBackgroundColor = isUnlocked
        ? (Theme.of(context).brightness == Brightness.light ? Colors.green.shade50 : Colors.green.shade900.withOpacity(0.4))
        : (Theme.of(context).brightness == Brightness.light ? Colors.grey.shade200 : Colors.grey.shade800);

    return Card(
      elevation: isUnlocked ? 2.5 : 0.5,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      color: tileBackgroundColor,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(vertical: 10, horizontal: 16),
        leading: CircleAvatar(
          radius: 24,
          backgroundColor: iconColor.withOpacity(isUnlocked ? 0.15 : 0.1),
          child: Icon(displayIconData, color: iconColor, size: 26),
        ),
        title: Text(
          achievement.name,
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: titleColor,
          ),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 4),
            Text(
              achievement.description,
              style: TextStyle(fontSize: 13, color: subtitleColor),
            ),
            if (isUnlocked && achievement.unlockedAt != null) ...[
              const SizedBox(height: 6),
              Text(
                'Unlocked: ${DateFormat.yMMMd().add_jm().format(achievement.unlockedAt!)}',
                style: TextStyle(
                  fontSize: 11,
                  fontStyle: FontStyle.italic,
                  color: isUnlocked ? (Theme.of(context).brightness == Brightness.light ? Colors.green.shade800 : Colors.green.shade300) : subtitleColor,
                ),
              ),
            ],
          ],
        ),
        trailing: isUnlocked
            ? Icon(Icons.check_circle, color: (Theme.of(context).brightness == Brightness.light ? Colors.green.shade600 : Colors.green.shade400), size: 28)
            : Icon(Icons.lock_outline, color: Colors.grey.shade400, size: 28),
      ),
    );
  }
}
