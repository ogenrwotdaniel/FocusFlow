import 'package:flutter/services.dart';
import 'dart:io';

class AndroidPlatformService {
  static const MethodChannel _channel = MethodChannel('com.example.focusflow_flutter/notifications');
  
  /// Singleton instance
  static final AndroidPlatformService _instance = AndroidPlatformService._internal();
  
  /// Factory constructor to return the singleton instance
  factory AndroidPlatformService() => _instance;
  
  /// Private constructor
  AndroidPlatformService._internal();
  
  /// Request battery optimization exemption (Android only)
  /// This helps ensure timers and notifications work reliably
  Future<bool> requestBatteryOptimizationExemption() async {
    if (!Platform.isAndroid) return false;
    
    try {
      final result = await _channel.invokeMethod<bool>('requestBatteryOptimizationExemption');
      return result ?? false;
    } on PlatformException catch (e) {
      print('Failed to request battery optimization exemption: ${e.message}');
      return false;
    }
  }
  
  /// Check if notification permission is granted (Android 13+)
  Future<bool> checkNotificationPermission() async {
    if (!Platform.isAndroid) return true;
    
    try {
      final result = await _channel.invokeMethod<bool>('checkNotificationPermission');
      return result ?? false;
    } on PlatformException catch (e) {
      print('Failed to check notification permission: ${e.message}');
      return false;
    }
  }
  
  /// Request notification permission (Android 13+)
  Future<bool> requestNotificationPermission() async {
    if (!Platform.isAndroid) return true;
    
    try {
      final result = await _channel.invokeMethod<bool>('requestNotificationPermission');
      return result ?? false;
    } on PlatformException catch (e) {
      print('Failed to request notification permission: ${e.message}');
      return false;
    }
  }
  
  /// Initialize all Android-specific requirements
  Future<void> initAndroidFeatures() async {
    if (!Platform.isAndroid) return;
    
    // Check and request notification permission on Android 13+
    final hasPermission = await checkNotificationPermission();
    if (!hasPermission) {
      await requestNotificationPermission();
    }
    
    // Request battery optimization exemption
    await requestBatteryOptimizationExemption();
  }
}
