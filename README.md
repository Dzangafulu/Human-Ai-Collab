<p align="center">
  <img src="app/src/main/res/drawable-nodpi/unitask_lockup.png" alt="UniTask logo" width="260" />
</p>

<h1 align="center">UniTask</h1>
<p align="center"><strong>Study smarter, achieve more.</strong></p>
<p align="center">An Android student productivity app combining task management, study planning, collaboration, and AI-powered assistance.</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84" />
  <img alt="Language" src="https://img.shields.io/badge/Language-Java-ED8B00" />
  <img alt="Backend" src="https://img.shields.io/badge/Backend-Firebase-FFCA28" />
  <img alt="Version" src="https://img.shields.io/badge/Version-2.4-blue" />
</p>

## Overview

**UniTask** is a collaborative Android application designed to help students organize academic work, plan study time, communicate with classmates, and stay focused. It brings tasks, notes, study sessions, messaging, and an AI study assistant together in one application.

Developed as a team project, UniTask explores how human–AI collaboration can support better planning, decision-making, and productivity in student workflows.

## Features

### Dashboard and task management
- A home dashboard with task summaries, upcoming deadlines, and study sessions.
- Create, edit, assign, track, and complete personal or group tasks.
- View task progress, priorities, and due dates.

### Study planning and Focus Mode
- Generate study-plan suggestions based on tasks and deadlines.
- Schedule study sessions with custom start times and a countdown timer.
- Receive local reminders before a session and notifications when it starts and ends.
- Optionally enable **Focus Mode** for a study session, select distracting apps, and display a blocking screen when those apps are opened.
- Return to the study timer from the blocking screen or request an early exit with account verification.

> **Focus Mode limitation:** App monitoring relies on Android Accessibility permission and is a best-effort distraction-reduction feature, not a tamper-proof app lock. Permissions and background restrictions vary by device.

### Unify — AI study assistant
- Ask **Unify** for study guidance and task-related recommendations.
- Uses **Firebase AI Logic with Google's Gemini models**.
- Provides local, rule-based recommendations when the AI service is unavailable or subject to quota limits.

### Notes and collaboration
- Create and manage personal notes.
- Find other students by email and send or accept chat requests.
- Exchange private messages and communicate in group chats.
- Collaborate on group tasks and identify message senders by their profile names.
- See unread-message indicators **inside the app**.
- Share small images and documents in chats (current prototype limit: **250 KB** per attachment, with image compression).

### Accounts and settings
- Firebase email/password authentication with authenticator-app **multi-factor authentication (TOTP)**.
- Student profile management and optional profile pictures.
- Dark mode and study/AI preferences.
- Firebase App Check integration for development and release configurations.

## Technology stack

| Area | Technologies |
| --- | --- |
| Android app | Java, XML, Android Studio, AndroidX, Material Components |
| Authentication | Firebase Authentication, TOTP multi-factor authentication |
| Data | Cloud Firestore; Firebase Realtime Database used by some existing chat components |
| AI | Firebase AI Logic, Gemini, local rule-based fallback |
| Background scheduling | WorkManager, AlarmManager, Android notifications |
| Focus features | Android Accessibility Service, optional Do Not Disturb access |
| App verification | Firebase App Check (debug provider for development; Play Integrity for release) |

## Getting started

### Requirements

- Android Studio with a compatible Android SDK (**compile SDK 36** in this project).
- JDK 17 for the Android Gradle Plugin setup.
- An Android device or emulator running **Android 7.0 (API 24)** or later.
- A configured Firebase project for the app, with access to the required Firebase services.

### Setup

1. Clone or download this repository.
2. In Android Studio, select **File → Open** and choose the project folder containing `settings.gradle.kts`.
3. Create or select a Firebase project and register the Android application using package name **`com.hacms.app`**. If you are setting up your own backend, place its `google-services.json` in the `app/` directory.
4. Configure Firebase Authentication (email/password and the MFA method used by the app), Cloud Firestore, and any Realtime Database paths your selected chat features use.
5. Review and deploy the appropriate Firestore security rules. The repository includes `firebase/firestore.rules` as a starting point; **review them against your current application code and data before publishing**.
6. Configure Firebase AI Logic and Firebase App Check. Check that the configured Gemini model is available to your project and that you have sufficient quota.
7. Allow Gradle Sync to finish, then choose **Build → Make Project**.
8. Run the application on an emulator or connected Android phone.

> **App Check note:** Debug builds may require a debug token to be registered for each test device. Signed release builds use Play Integrity and require the correct release SHA-256 fingerprint and provider settings. A signed APK does not automatically guarantee App Check verification on every device.

## Project structure

```text
app/
  src/main/java/com/hacms/app/   # App screens, Firebase integration, AI, chat, Focus Mode
  src/main/res/                 # Layouts, icons, styles, other Android resources
  src/main/AndroidManifest.xml  # Android app components and permissions
firebase/
  firestore.rules               # Firestore rules supplied with the project
docs/                             # Setup notes and change history
```

## Permissions and limitations

- **Accessibility access** is optional and used for Focus Mode app monitoring; users must grant it in Android settings.
- **Notification permission** may be needed for study-session alerts. Scheduled local notifications can run while the app's UI is closed, but Android battery restrictions or force-stop can prevent timely delivery.
- **Do Not Disturb access** is optional and must be approved by the user.
- **Unread chat indicators** are in-app indicators, not external push notifications.
- **Attachments** are intentionally size-limited for this prototype.
- AI availability depends on Firebase AI Logic, App Check, model support, network connectivity, and service quotas.

## Team and purpose

UniTask was developed collaboratively as an academic software project, bringing together work on authentication, task management, dashboards and scheduling, AI assistance, and student collaboration.
