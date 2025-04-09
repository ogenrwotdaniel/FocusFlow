# FocusFlow

FocusFlow is a productivity app designed to help users maintain focus during work or study sessions. It provides analytics, personalized insights, and social features to improve productivity.

## Features

- **Focus Timer**: Set customizable focus sessions with breaks
- **Analytics Dashboard**: View productivity metrics and trends
- **AI-Powered Insights**: Get personalized recommendations based on your productivity patterns
- **Social Sessions**: Join or create group focus sessions with friends
- **Background Audio**: Optional ambient sounds to enhance focus
- **Virtual Garden**: Grow virtual trees based on your focus time

## Technical Details

- **Platform**: Android
- **Language**: Kotlin
- **Architecture**: MVVM with Clean Architecture principles
- **Dependencies**:
  - Firebase Authentication, Firestore, Realtime Database, Messaging, and Analytics
  - Dagger Hilt for dependency injection
  - Jetpack components (ViewModel, LiveData, Room)
  - Kotlin Coroutines for asynchronous programming

## Firebase Integration

FocusFlow uses Firebase for:
- User authentication
- Storing user data and session records
- Tracking analytics and providing insights
- Real-time presence and group sessions
- Push notifications for reminders and social features
