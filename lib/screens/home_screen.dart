import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:focus_flow/providers/session_provider.dart';
import 'package:focus_flow/providers/stats_provider.dart';
import 'package:focus_flow/providers/garden_provider.dart';
import 'package:focus_flow/screens/timer_screen.dart';
import 'package:focus_flow/screens/stats_screen.dart';
import 'package:focus_flow/screens/settings_screen.dart';
import 'package:focus_flow/widgets/session_list_item.dart';
import 'package:focus_flow/widgets/garden_preview.dart';
import 'package:intl/intl.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;
  late final List<Widget> _screens;

  @override
  void initState() {
    super.initState();
    _screens = [
      const TasksTab(),
      const TimerScreen(),
      const StatsScreen(),
      const SettingsScreen(),
    ];
    
    _loadData();
  }
  
  Future<void> _loadData() async {
    final sessionProvider = Provider.of<SessionProvider>(context, listen: false);
    await sessionProvider.loadSessions();
  }

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
        title: const Text('Focus Flow'),
      ),
      body: _screens[_selectedIndex],
      bottomNavigationBar: BottomNavigationBar(
        type: BottomNavigationBarType.fixed,
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(
            icon: Icon(Icons.task),
            label: 'Tasks',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.timer),
            label: 'Timer',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.bar_chart),
            label: 'Stats',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings),
            label: 'Settings',
          ),
        ],
        currentIndex: _selectedIndex,
        selectedItemColor: Theme.of(context).colorScheme.primary,
        unselectedItemColor: Colors.grey,
        onTap: _onItemTapped,
      ),
    );
  }
}

class TasksTab extends StatelessWidget {
  const TasksTab({super.key});

  @override
  Widget build(BuildContext context) {
    final sessionProvider = Provider.of<SessionProvider>(context);
    final statsProvider = Provider.of<StatsProvider>(context);
    final gardenProvider = Provider.of<GardenProvider>(context);
    
    // Format today's focus time
    final todayFocusMinutes = sessionProvider.getTodayFocusTime();
    final hours = todayFocusMinutes ~/ 60;
    final minutes = todayFocusMinutes % 60;
    final focusTimeFormatted = hours > 0 
        ? '$hours hr ${minutes > 0 ? '$minutes min' : ''}' 
        : '$minutes min';
    
    return SingleChildScrollView(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Stats overview
            Card(
              elevation: 2,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        Column(
                          children: [
                            const Text(
                              'Today\'s Focus',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.grey,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              focusTimeFormatted,
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        Column(
                          children: [
                            const Text(
                              'Current Streak',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.grey,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '${statsProvider.currentStreak} days',
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Garden preview
            const Text(
              'Your Garden',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              height: 120,
              child: GardenPreview(
                trees: gardenProvider.recentTrees,
                availablePoints: gardenProvider.unspentPoints,
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Recent sessions
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Recent Sessions',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  'Total: ${sessionProvider.completedSessions.length}',
                  style: const TextStyle(
                    color: Colors.grey,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            sessionProvider.recentSessions.isEmpty
                ? const Center(
                    child: Padding(
                      padding: EdgeInsets.all(16.0),
                      child: Text(
                        'No recent sessions yet.\nStart a focus session to begin tracking your productivity!',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.grey),
                      ),
                    ),
                  )
                : ListView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: sessionProvider.recentSessions.length,
                    itemBuilder: (context, index) {
                      final session = sessionProvider.recentSessions[index];
                      return SessionListItem(session: session);
                    },
                  ),
            
            const SizedBox(height: 24),
            
            // Start focus button
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).colorScheme.primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
                onPressed: () {
                  // Navigate to Timer screen directly
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const TimerScreen()),
                  );
                },
                child: const Text(
                  'Start Focus Session',
                  style: TextStyle(fontSize: 16),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
