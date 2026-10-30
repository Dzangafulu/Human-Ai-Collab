# UniTask v2 → integrated UniTask 2.0: what changed

## Preserved from uploaded `UniTask v2.zip`
- Existing Java authentication implementation: `LoginActivity.java`, `SignupActivity.java`, `MfaEnrollActivity.java` and their layouts; MFA and email verification logic were not deliberately removed.
- Existing Firestore notes feature: `NoteFragment`, `CreateNoteFragment`, `EditNoteFragment`, `NoteDetailsFragment`, `firebasemodel` and associated layouts/adapters.
- Original student messaging, Task Board Activity, Gemini client, local task ranking and scheduler source retained where possible, although some old screens are no longer navigation destinations.
- Original Firebase configuration in `app/google-services.json`, including its `com.hacms.app` Android client. Live Firebase data was **not** modified from this package.

## Added or extensively reworked
| Area | Source and behaviour |
|---|---|
| Branding | Generated `unitask_lockup.png` from user-supplied artwork without old slogan; added `unitask_icon.png`, adaptive/legacy app icons; `SplashActivity.java` provides fade/slide animation and new slogan. |
| Bottom tabs | Replaced `res/menu/bottom_nav_menu.xml` with Home, Tasks, Notes, Unify and Chats, each with a label. Rewired `res/navigation/nav_graph.xml` to `com.hacms.app` classes. |
| Dashboard | Replaced placeholder `DashboardFragment.java` with live Firestore counts, upcoming tasks, active task cards, personalised greeting/avatar and scheduler shortcut. |
| Tasks | New `TasksFragment.java` directly in the bottom tab, with personal/group tasks, due date/time pickers, CRUD, priority, completion and group assignee selection. |
| Firestore tasks | Reworked `FirebaseTaskRepository.java` and extended `Task.java` with document ID, group, assignee and time fields; saved records persist and synchronise. |
| Chat requests | New `ContactsFragment.java` (exact-email search, pending requests, accept/decline, persistent connections). No longer lists all registered users automatically. |
| Private chat | New `DirectChatFragment.java` stores messages in Firestore under accepted connections, with Firestore security rules. Old RTDB chat classes remain as legacy source, and old RTDB message history is not migrated. |
| Groups | New `GroupFragment.java` creates groups of existing connections, supports group chat and addition of members; `@Unify` invokes Gemini if consent is enabled; group assignments are created from Tasks. |
| AI | Replaced `ChatBotFragment.java` which contained a hard-coded **OpenAI API key**, with Unify through existing `GeminiAiClient.java`, consent and optional task context. On Gemini errors, calls `LocalStudyAssistant.java`. Default model changed to previously observed free-tier `gemini-2.5-flash-lite`; check quotas. |
| App Check | Added separate `src/debug/.../AppCheckBootstrap.java` using Debug provider and `src/release/.../AppCheckBootstrap.java` using Play Integrity. Existing `StudentApplication.java` now works with them through the manifest. Debug credentials are not embedded in release source. |
| Profile | New `ProfileFragment.java`: Firestore display-name update and 160×160 compressed JPEG photo as Base64 in private profile document. Shared `Ui.java` displays initials/photo in header avatar. |
| Settings | New `SettingsFragment.java`: dark mode, AI consent and task-sharing opt-in, profile shortcut and logout. |
| Theming | Material DayNight colours, reusable UI panels and fonts, improved bottom nav colour selector, Notes toolbar colour, login branding. |
| Database rules | `firebase/firestore.rules` for `users`, `tasks`, `study_sessions`, `notes`, `chat_requests`, `connections` and `groups`. Requires manual review and publishing. |
| Documentation | `README.md`, `docs/SETUP_AND_TESTING.md`, this changelog and Firebase rules. |

## Minimal fixes to original source
- `MainActivity.java`: import `android.view.Menu`.
- `TaskBoardFragment.java`: import missing `Intent` (legacy fragment, not selected in main bottom navigation).
- `Task.java`: add missing Firestore `getDocumentId`/`setDocumentId` and new fields.
- `ChatFragment.java`: correct malformed `Intent` constructor in legacy chat screen.
- `NoteFragment.java`: route Edit action to `EditNoteFragment` through Navigation rather than trying to launch a Fragment as an Activity.
- `SignupActivity.java`: save normalized `emailLowercase` for exact-email contact lookup.
- `StudyPlanEngine.java`: support both old `d/M/yyyy` and new `yyyy-MM-dd` due dates.
- `app/build.gradle.kts`: dependencies for Firestore UI, navigation, Realtime DB, Firebase AI, App Check; compatible BoM, build config and resource support.
- `app/src/main/AndroidManifest.xml`: add application bootstrap and logo splash entry, keep original activities.

## Not done automatically
- No changes were made to the live Firebase Console; publish and reconcile rules yourself.
- No Android APK was produced and no emulator/build-on-device verification was possible in this environment.
- Original RTDB private-message history was not copied into Firestore.
- Firebase App Check registration for **`com.hacms.app`** and signed-release certificate SHA-256 were not created; these require console access and a release signing certificate.
- Old OpenAI API key removed from current project source but must still be revoked by the key owner and purged from any exposed Git history where feasible.
- Group membership is client-managed; additional consent/approval and anti-abuse measures are recommended before any public production launch.
