import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter/material.dart';
import 'package:focus_flow/services/android_platform_service.dart';
import 'package:timezone/timezone.dart' as tz;

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  final FlutterLocalNotificationsPlugin _notificationsPlugin = FlutterLocalNotificationsPlugin();
  Function(String?)? _onNotificationTap; // Store the callback

  factory NotificationService() {
    return _instance;
  }

  NotificationService._internal();

  Future<void> init({Function(String?)? onNotificationTap}) async { // Accept callback
    _onNotificationTap = onNotificationTap; // Store it

    // Initialize Android-specific features
    await AndroidPlatformService().initAndroidFeatures();
    
    // Initialize settings for Android with improved channel settings
    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('@mipmap/ic_launcher');
        
    // Create notification channels for Android
    await _createNotificationChannels();

    // Initialize settings for Android only
    const InitializationSettings initializationSettings = InitializationSettings(
      android: initializationSettingsAndroid,
    );

    // Initialize notification plugin
    await _notificationsPlugin.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse: _onDidReceiveNotificationResponse,
      onDidReceiveBackgroundNotificationResponse: _onDidReceiveBackgroundNotificationResponse, // Added for background
    );
    
    // Request Android permissions
    await _requestPermissions();
  }

  Future<void> _requestPermissions() async {
    // For Android, use our custom platform service for Android 13+ permissions
    final hasPermission = await AndroidPlatformService().checkNotificationPermission();
    if (!hasPermission) {
      await AndroidPlatformService().requestNotificationPermission();
    }
    
    // Also use the plugin's built-in permission request for compatibility
    await _notificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
  }
  
  // Create optimized notification channels for Android
  Future<void> _createNotificationChannels() async {
    final AndroidFlutterLocalNotificationsPlugin? androidPlugin = _notificationsPlugin
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
        
    if (androidPlugin == null) return;
    
    // Timer channel - low priority, quiet
    await androidPlugin.createNotificationChannel(
      const AndroidNotificationChannel(
        'timer_channel',
        'Timer Notifications',
        description: 'Notifications for focus timer',
        importance: Importance.low,
        playSound: false,
        enableVibration: false,
      ),
    );
    
    // Motivation channel - higher priority with sound
    await androidPlugin.createNotificationChannel(
      const AndroidNotificationChannel(
        'motivation_channel',
        'Motivational Notifications',
        description: 'Motivational messages and reminders',
        importance: Importance.high, 
        playSound: true,
        enableVibration: true,
      ),
    );
    
    // Session completion channel - highest priority
    await androidPlugin.createNotificationChannel(
      const AndroidNotificationChannel(
        'completion_channel',
        'Session Completion',
        description: 'Notifications for completed focus sessions',
        importance: Importance.max,
        playSound: true,
        enableVibration: true,
      ),
    );
    
    // Scheduled Session Reminder channel - high priority
    await androidPlugin.createNotificationChannel(
      const AndroidNotificationChannel(
        'scheduled_session_channel', // Unique ID
        'Scheduled Session Reminders',
        description: 'Reminders for your planned focus sessions',
        importance: Importance.high, // Or Importance.max
        playSound: true,
        enableVibration: true,
      ),
    );
  }

  void _onDidReceiveNotificationResponse(NotificationResponse response) {
    // Handle notification response
    if (_onNotificationTap != null && response.payload != null) {
      _onNotificationTap!(response.payload);
    }
    debugPrint('Notification response received: ${response.payload}');
  }

  // Static method for background/terminated state handling if needed
  @pragma('vm:entry-point')
  static void _onDidReceiveBackgroundNotificationResponse(NotificationResponse response) {
    // IMPORTANT: This handler runs in a separate isolate.
    // It cannot directly update UI or call regular app logic easily.
    // For now, we'll just print. More complex background handling might require
    // plugins like flutter_isolate or careful use of SharedPreferences/databases
    // to communicate back to the main app when it next starts.
    debugPrint('Background notification tapped: ${response.payload}');

    // If you need to pass data to the main app when it opens next, 
    // you could save the payload to SharedPreferences here.
    // Example (requires SharedPreferences to be initialized in this isolate if used directly):
    // SharedPreferences.getInstance().then((prefs) {
    //   prefs.setString('tapped_notification_payload', response.payload ?? '');
    // });
  }

  // Show timer notification (optimized for Android)
  Future<void> showTimerNotification({
    required int id,
    required String title,
    required String body,
    String? payload,
  }) async {
    const AndroidNotificationDetails androidDetails = AndroidNotificationDetails(
      'timer_channel', 
      'Timer Notifications',
      channelDescription: 'Notifications for the active focus timer',
      importance: Importance.low, 
      priority: Priority.low,
      playSound: false,
      enableVibration: false,
      ongoing: true, 
      autoCancel: false, 
      visibility: NotificationVisibility.public, 
      category: AndroidNotificationCategory.progress, 
    );

    const NotificationDetails platformDetails = NotificationDetails(
      android: androidDetails,
    );

    await _notificationsPlugin.show(
      id,
      title,
      body,
      platformDetails,
      payload: payload,
    );
  }

  // Show motivation notification (optimized for Android)
  Future<void> showMotivationNotification({
    required int id,
    required String title,
    required String body,
    String? payload,
  }) async {
    final AndroidNotificationDetails androidDetails = AndroidNotificationDetails(
      'motivation_channel',
      'Motivational Notifications',
      channelDescription: 'Motivational messages and reminders',
      importance: Importance.high,
      priority: Priority.high,
      enableLights: true,
      color: const Color.fromARGB(255, 66, 165, 245), // Light blue color
      visibility: NotificationVisibility.public,
      category: AndroidNotificationCategory.reminder,
      fullScreenIntent: false,
    );

    final NotificationDetails platformDetails = NotificationDetails(
      android: androidDetails,
    );

    await _notificationsPlugin.show(
      id,
      title,
      body,
      platformDetails,
      payload: payload,
    );
  }
  
  // Cancel a notification
  Future<void> cancelNotification(int id) async {
    await _notificationsPlugin.cancel(id);
  }
  
  // Cancel all notifications
  Future<void> cancelAllNotifications() async {
    await _notificationsPlugin.cancelAll();
  }
  
  // Show session completion notification (new method optimized for Android)
  Future<void> showCompletionNotification({
    required int id,
    required String title,
    required String body,
    String? payload,
  }) async {
    final AndroidNotificationDetails androidDetails = AndroidNotificationDetails(
      'completion_channel',
      'Session Completion',
      channelDescription: 'Notifications for completed focus sessions',
      importance: Importance.max,
      priority: Priority.max,
      enableLights: true,
      color: const Color.fromARGB(255, 76, 175, 80), // Green color
      visibility: NotificationVisibility.public,
      category: AndroidNotificationCategory.alarm,
      fullScreenIntent: true,
      // Add actions for Android
      actions: <AndroidNotificationAction>[
        const AndroidNotificationAction('start_new', 'Start New Session'),
        const AndroidNotificationAction('view_stats', 'View Stats'),
      ],
    );

    final NotificationDetails platformDetails = NotificationDetails(
      android: androidDetails,
    );

    await _notificationsPlugin.show(
      id,
      title,
      body,
      platformDetails,
      payload: payload,
    );
  }

  // Schedule a notification for a specific time
  Future<void> scheduleSessionNotification({
    required int id,
    required String title,
    required String body,
    required DateTime scheduledTime,
    String? payload,
  }) async {
    await _ensurePermissions();

    final AndroidNotificationDetails androidDetails = AndroidNotificationDetails(
      'scheduled_session_channel', // Use the new channel ID
      'Scheduled Session Reminders',
      channelDescription: 'Reminders for your planned focus sessions',
      importance: Importance.high, // Match channel importance
      priority: Priority.high,
      playSound: true,
      enableVibration: true,
      // You can add other specific details like custom sound if needed
      // sound: RawResourceAndroidNotificationSound('your_custom_sound'), 
      // styleInformation: BigTextStyleInformation(''), // For longer text
    );

    final NotificationDetails platformDetails = NotificationDetails(
      android: androidDetails,
      // iOS: DarwinNotificationDetails(), // Add iOS details if supporting iOS
    );

    // Ensure the scheduledTime is in the future
    if (scheduledTime.isBefore(DateTime.now())) {
      debugPrint('Attempted to schedule notification in the past. Skipping.');
      return;
    }

    await _notificationsPlugin.zonedSchedule(
      id,
      title,
      body,
      tz.TZDateTime.from(scheduledTime, tz.local), // Convert to TZDateTime
      platformDetails,
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle, // For precise timing
      payload: payload,
      matchDateTimeComponents: DateTimeComponents.time, // For daily repeating, if needed later
    );
    debugPrint('Notification scheduled for $id at $scheduledTime');
  }

  // Helper to ensure permissions before scheduling (can be called internally)
  Future<void> _ensurePermissions() async {
    // This can re-use the logic from _requestPermissions or simplify if needed
    // For now, assuming init() handles initial permission requests.
    // If scheduling can happen before init() is fully complete in some edge cases,
    // this method might need to be more robust.
    final hasPermission = await AndroidPlatformService().checkNotificationPermission();
    if (!hasPermission) {
      await AndroidPlatformService().requestNotificationPermission();
      // Also plugin's request
      await _notificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
    }
  }
}
