import 'package:flutter/material.dart';
import 'package:audioplayers/audioplayers.dart';
import 'package:focus_flow/models/settings_model.dart';
import 'package:focus_flow/providers/settings_provider.dart';

class AmbientSoundProvider with ChangeNotifier {
  final AudioPlayer _audioPlayer = AudioPlayer();
  final SettingsProvider _settingsProvider;

  String? _currentSoundAsset;
  bool _isPlaying = false;
  PlayerState _playerState = PlayerState.stopped;

  bool get isPlaying => _isPlaying;

  AmbientSoundProvider(this._settingsProvider) {
    _audioPlayer.setReleaseMode(ReleaseMode.loop); // Default to loop
    _audioPlayer.onPlayerStateChanged.listen((PlayerState state) {
      _playerState = state;
      _isPlaying = state == PlayerState.playing;
      notifyListeners();
    });
    _loadInitialSound();
  }

  void _loadInitialSound() {
    final settings = _settingsProvider.settings;
    if (settings.selectedAmbientSound != 'none' && 
        (settings.playAmbientSoundDuringFocus || settings.playAmbientSoundDuringBreaks)) {
      // We don't auto-play here, just prepare if a sound is selected
      // Actual playback will be triggered by TimerProvider based on session type
      prepareSound(settings.selectedAmbientSound, settings.ambientSoundVolume);
    }
  }

  Future<void> prepareSound(String assetPath, double volume) async {
    if (assetPath == 'none') {
      await stopSound();
      _currentSoundAsset = null;
      return;
    }
    if (_currentSoundAsset == assetPath && _playerState != PlayerState.stopped && _playerState != PlayerState.completed) {
      // Sound is already loaded or playing, just adjust volume if needed
      await setVolume(volume);
      return;
    }
    _currentSoundAsset = assetPath;
    await _audioPlayer.setSource(AssetSource(assetPath)); 
    await setVolume(volume);
    // Do not auto-play here; playback is controlled by session state
  }

  Future<void> playSound() async {
    if (_currentSoundAsset != null && _currentSoundAsset != 'none') {
      try {
        await _audioPlayer.resume(); // Use resume to play or resume from pause
        _isPlaying = true;
      } catch (e) {
        debugPrint('Error playing sound: $e');
        _isPlaying = false;
      }
    } else {
      _isPlaying = false;
    }
    notifyListeners();
  }

  Future<void> pauseSound() async {
    if (_isPlaying) {
      try {
        await _audioPlayer.pause();
        _isPlaying = false;
      } catch (e) {
        debugPrint('Error pausing sound: $e');
      }
      notifyListeners();
    }
  }

  Future<void> stopSound() async {
    try {
      await _audioPlayer.stop();
      _isPlaying = false;
      _currentSoundAsset = null; // Clear current sound when stopped explicitly
    } catch (e) {
      debugPrint('Error stopping sound: $e');
    }
    notifyListeners();
  }

  Future<void> setVolume(double volume) async {
    try {
      await _audioPlayer.setVolume(volume.clamp(0.0, 1.0));
    } catch (e) {
      debugPrint('Error setting volume: $e');
    }
  }

  // Called by TimerProvider or when settings change
  void updatePlaybackBasedOnSession(String sessionType, bool isSessionActive) {
    final settings = _settingsProvider.settings;
    bool shouldPlay = false;

    if (settings.selectedAmbientSound == 'none' || !isSessionActive) {
      shouldPlay = false;
    } else {
      if (sessionType == 'focus' && settings.playAmbientSoundDuringFocus) {
        shouldPlay = true;
      } else if ((sessionType == 'shortBreak' || sessionType == 'longBreak') && settings.playAmbientSoundDuringBreaks) {
        shouldPlay = true;
      }
    }

    if (shouldPlay) {
      if (_currentSoundAsset != settings.selectedAmbientSound || _playerState == PlayerState.stopped || _playerState == PlayerState.completed) {
        // If sound changed or was stopped, prepare and play
        prepareSound(settings.selectedAmbientSound, settings.ambientSoundVolume).then((_) {
          if (_isPlaying && _playerState != PlayerState.playing) playSound(); // Play if it was supposed to be playing but stopped
          else if (!_isPlaying) playSound(); // Or just play if not playing
        });
      } else if (!_isPlaying) {
        // If correct sound is loaded but not playing, play it
        playSound();
      }
      // Ensure volume is up-to-date
      setVolume(settings.ambientSoundVolume);
    } else {
      if (_isPlaying) {
        pauseSound(); // Pause if sound shouldn't be playing
      }
    }
  }

  // This method should be called when SettingsProvider notifies its listeners
  void onSettingsChanged() {
    final settings = _settingsProvider.settings;
    // If the sound selection itself changed to 'none', stop immediately.
    if (settings.selectedAmbientSound == 'none' && _isPlaying) {
      stopSound();
      _currentSoundAsset = null;
      return;
    }
    // If a sound is selected but playback toggles are off and it's playing, pause.
    if (settings.selectedAmbientSound != 'none' && 
        !settings.playAmbientSoundDuringFocus && 
        !settings.playAmbientSoundDuringBreaks && 
        _isPlaying) {
      pauseSound();
      return;
    }
    
    // If a sound is selected, ensure it's prepared (volume might have changed)
    if (settings.selectedAmbientSound != 'none') {
       prepareSound(settings.selectedAmbientSound, settings.ambientSoundVolume);
    }

    // The actual play/pause logic based on current session type and active state
    // will be handled by updatePlaybackBasedOnSession, which should be called by TimerProvider.
    // However, if a sound was playing and now it's globally disabled by settings, it should stop.
    // This part is a bit tricky as TimerProvider usually dictates play/pause based on session.
    // For now, `updatePlaybackBasedOnSession` is the main controller when a session is active.
    // If no session is active, and settings change, this method handles preparation.
  }

  @override
  void dispose() {
    _audioPlayer.dispose();
    super.dispose();
  }
}
