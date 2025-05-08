import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
// import 'package:firebase_core/firebase_core.dart';
import 'package:provider/provider.dart';
import 'dart:io' show Platform;
import 'package:timezone/data/latest_all.dart' as tz_data; // Import timezone data
import 'package:timezone/timezone.dart' as tz; // Import timezone library
import 'package:focus_flow/services/notification_service.dart';
import 'package:focus_flow/services/android_platform_service.dart';
import 'package:focus_flow/providers/timer_provider.dart';
import 'package:focus_flow/providers/session_provider.dart';
import 'package:focus_flow/providers/settings_provider.dart';
import 'package:focus_flow/providers/stats_provider.dart';
import 'package:focus_flow/providers/garden_provider.dart';
import 'package:focus_flow/providers/task_provider.dart';
import 'package:focus_flow/providers/schedule_provider.dart';
import 'package:focus_flow/providers/achievement_provider.dart';
import 'package:focus_flow/providers/ambient_sound_provider.dart';
import 'package:focus_flow/providers/calendar_provider.dart';
import 'package:focus_flow/models/scheduled_session_model.dart';
import 'package:focus_flow/screens/home_screen.dart';
import 'package:focus_flow/screens/timer_screen.dart';

// Global navigator key
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

// Handler for notification taps
Future<void> handleNotificationTap(String? payload) async {
  if (payload == null) return;

  final BuildContext? context = navigatorKey.currentContext;
  if (context != null) {
    final scheduleProvider = Provider.of<ScheduleProvider>(context, listen: false);
    try {
      final session = scheduleProvider.scheduledSessions.firstWhere((s) => s.id == payload);
      scheduleProvider.markSessionAsStarted(session.id); // Mark as started
      navigatorKey.currentState?.push(
        MaterialPageRoute(
          builder: (_) => TimerScreen(scheduledSession: session),
        ),
      );
    } catch (e) {
      debugPrint('Error finding scheduled session for notification: $e');
      // Optionally, navigate to a default screen or show an error
    }
  }
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize timezone data
  tz_data.initializeTimeZones();
  // Set the local timezone (optional, but good practice if you need to refer to it)
  // You might need to get the actual local timezone string dynamically if needed
  // For now, let this be handled by the system or flutter_local_notifications default
  
  // Set preferred orientations for better Android experience
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  
  // Set system UI overlay style for Android
  if (Platform.isAndroid) {
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
      systemNavigationBarColor: Colors.white,
      systemNavigationBarIconBrightness: Brightness.dark,
    ));
  }
  
  // Initialize Firebase
  // await Firebase.initializeApp();
  
  // Initialize Android platform service if on Android
  if (Platform.isAndroid) {
    await AndroidPlatformService().initAndroidFeatures();
  }
  
  // Initialize notifications
  await NotificationService().init(onNotificationTap: handleNotificationTap);
  
  // Example: Get current location for timezone (if needed, otherwise flutter_local_notifications handles it)
  // tz.setLocalLocation(tz.getLocation(await FlutterNativeTimezone.getLocalTimezone()));
  
  runApp(const FocusFlowApp());
}

class FocusFlowApp extends StatelessWidget {
  const FocusFlowApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => SettingsProvider()), // Settings first
        
        // Achievement and Stats Providers
        ChangeNotifierProvider(create: (_) => AchievementProvider()), // AchievementProvider is base
        ChangeNotifierProxyProvider<AchievementProvider, StatsProvider>(
          create: (context) => StatsProvider(), // StatsProvider loads its own data
          update: (context, achievementProvider, previousStatsProvider) {
            final statsProvider = previousStatsProvider ?? StatsProvider();
            statsProvider.setAchievementProvider(achievementProvider);
            // Ensure stats are loaded, constructor usually handles this or call explicitly if needed
            // statsProvider.loadStats(); 
            return statsProvider;
          },
        ),

        // TaskProvider now depends on StatsProvider
        ChangeNotifierProxyProvider<StatsProvider, TaskProvider>(
          create: (context) => TaskProvider(), // TaskProvider loads its own tasks
          update: (context, statsProvider, previousTaskProvider) {
            final taskProvider = previousTaskProvider ?? TaskProvider();
            taskProvider.setStatsProvider(statsProvider);
            return taskProvider;
          },
        ),
        // Original TaskProvider: ChangeNotifierProvider(create: (context) => TaskProvider()),

        ChangeNotifierProxyProvider<SettingsProvider, AmbientSoundProvider>(
          create: (context) => AmbientSoundProvider(Provider.of<SettingsProvider>(context, listen: false)),
          update: (context, settingsProvider, ambientSoundProvider) {
            if (ambientSoundProvider == null) throw ArgumentError.notNull('ambientSoundProvider');
            ambientSoundProvider.onSettingsChanged(); // Removed argument from onSettingsChanged call
            return ambientSoundProvider;
          },
        ),
        
        // TimerProvider now depends on SettingsProvider, AmbientSoundProvider, TaskProvider, StatsProvider, and NotificationService
        ChangeNotifierProxyProvider5<SettingsProvider, AmbientSoundProvider, TaskProvider, StatsProvider, NotificationService, TimerProvider>(
          create: (context) => TimerProvider(
            Provider.of<SettingsProvider>(context, listen: false),
            Provider.of<AmbientSoundProvider>(context, listen: false),
            Provider.of<TaskProvider>(context, listen: false),
            Provider.of<StatsProvider>(context, listen: false),
            NotificationService(), // Pass NotificationService instance
          ),
          update: (context, settingsProvider, ambientSoundProvider, taskProvider, statsProvider, notificationService, timerProvider) {
            if (timerProvider == null) throw ArgumentError.notNull('timerProvider');
            timerProvider.updateProviders(
              settingsProvider,
              ambientSoundProvider,
              taskProvider,
              statsProvider,
              notificationService, // Pass NotificationService instance
            );
            return timerProvider;
          },
        ),
        ChangeNotifierProvider(create: (_) => SessionProvider()),
        ChangeNotifierProxyProvider<AchievementProvider, GardenProvider>(
          create: (context) => GardenProvider(
            Provider.of<AchievementProvider>(context, listen: false),
          ),
          update: (context, achievementProvider, previousGardenProvider) => 
              previousGardenProvider ?? GardenProvider(achievementProvider),
        ),
        ChangeNotifierProvider(create: (_) => CalendarProvider()),
        // ScheduleProvider now depends on SettingsProvider and NotificationService
        ChangeNotifierProxyProvider2<SettingsProvider, NotificationService, ScheduleProvider>(
          create: (context) => ScheduleProvider(), // Initial plain ScheduleProvider
          update: (context, settingsProvider, notificationService, previousScheduleProvider) {
            final scheduleProvider = previousScheduleProvider ?? ScheduleProvider();
            // updateDependencies will call loadScheduledSessions and then updateAllSessionReminders
            scheduleProvider.updateDependencies(settingsProvider, notificationService);
            return scheduleProvider;
          },
        ),
      ],
      child: MaterialApp(
        navigatorKey: navigatorKey,
        title: 'Focus Flow',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.blue,
            primary: Colors.blue,
            secondary: Colors.green,
            background: Colors.white,
            surface: Colors.white,
            onSurface: Colors.black87,
            // Optimize colors for Android Material You design
            brightness: Brightness.light,
          ),
          useMaterial3: true,
          fontFamily: 'Roboto',
          appBarTheme: const AppBarTheme(
            backgroundColor: Colors.blue,
            foregroundColor: Colors.white,
            elevation: 0,
            centerTitle: true,
            systemOverlayStyle: SystemUiOverlayStyle(
              statusBarColor: Colors.transparent,
              statusBarIconBrightness: Brightness.light,
            ),
          ),
          // Optimize visual density for Android
          visualDensity: VisualDensity.adaptivePlatformDensity,
          // Optimize card theme
          cardTheme: const CardTheme(
            elevation: 2,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.all(Radius.circular(12)),
            ),
          ),
          // Optimize button theme for Android
          elevatedButtonTheme: ElevatedButtonThemeData(
            style: ElevatedButton.styleFrom(
              elevation: 2,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
          ),
        ),
        home: const HomeScreen(),
        debugShowCheckedModeBanner: false,
        // Set better Android platform customizations
        themeMode: ThemeMode.system,
      ),
    );
  }
}
