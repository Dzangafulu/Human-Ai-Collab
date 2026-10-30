# UniTask 2.1 change inventory

## Modified
- `app/src/main/java/com/hacms/app/MfaEnrollActivity.java`: format/validation for per-user phone, 6-digit code checks, resend capability. No Firebase Console data changed.
- `app/src/main/java/com/hacms/app/DashboardFragment.java`: corrected invalid greeting cast; subscribes to `study_sessions`; shows future session cards below Generate button, auto-refreshes expiration; schedules task/session reminders.
- `app/src/main/java/com/hacms/app/SchedulerFragment.java`: replaces preset-hour choices/save button with manual dated/time-picker/duration form plus automatically persisted task-based generation; shows saved sessions and supports removal.
- `app/src/main/java/com/hacms/app/StudyPlanEngine.java`: task-labelled, deadline-aware 7-day plan generation with arbitrary hour and minute; retains previous 3-argument overload.
- `app/src/main/java/com/hacms/app/ChatBotFragment.java`: removed emoji, retains messages while navigating via activity-scoped ViewModel; explicit scheduling commands persist task-related sessions; Gemini/free-tier fallback unchanged.
- `app/src/main/java/com/hacms/app/GeminiAiClient.java`: moves from deprecated Gemini 2.5 Flash-Lite to supported free-tier Gemini 3.5 Flash-Lite; actual project quota still needs confirming in AI Studio.
- `app/src/main/java/com/hacms/app/GroupFragment.java`: bot display now 'Unify' without emoji.
- `app/src/main/java/com/hacms/app/Ui.java`: fixed duplicate local variable `words` shadowing.
- `app/src/main/java/com/hacms/app/ContactsFragment.java`: fixed non-effectively-final `title` lambda capture.
- `app/src/main/java/com/hacms/app/MainActivity.java`: requests runtime notification permission Android 13+.
- `app/src/main/java/com/hacms/app/FirebaseTaskRepository.java`: cancels a deleted task's scheduled notification.
- `app/src/main/res/layout/fragment_scheduler.xml`: new editable date, time, duration, and study name controls.
- `app/src/main/res/menu/bottom_nav_menu.xml`: changed Home icon to house vector.
- `app/src/main/AndroidManifest.xml`: POST_NOTIFICATIONS permission.
- `app/build.gradle.kts`: minor version bump and WorkManager/ViewModel dependencies.

## Added
- `app/src/main/java/com/hacms/app/StudySessionStore.java`: owner-scoped Firestore session persistence, back-compatible time reading.
- `app/src/main/java/com/hacms/app/ReminderScheduler.java`: one-time, unique WorkManager reminders.
- `app/src/main/java/com/hacms/app/ReminderWorker.java`: Android system notifications.
- `app/src/main/java/com/hacms/app/UnifyConversation.java`: in-memory (per MainActivity) Unify chat history.
- `app/src/main/res/drawable/ic_home_outline.xml`: house icon.
- `RELEASE_2_1_SETUP.md`: exact Android Studio and Firebase steps.

## Preserved
Existing Firebase project, `google-services.json`, Firestore rules file, note features, existing task/group UI and chat collections, login process and Gemini AI client. No Firebase credentials included beyond the existing standard client config. No production SMS billing enabled. No Firebase email templates edited remotely.
