import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';
import 'package:focus_flow/providers/timer_provider.dart'; // Assuming TimerProvider is accessible
import 'package:provider/provider.dart';

// This is the entry point for the overlay window.
@pragma("vm:entry-point")
void overlayMain() {
  // runApp(MaterialApp(home: FloatingTimerWidget())); // This won't work directly with provider
  // Instead, we might need a way to get TimerProvider's data or pass it initially.
  // For simplicity, we'll start with a placeholder text and figure out data passing.
  runApp(
    MaterialApp(
      debugShowCheckedModeBanner: false,
      home: FloatingTimerContent(), // Updated to use a stateful widget for timer updates
    ),
  );
}

class FloatingTimerContent extends StatefulWidget {
  const FloatingTimerContent({super.key});

  @override
  State<FloatingTimerContent> createState() => _FloatingTimerContentState();
}

class _FloatingTimerContentState extends State<FloatingTimerContent> {
  String _timeDisplay = "--:--";
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    // Listen for data from the main app
    FlutterOverlayWindow.overlayListener.listen((data) {
      if (data is Map && data.containsKey('remainingTime')) {
        if (mounted) {
          setState(() {
            _updateTimerDisplay(data['remainingTime']);
          });
        }
      }
    });
    // Request initial time update
    FlutterOverlayWindow.shareData({'action': 'getTime'}); 
  }

  void _updateTimerDisplay(int totalSeconds) {
    if (totalSeconds < 0) totalSeconds = 0;
    final minutes = totalSeconds ~/ 60;
    final seconds = totalSeconds % 60;
    _timeDisplay = "${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}";
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent, // Makes the background of the overlay transparent
      child: Center(
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 8.0, horizontal: 12.0),
          decoration: BoxDecoration(
            color: Colors.black.withOpacity(0.7),
            borderRadius: BorderRadius.circular(10.0),
          ),
          child: Text(
            _timeDisplay,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 20.0,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
      ),
    );
  }
}
