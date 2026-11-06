# UniTask 2.3 — change log

## Preserved

The supplied UniTask 2.2 project's login, signup, authenticator-app (TOTP) MFA, Firebase configuration, Tasks, Notes, Dashboard data, Scheduler scheduling logic, Gemini Unify integration, existing private/group contact request workflow and group membership functionality were retained. The project package name stays `com.hacms.app`.

## New files

- `FocusModeManager.java`: local device-level preferences and cached session scheduling; optional DND start/end.
- `FocusScheduleSync.java`: Firestore sessions listener, opt-in session cache.
- `FocusAlarmReceiver.java`, `FocusBootReceiver.java`: best-effort scheduled state updates and reboot recovery.
- `FocusAccessibilityService.java`, `res/xml/focus_accessibility.xml`: narrowly scoped, disclosed foreground-app detection and a blocking overlay.
- `FocusSettingsFragment.java`: Focus Mode enable/consent, app selection, system permission entry points.
- `FocusStatusFragment.java`: active session display and countdown.
- `FocusUnlockActivity.java`: password reauthentication, optional TOTP challenge, reset email, early exit.
- `MessageAttachment.java`: document/photo picker and 250 KB Firestore messages; photo compression and Save As action.
- `UnreadMarker.java`: per-conversation read receipt updating.
- `UnreadBadgeController.java`: bottom navigation Chats badge (live while open).
- `ThreadUnreadIndicator.java`: unread indicators on individual group/contact rows.

## Modified files

- `GroupFragment.java`: message list scrolls independently; composer remains anchored to bottom; plus button offers attachment and add-member actions; read receipts and attachment display.
- `DirectChatFragment.java`: preserves bottom composer, adds attachment picker/display and read receipts.
- `ContactsFragment.java`: shows unread count beside approved contacts.
- `SettingsFragment.java`: adds Focus Mode settings link.
- `DashboardFragment.java`: shows active Focus Mode timer entry.
- `SchedulerFragment.java`: adds optional Focus Mode checkbox per newly created manual/generated session.
- `StudySessionStore.java`: backwards-compatible `focusModeEnabled` flag in study-session documents; previous public method signatures preserved via overloads.
- `MainActivity.java`: start/stop live unread listeners and cached Focus session listeners.
- `app/src/main/AndroidManifest.xml`: adds accessibility service, timers/boot receiver, FocusUnlockActivity, permitted app discovery, DND permission and alarm scheduling.
- `res/navigation/nav_graph.xml`: adds Focus Settings and Focus Timer screens.
- `res/layout/fragment_scheduler.xml`: Focus Mode checkbox.
- `res/values/strings.xml`: Accessibility service explanation.
- `firebase/firestore.rules`: attachment-size validation and per-user read receipt rules in connection and group subcollections.
- `app/build.gradle.kts`: app version bumped to 2.3 (same dependencies used in 2.2).

## Explicitly not included

- No push notifications, Cloud Functions, Cloud Storage or paid infrastructure.
- No system-wide app suspension or guaranteed app blocking. No attempt to conceal a running Accessibility service or prevent the user revoking Android permissions.
- No modifications to the authenticator-app MFA source or Gemini provider.
- No production reliability or full Android build guarantee: needs validation on the development team's Android SDK and physical devices.
