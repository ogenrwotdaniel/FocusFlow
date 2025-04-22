import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:focus_flow/providers/stats_provider.dart';
import 'package:focus_flow/providers/session_provider.dart';
import 'package:intl/intl.dart';
import 'package:fl_chart/fl_chart.dart';

class StatsScreen extends StatelessWidget {
  const StatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          toolbarHeight: 0,
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Overview'),
              Tab(text: 'Weekly Stats'),
            ],
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white70,
            indicatorColor: Colors.white,
          ),
        ),
        body: const TabBarView(
          children: [
            StatsOverviewTab(),
            WeeklyStatsTab(),
          ],
        ),
      ),
    );
  }
}

class StatsOverviewTab extends StatelessWidget {
  const StatsOverviewTab({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer2<StatsProvider, SessionProvider>(
      builder: (context, statsProvider, sessionProvider, child) {
        // Format total focus time
        final totalMinutes = statsProvider.totalFocusMinutes;
        final hours = totalMinutes ~/ 60;
        final minutes = totalMinutes % 60;
        
        final totalFocusFormatted = hours > 0 
            ? '$hours hr${hours > 1 ? 's' : ''} ${minutes > 0 ? '$minutes min' : ''}' 
            : '$minutes min';
        
        // Format average daily focus time
        final avgDaily = statsProvider.getAverageDailyFocusTime().toInt();
        final avgHours = avgDaily ~/ 60;
        final avgMinutes = avgDaily % 60;
        
        final avgFocusFormatted = avgHours > 0 
            ? '$avgHours hr${avgHours > 1 ? 's' : ''} ${avgMinutes > 0 ? '$avgMinutes min' : ''}' 
            : '$avgMinutes min';

        return SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Stat cards
                StatsCard(
                  title: 'Total Focus Time',
                  value: totalFocusFormatted,
                  icon: Icons.timer,
                  color: Theme.of(context).colorScheme.primary,
                ),
                
                const SizedBox(height: 16),
                
                Row(
                  children: [
                    Expanded(
                      child: StatsCard(
                        title: 'Sessions',
                        value: '${sessionProvider.completedSessions.length}',
                        icon: Icons.event_available,
                        color: Colors.orange,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: StatsCard(
                        title: 'Current Streak',
                        value: '${statsProvider.currentStreak} days',
                        icon: Icons.local_fire_department,
                        color: Colors.red,
                      ),
                    ),
                  ],
                ),
                
                const SizedBox(height: 16),
                
                Row(
                  children: [
                    Expanded(
                      child: StatsCard(
                        title: 'Longest Streak',
                        value: '${statsProvider.longestStreak} days',
                        icon: Icons.emoji_events,
                        color: Colors.amber,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: StatsCard(
                        title: 'Daily Average',
                        value: avgFocusFormatted,
                        icon: Icons.calendar_today,
                        color: Colors.teal,
                      ),
                    ),
                  ],
                ),
                
                const SizedBox(height: 32),
                
                // Recent activity calendar
                const Text(
                  'Recent Activity',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                
                const SizedBox(height: 16),
                
                const ActivityCalendar(),
                
                const SizedBox(height: 32),
                
                // Achievements
                const Text(
                  'Achievements',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                
                const SizedBox(height: 16),
                
                AchievementItem(
                  title: 'Beginner Focuser',
                  description: 'Complete 5 focus sessions',
                  isUnlocked: sessionProvider.completedSessions.length >= 5,
                ),
                
                AchievementItem(
                  title: 'Consistency Master',
                  description: 'Maintain a 3-day streak',
                  isUnlocked: statsProvider.currentStreak >= 3,
                ),
                
                AchievementItem(
                  title: 'Focus Expert',
                  description: 'Accumulate 5 hours of focus time',
                  isUnlocked: statsProvider.totalFocusMinutes >= 300,
                ),
                
                AchievementItem(
                  title: 'Focus Guru',
                  description: 'Complete 50 focus sessions',
                  isUnlocked: sessionProvider.completedSessions.length >= 50,
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class WeeklyStatsTab extends StatelessWidget {
  const WeeklyStatsTab({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<StatsProvider>(
      builder: (context, statsProvider, child) {
        final weeklyData = statsProvider.getWeeklyFocusTime();
        
        return SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Chart title
                const Text(
                  'This Week\'s Focus Minutes',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                
                const SizedBox(height: 24),
                
                // Bar chart for weekly data
                SizedBox(
                  height: 250,
                  child: BarChart(
                    BarChartData(
                      alignment: BarChartAlignment.spaceAround,
                      maxY: _getMaxValue(weeklyData.values.toList()) + 10,
                      barTouchData: BarTouchData(
                        enabled: true,
                        touchTooltipData: BarTouchTooltipData(
                          getTooltipColor: (_, __, ___) => Colors.blueGrey,
                          getTooltipItem: (group, groupIndex, rod, rodIndex) {
                            final dayName = weeklyData.keys.elementAt(groupIndex);
                            final minutes = weeklyData.values.elementAt(groupIndex);
                            return BarTooltipItem(
                              '$dayName: $minutes mins',
                              const TextStyle(color: Colors.white),
                            );
                          },
                        ),
                      ),
                      titlesData: FlTitlesData(
                        show: true,
                        bottomTitles: AxisTitles(
                          sideTitles: SideTitles(
                            showTitles: true,
                            getTitlesWidget: (value, meta) {
                              final index = value.toInt();
                              if (index >= 0 && index < weeklyData.length) {
                                return Padding(
                                  padding: const EdgeInsets.only(top: 8.0),
                                  child: Text(
                                    weeklyData.keys.elementAt(index).substring(0, 1),
                                    style: const TextStyle(
                                      color: Colors.grey,
                                      fontWeight: FontWeight.bold,
                                      fontSize: 14,
                                    ),
                                  ),
                                );
                              }
                              return const Text('');
                            },
                          ),
                        ),
                        leftTitles: AxisTitles(
                          sideTitles: SideTitles(
                            showTitles: true,
                            reservedSize: 30,
                            interval: 30,
                            getTitlesWidget: (value, meta) {
                              if (value == 0) {
                                return const Text('');
                              }
                              return Padding(
                                padding: const EdgeInsets.only(right: 8.0),
                                child: Text(
                                  value.toInt().toString(),
                                  style: const TextStyle(
                                    color: Colors.grey,
                                    fontSize: 12,
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                        topTitles: AxisTitles(
                          sideTitles: SideTitles(showTitles: false),
                        ),
                        rightTitles: AxisTitles(
                          sideTitles: SideTitles(showTitles: false),
                        ),
                      ),
                      gridData: FlGridData(
                        show: true,
                        drawVerticalLine: false,
                        horizontalInterval: 30,
                        getDrawingHorizontalLine: (value) {
                          return FlLine(
                            color: Colors.grey.shade300,
                            strokeWidth: 1,
                          );
                        },
                      ),
                      borderData: FlBorderData(
                        show: false,
                      ),
                      barGroups: _getBarGroups(weeklyData, context),
                    ),
                  ),
                ),
                
                const SizedBox(height: 48),
                
                // Current week summary
                Card(
                  elevation: 2,
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'This Week\'s Summary',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 16),
                        _buildSummaryRow(
                          'Total Focus Time', 
                          _formatMinutes(weeklyData.values.fold(0, (sum, mins) => sum + mins)),
                        ),
                        const Divider(),
                        _buildSummaryRow(
                          'Daily Average', 
                          _formatMinutes(weeklyData.values.fold(0, (sum, mins) => sum + mins) ~/ 7),
                        ),
                        const Divider(),
                        _buildSummaryRow(
                          'Most Productive Day', 
                          _getMostProductiveDay(weeklyData),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
  
  double _getMaxValue(List<int> values) {
    if (values.isEmpty) return 60; // Default max if no data
    return values.reduce((curr, next) => curr > next ? curr : next).toDouble();
  }
  
  List<BarChartGroupData> _getBarGroups(Map<String, int> data, BuildContext context) {
    final color = Theme.of(context).colorScheme.primary;
    
    return List.generate(data.length, (index) {
      final minutes = data.values.elementAt(index);
      return BarChartGroupData(
        x: index,
        barRods: [
          BarChartRodData(
            toY: minutes.toDouble(),
            color: color,
            width: 20,
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(6),
              topRight: Radius.circular(6),
            ),
          ),
        ],
      );
    });
  }
  
  String _formatMinutes(int minutes) {
    final hours = minutes ~/ 60;
    final mins = minutes % 60;
    
    if (hours > 0) {
      return '$hours hr${hours > 1 ? 's' : ''} ${mins > 0 ? '$mins min' : ''}';
    } else {
      return '$mins min';
    }
  }
  
  String _getMostProductiveDay(Map<String, int> data) {
    if (data.isEmpty) return 'No data';
    
    String maxDay = data.keys.first;
    int maxMinutes = data.values.first;
    
    data.forEach((day, minutes) {
      if (minutes > maxMinutes) {
        maxMinutes = minutes;
        maxDay = day;
      }
    });
    
    return '$maxDay (${_formatMinutes(maxMinutes)})';
  }
  
  Widget _buildSummaryRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(color: Colors.grey),
          ),
          Text(
            value,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
        ],
      ),
    );
  }
}

class StatsCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color color;
  
  const StatsCard({
    super.key,
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  icon,
                  color: color,
                  size: 28,
                ),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: TextStyle(
                    color: Colors.grey.shade700,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              value,
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class ActivityCalendar extends StatelessWidget {
  const ActivityCalendar({super.key});

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final daysInMonth = _getDaysInMonth(now.year, now.month);
    final firstDayOfMonth = DateTime(now.year, now.month, 1);
    final firstWeekday = firstDayOfMonth.weekday;
    
    return Consumer<StatsProvider>(
      builder: (context, statsProvider, child) {
        return Container(
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey.shade300),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
            children: [
              // Month header
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      DateFormat('MMMM yyyy').format(now),
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                  ],
                ),
              ),
              
              // Weekday headers
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: const [
                  _WeekdayLabel('M'),
                  _WeekdayLabel('T'),
                  _WeekdayLabel('W'),
                  _WeekdayLabel('T'),
                  _WeekdayLabel('F'),
                  _WeekdayLabel('S'),
                  _WeekdayLabel('S'),
                ],
              ),
              
              const SizedBox(height: 8),
              
              // Calendar grid
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 7,
                  childAspectRatio: 1,
                ),
                itemCount: 42, // 6 weeks of 7 days
                itemBuilder: (context, index) {
                  // Adjusting for first day of month offset
                  final adjustedIndex = index - (firstWeekday - 1);
                  
                  // Only show days in the current month
                  if (adjustedIndex < 0 || adjustedIndex >= daysInMonth) {
                    return const SizedBox();
                  }
                  
                  final day = adjustedIndex + 1;
                  final date = DateTime(now.year, now.month, day);
                  final dateStr = DateFormat('yyyy-MM-dd').format(date);
                  final minutes = statsProvider.dailyFocusMinutes[dateStr] ?? 0;
                  
                  return _CalendarDay(
                    day: day,
                    focusMinutes: minutes,
                    isToday: day == now.day,
                  );
                },
              ),
              
              const SizedBox(height: 16),
              
              // Legend
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    _ActivityIndicator(color: Colors.grey.shade300, label: 'No Focus'),
                    const SizedBox(width: 16),
                    _ActivityIndicator(color: Colors.lightBlue.shade200, label: '< 30m'),
                    const SizedBox(width: 16),
                    _ActivityIndicator(color: Colors.blue.shade400, label: '30m - 2h'),
                    const SizedBox(width: 16),
                    _ActivityIndicator(color: Colors.blue.shade700, label: '> 2h'),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
  
  int _getDaysInMonth(int year, int month) {
    return DateTime(year, month + 1, 0).day;
  }
}

class _WeekdayLabel extends StatelessWidget {
  final String label;
  
  const _WeekdayLabel(this.label);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 30,
      child: Text(
        label,
        textAlign: TextAlign.center,
        style: TextStyle(
          color: Colors.grey.shade600,
          fontWeight: FontWeight.bold,
          fontSize: 12,
        ),
      ),
    );
  }
}

class _CalendarDay extends StatelessWidget {
  final int day;
  final int focusMinutes;
  final bool isToday;
  
  const _CalendarDay({
    required this.day,
    required this.focusMinutes,
    required this.isToday,
  });

  @override
  Widget build(BuildContext context) {
    Color backgroundColor;
    
    if (focusMinutes == 0) {
      backgroundColor = Colors.grey.shade300;
    } else if (focusMinutes < 30) {
      backgroundColor = Colors.lightBlue.shade200;
    } else if (focusMinutes < 120) {
      backgroundColor = Colors.blue.shade400;
    } else {
      backgroundColor = Colors.blue.shade700;
    }
    
    return Container(
      margin: const EdgeInsets.all(2),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(4),
        border: isToday ? Border.all(
          color: Colors.black,
          width: 2,
        ) : null,
      ),
      child: Center(
        child: Text(
          day.toString(),
          style: TextStyle(
            fontSize: 12,
            fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
            color: focusMinutes > 30 ? Colors.white : Colors.black87,
          ),
        ),
      ),
    );
  }
}

class _ActivityIndicator extends StatelessWidget {
  final Color color;
  final String label;
  
  const _ActivityIndicator({
    required this.color,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(2),
          ),
        ),
        const SizedBox(width: 4),
        Text(
          label,
          style: const TextStyle(
            fontSize: 10,
            color: Colors.grey,
          ),
        ),
      ],
    );
  }
}

class AchievementItem extends StatelessWidget {
  final String title;
  final String description;
  final bool isUnlocked;
  
  const AchievementItem({
    super.key,
    required this.title,
    required this.description,
    required this.isUnlocked,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: isUnlocked ? Colors.amber : Colors.grey.shade300,
          shape: BoxShape.circle,
        ),
        child: Icon(
          isUnlocked ? Icons.emoji_events : Icons.lock,
          color: isUnlocked ? Colors.white : Colors.grey.shade600,
        ),
      ),
      title: Text(
        title,
        style: TextStyle(
          fontWeight: FontWeight.bold,
          color: isUnlocked ? Colors.black : Colors.grey,
        ),
      ),
      subtitle: Text(
        description,
        style: TextStyle(
          color: isUnlocked ? Colors.grey.shade700 : Colors.grey.shade500,
        ),
      ),
    );
  }
}
