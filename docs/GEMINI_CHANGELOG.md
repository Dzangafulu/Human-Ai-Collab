# Gemini upgrade: exact changes

This changelog describes changes **relative to `HumanAI_Integrated_AndroidStudio.zip`** (previously integrated app). Teammate contributions were retained unless noted below.

## Modified

1. `app/build.gradle.kts` — Firebase BoM updated from 33.1.0 to 35.0.2; added `firebase-ai`, debug App Check, release Play Integrity App Check, Java Guava futures. Removed the **old unused legacy Google Generative AI SDK dependency**, plus duplicate Guava version to avoid conflicts. Other teammate libraries remain untouched.
2. `app/src/main/AndroidManifest.xml` — added `android:name=".StudentApplication"` to initialize Firebase App Check before calls. Existing permissions, launcher and teammates' activities unchanged.
3. `app/src/main/java/com/example/human_ai_collaboration_platform/ChatBotFragment.java` — replaced the previous safe local-mock response path with Firebase AI Logic real Gemini calls plus automatic fallback. Retained chat RecyclerView, `ChatBotAdapter`, `ChatBotModel`, send field/button IDs and appearance. Added visible AI status, first-use consent, opt-in for task context, bounded chat history, error handling, per-device 12 second request delay, one-request-at-a-time lockout, and a manual **local recommendations** button that uses no Gemini quota. **No OpenAI API key is present.** No chat UI overhaul.
4. `app/src/main/res/layout/fragment_chat_bot.xml` — **only** added a small assistant status/checkbox strip at the top; retained teammate's RecyclerView and send controls.
5. `README.md` — links to setup guide and explains the final project.

## Added

6. `app/src/main/java/com/example/human_ai_collaboration_platform/StudentApplication.java` — early Firebase/App Check initialisation.
7. `app/src/debug/java/com/example/human_ai_collaboration_platform/AppCheckBootstrap.java` — **debug builds only**: Firebase App Check debug provider (requires manually allowlisting a debug token in Firebase Console).
8. `app/src/release/java/com/example/human_ai_collaboration_platform/AppCheckBootstrap.java` — **release builds only**: Play Integrity App Check provider (requires production Firebase registration).
9. `app/src/main/java/com/example/human_ai_collaboration_platform/GeminiAiClient.java` — real Firebase AI Logic model `gemini-3.5-flash-lite`; Java future callback. **No API key** embedded.
10. `app/src/main/java/com/example/human_ai_collaboration_platform/LocalStudyAssistant.java` — deterministic fallback recommendations from existing Task model/StudyPlanEngine. Honest handling of general-knowledge questions and missing task data.
11. `docs/START_HERE_GEMINI.md` — comprehensive setup, Firebase, security and testing instructions.
12. `docs/GEMINI_CHANGELOG.md` — this file.

## Preserved from prior integration

- `LoginActivity`, `SignupActivity`, `MainActivity`: existing Firebase Auth/navigation.
- `TaskBoardActivity`, `TaskBoardFragment`, `Task`: existing Task Board UI/CRUD and model.
- `FirebaseTaskRepository`: existing Firestore task persistence.
- `DashboardFragment`, `SchedulerFragment`, `StudyPlanEngine`: existing Part 4 UI/algorithm/persistence.
- `ChatFragment`, `ChatListFragment`, `MessageAdapter` etc.: existing student-to-student chat (Realtime Database).
- Existing menus, navigation graph, dashboard/scheduler layouts, signup/login layouts, Firestore tasks/study-session rules document.

## Important limits and things *not* changed

- Cannot verify live Firebase AI Logic, emulator or compile: this environment has no Android SDK and no Firebase Console access.
- Initial OpenAI key **must still be revoked manually** in the original account and historical Git commits reviewed.
- Gemini free quota is **shared** across the Firebase project. `gemini-3.5-flash-lite` has free-tier eligibility per documentation, but actual quota for this project needs to be checked. The 2.5 quota screenshot doesn't prove a 3.5 quota.
- Local scheduler is the same simplified 1h sessions over a 7-day window; does not automatically book group meetings or manage calendar conflicts.
- Existing Task Board does not track a numeric % complete, and assigning text does not give a recipient another user's task permissions.
- Chat consent is a local per-device preference and task-context opt-in, not a university-wide privacy-compliance system.
