> **OLDER GUIDE / HISTORICAL RECORD** — For the Gemini upgrade, follow [START_HERE_GEMINI.md](START_HERE_GEMINI.md). The earlier text below describes the previous mock-AI build and is retained for version history only.

# Integration change log

**Base:** `HumanAICollaborationPlatform(1).zip` (package `com.example.human_ai_collaboration_platform`).
**Firebase:** existing `humanai-collab` `google-services.json` retained as supplied.
**No attempt was made to alter your Firebase Console or create online data.**

## New files
- `FirebaseTaskRepository.java`: maps teammate `Task` fields to Firestore `tasks` documents, scoped by `ownerUid`.
- `StudyPlanEngine.java`: deterministic 7-day task planner (Java-only; no AI model).
- `SchedulerFragment.java`: scheduler screen, generates previews, stores `study_sessions` in Firestore.
- `res/layout/fragment_dashboard.xml`: dashboard based on provided first screenshot.
- `res/layout/fragment_scheduler.xml`: scheduler interface.
- `res/drawable/dashboard_panel.xml`, `dashboard_white_card.xml`, `dashboard_avatar.xml`, `stat_blue.xml`, `stat_orange.xml`, `stat_green.xml`: Dashboard styling.
- `docs/FIRESTORE_RULES_ADD_THESE_BLOCKS.txt`: rule snippets to merge with existing rules.
- `docs/README_SETUP.md`: Windows/Android Studio/Firebase setup and test instructions.
- `app/src/test/java/com/example/human_ai_collaboration_platform/StudyPlanEngineTest.java`: optional planner unit tests.

## Edited files and why
- `Task.java`: added optional Firestore `documentId` getter/setter, kept all existing fields/constructor.
- `TaskBoardActivity.java`: left the teammate's controls, CRUD dialogs, task suggestions and visual layout intact; connected existing CRUD operations to `FirebaseTaskRepository` and added live task listener.
- `DashboardFragment.java`: replaced the old one-line placeholder with the actual dashboard, reads `users/{uid}` name and Firebase tasks.
- `res/navigation/nav_graph.xml`: added `schedulerFragment` and `chatConversationFragment` destinations.
- `SignupActivity.java`: fixed sign-up navigation; it previously tried to start a Fragment as an Activity.
- `ChatListFragment.java`: fixed conversation navigation to use Navigation component instead of replacing the NavHost itself (chat UI preserved).
- `ChatFragment.java`: fixed the toolbar Back action to return using the Navigation component (chat UI preserved).
- `ChatBotFragment.java`: **removed an exposed OpenAI secret** and the broken direct-to-OpenAI mobile request; kept original screen and message adapter; currently sends safe, clearly labeled task-based mock recommendations.

## Teammate code intentionally preserved
- Login UI, signup UI, password reset and Firebase Authentication; Task Board layout and form; user lists; message rendering; Realtime Database chat writes/reads; Gradle/AGP versions; existing project package name; Firebase config; bottom navigation and theme.

## Important functional limits
- Tasks now persist in Firestore. Existing old tasks in a teammate's in-memory session do not automatically migrate.
- The Task Board's `assignedTo` is free text and does not provide shared team permissions.
- Progress bars show **50% purely as a visual indication of "In Progress"**, not measured task completion; existing Task model has no numeric progress field.
- Study scheduler uses one 60-minute slot per unfinished task, up to 7 days at the selected start time and 1–3 sessions/day. It is **not** a conflict-aware calendar or accurate estimation engine. Unscheduled excess tasks are not included; due dates are used for ranking, not enforced as hard constraints.
- Saved sessions replace the same user/date/time slot when resaved. Other older slots remain stored.
- The AI Assistant is a **mock prototype**, not a real language model. It recommends up to three tasks using deadlines and priority.
- Task records belong to the authenticated creator; assigned tasks cannot be automatically viewed by other users. A true collaborative assignment system requires a new permissions design.
- The existing Android chat uses two mirrored Realtime Database chat rooms. Its security rules were not provided; verify access controls before public deployment.
- Build and end-to-end runtime have NOT been verified in an Android environment. Static XML/resource checks and a standalone Java planner test have been performed; see README_SETUP.

## Security
- An OpenAI API secret appeared in original team sources. Treat it as **compromised**, rotate/revoke it now. Never upload that historical secret to GitHub or commit logs. The integrated source no longer contains it.
- Do not place a new LLM provider secret inside any Android app. If live AI is added later, use a server proxy/authenticated cloud function and enforce auth/rate limiting.
