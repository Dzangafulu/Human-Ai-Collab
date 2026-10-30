# UniTask 2.1 — Dashboard study sessions, reminders and Unify conversation

**Start with `RELEASE_2_1_SETUP.md` for the current installation and Firebase instructions.** `CHANGES_2_1.md` documents every addition and modification. The older `docs/` files describe the previous release and may not include the new scheduler and notification features. Real SMS MFA is not guaranteed on Firebase Spark (see setup guide).

# UniTask — Study smarter, achieve more

Android Studio Java/XML application assembled from the user-supplied **UniTask v2** project. Includes launch branding, bottom navigation, Firebase Authentication/MFA, Dashboard, task management with dates/times, notes, settings, profile photos, contact requests, private chats, groups, shared assignments and Gemini-based **Unify** with a local fallback.

**Start here:** `docs/SETUP_AND_TESTING.md` and `docs/INTEGRATION_CHANGELOG.md`.

**Important:** The Java sources were syntax-checked and the Android XML files validated. The scheduling logic passed a standalone Java test. The project has **not** been fully built inside Android Studio or tested against your live Firebase account, because the Android SDK/emulator and Firebase project access are unavailable in this environment. It is a source-code integration deliverable—not a verified production release or compiled APK.

The app package ID is `com.hacms.app`, matching the latest team's code. This is different from the previous integration's `com.example.human_ai_collaboration_platform`; Firebase App Check registrations and SHA-256 fingerprints are app-specific.

## Modules
- Home — live Firestore task summaries and scheduling shortcut
- Tasks — direct task-board screen, personal/group assignment and completion
- Notes — teammate's existing Notes feature (Firestore)
- Unify — Firebase AI Logic Gemini model, with explicit consent and Java fallback
- Chats — email request/accept, private Firestore direct messages, groups and `@Unify`
- Profile — name and optional locally compressed photo saved to Firestore
- Settings — dark mode, AI consent, opt-in task context and logout

## Security
The original hardcoded OpenAI key was removed from the source in this package. It **must still be revoked/rotated** by the key owner. Debug App Check and release Play Integrity are separated into Android source sets. Never commit App Check debug tokens, release signing passwords or private keys.
