> **OLDER GUIDE / HISTORICAL RECORD** — For the Gemini upgrade, follow [START_HERE_GEMINI.md](START_HERE_GEMINI.md). The earlier text below describes the previous mock-AI build and is retained for version history only.

# Human–AI Collaboration Platform — Final Integrated Prototype

This Android Studio Java/XML project merges the existing team application with a new Firestore-backed Dashboard + Study Scheduler. **Open this as one project — do not copy individual files into another project.**

## A. Before opening Android Studio
1. Download and **Extract All** from `HumanAI_Integrated_AndroidStudio.zip` into a writable folder, e.g. `Documents/AndroidStudioProjects/HumanAI_Integrated_AndroidStudio`.
2. Keep the entire extracted folder, including `gradlew.bat`, `gradle/wrapper`, `settings.gradle.kts`, `app`, and `app/google-services.json`.
3. **Revoke/rotate any OpenAI API key from the original teammate ZIPs**. The integrated app has no OpenAI key.
4. Firebase project from original config: `humanai-collab`. The current application ID is `com.example.human_ai_collaboration_platform`. Do not change package name unless you also add that app to Firebase and download its matching google-services.json.

## B. Open and sync the project
1. Open Android Studio -> **File > Open** (or **Open** from welcome page).
2. Pick the extracted folder containing `settings.gradle.kts`, not the `app` subfolder.
3. Choose **Trust Project** only if you trust the code, and wait for Gradle sync.
4. The teammate-provided project uses **Android Gradle Plugin 9.4.1, Gradle 9.6.0, compileSdk 37, targetSdk 37, minSdk 24, and Java 11 language compatibility**. Install a compatible Android Studio version and SDK Platform 37 from **Tools > SDK Manager**. Google documents AGP 9.4 compatibility with Gradle 9.6.0, Build Tools 36.0.0, and at least JDK 17; an appropriate embedded JDK is recommended. If your Android Studio does not support these, capture the sync error; do NOT arbitrarily downgrade versions before coordinating with teammates.
5. If prompted for JDK/Gradle JDK, choose the compatible **Embedded JDK** from **Settings > Build, Execution, Deployment > Build Tools > Gradle**.
6. Run **File > Sync Project with Gradle Files**. After sync, choose **Build > Make Project** (or Rebuild Project where available). In the Project panel, you may right-click `StudyPlanEngineTest` and choose **Run** to test the pure Java planner.

## C. Firebase console steps (essential)
Your screenshots show Email/Password login enabled, a `users` Firestore collection, and Realtime Database node `chats`. The mobile configuration already points to the same Firebase project; no new Firebase project is necessary.
1. Go to https://console.firebase.google.com/ and choose `HumanAI-Collab` / project ID `humanai-collab`.
2. **Authentication > Sign-in method**: keep **Email/Password enabled** (the screenshot confirms it was enabled).
3. **Firestore Database > Rules**: before changing anything, copy and save your existing rules. Open `docs/FIRESTORE_RULES_ADD_THESE_BLOCKS.txt`; paste the two match blocks **INSIDE your existing Firestore document matching scope**, alongside existing rules. Do **not** replace rules governing `users` or others. Publish only after your team approves the changes. You do not need to create the `tasks` or `study_sessions` collections manually; Firestore creates them automatically on the first successful write.
4. **Firestore > Data**: after successfully creating a task in the app, confirm the new `tasks` collection. Document should include `ownerUid`, `title`, `status`, `priority`, `dueDate`, etc. After saving a study plan, confirm `study_sessions` exists.
5. **Realtime Database > Data**: leave existing `chats` data intact. Verify its Rules with the chat teammate separately; these new Firestore rules are NOT Realtime Database rules.
6. If signup or user list fails, inspect existing `users` Firestore access rules. The app requires a signed-in user to read profiles; each student should only edit their own profile.
7. Ensure the emulator/device can access the internet; Firebase calls cannot succeed offline on first launch.

## D. Run the app
1. Android Studio -> **Tools > Device Manager** -> **Create Device**, choose a Pixel emulator and a compatible Android system image (Google Play/Google APIs image recommended). Or connect a real Android phone with USB debugging enabled.
2. Choose the device and `app` run configuration, then click **Run (green triangle)**.
3. On first launch you should see your teammate's **Login** screen. Sign in with a test account (or use Signup).
4. The bottom navigation opens **Dashboard**, **Tasks**, **Assistant**, and **chat**. The **Tasks** tab shows the teammate's link to Task Board; tap "Task Board" to open the full existing CRUD interface.
5. In Task Board, tap **+ Add Task**. Give the task a title, status, priority and date with the picker; save. The item should appear on the list and in Firebase.
6. Go Back to main, open **Dashboard**: task counts and upcoming/active items should reflect Firestore. Tap the calendar icon in the navy card to open **Study Scheduler**.
7. In Scheduler choose 1–3 daily slots and a start hour; press **Generate study plan** to preview. Press **Save plan to Firebase** and verify documents in `study_sessions`.
8. Open **Assistant**. Ask "What should I focus on?". It returns **mock priority-based recommendations from your actual tasks**, not a live AI answer.
9. Open **chat** and test messaging with a SECOND Firebase user signed into another emulator/device. One account alone cannot validate peer messaging.
10. Exit/reopen app and log back in. Firestore tasks and saved sessions should remain available.

## E. Troubleshooting
- **Gradle sync / unsupported AGP**: install Android Studio that supports the repository's AGP 9.4.1 and SDK 37. Keep team versions intact; share the full error if changes are needed.
- **google-services.json mismatch / No matching client found**: project applicationId must remain `com.example.human_ai_collaboration_platform`, or Firebase console must register a matching Android package and issue a new config file.
- **PERMISSION_DENIED** loading/saving tasks or study sessions: re-check Firestore rules and ensure signed-in user; new queries include `ownerUid == FirebaseAuth.getUid()`.
- **Signup fails**: check Email/Password provider, Firebase project/package and user profile security rules.
- **Chat fails**: check Realtime Database service and its separate rules, plus signed-in peer users.
- **No upcoming items**: use Task Board to add tasks; dashboard deliberately does not invent sample records.
- **AI answers are generic**: expected prototype behavior; live API intentionally disabled due to exposed-key security issue.
- **No task progress % field**: teammate's current Task class stores only status, not numerical progress. The 50% in-progress bar is a visual state indicator.

## F. Test checklist (keep a record)
- [ ] Sign up (new account) and see dashboard after signup
- [ ] Logout and log back in
- [ ] Password-reset email
- [ ] Task Board Create / Edit / Assign / Delete / Suggestions
- [ ] New task appears on Dashboard and task counts update
- [ ] Planner generates sessions sorted by upcoming deadline
- [ ] Save Plan creates Firestore `study_sessions` documents
- [ ] Kill/relaunch: tasks and saved sessions still there
- [ ] Prototype Assistant answers using task data
- [ ] Two-user realtime chat works without breaking bottom navigation
- [ ] Confirm wrong user cannot read another student's tasks/sessions

## G. GitHub integration
Use a new branch `feature/integrated-prototype`. Commit this source tree, not the `.gradle`, `build`, `.idea` or `local.properties` directories. **Avoid committing the original ZIP/source with the exposed OpenAI key**. Open a pull request for review before merging. The local `app/google-services.json` is supplied from the team repo for convenience; it contains Firebase client configuration, not a server-admin service-account key. Apply your team's policy on storing it in GitHub.

## H. Scope and truthfulness
The dashboard and scheduler are real implemented screens with Firebase storage, but final **Android compilation and Firebase runtime behavior are not yet verified** from this environment. The scheduler uses simplified 1-hour sessions for a seven-day window and does not yet handle timetable conflicts, personal workload estimates or overlapping meetings. Assign-to-person text does not grant shared permissions. See `CHANGELOG_AND_INTEGRATION.md` for every addition/change.
