# UniTask: Android Studio, Firebase and phone deployment guide

## A. Before you start

1. Back up the group's current GitHub branch and export the Firestore security rules in Firebase Console. Don't overwrite your teammates' live project or rules without a backup.
2. Install an Android Studio release that supports **Android Gradle Plugin 8.11.2**, JDK **17 or newer**, Gradle **8.13** and **Android SDK Platform 36**. If requested, download SDK 36 in Tools → SDK Manager. Keep the supplied Gradle wrapper.
3. Extract `UniTask_Complete_AndroidStudio.zip` to a regular folder (e.g. `Documents/AndroidStudioProjects/UniTask`). **Do not open the ZIP without extracting.**
4. In Android Studio select **File → Open**. Select the folder containing `settings.gradle.kts`. Trust it only if your group trusts its source code. Wait for Gradle sync and open the Build tool window for errors.
5. Under the project dropdown choose `app`, select an emulator, and select Build → Make Project, then Run. The package/application ID is `com.hacms.app`.
6. Firebase's existing `app/google-services.json` includes the `com.hacms.app` Android client and points to `humanai-collab`. If you replace the configuration, use **Project Settings → General → Your Apps → com.hacms.app → download google-services.json**, and place it at **app/google-services.json**. Do not create another Firebase project.
7. UniTask starts with a short animated logo and then opens the login flow. The original team's signup, email verification and MFA code has been retained. Existing accounts may log in across devices. If Firebase SMS MFA is not configured or you encounter phone-auth billing issues, check Authentication/Identity Platform settings instead of removing MFA silently.

## B. Firebase: configure new app collections

1. Visit `https://console.firebase.google.com/` and select **HumanAI-Collab**.
2. Open **Firestore Database → Rules**. **Copy the entire existing rules into a backup text file before changing them.**
3. Review the supplied `firebase/firestore.rules` file. It covers `users`, `tasks`, `study_sessions`, `notes/{uid}/myNotes`, `chat_requests`, `connections/{id}/messages`, and `groups/{id}/messages`. If you have additional collections/rules in production, merge these sections rather than replacing unrelated rules. Ask an owner to review rules for privacy before publishing.
4. Click **Publish**. You do **not** need to manually create collections; Firestore will create them when the app first writes valid documents.
5. Go to **Firestore → Data** to confirm writes while testing. The `users` collection from your screenshots should remain intact.
6. Existing signed-up profiles may not contain `emailLowercase`. The app first searches by `emailLowercase`, then by exact `email`, so older lowercase email documents still work. For reliable contact lookup, edit existing user documents **using their exact account email lowercased** and add `emailLowercase`. New signups add it automatically; saving a profile also sets it.
7. **Keep existing Realtime Database content unchanged**. The new consent-based direct chats use the new Firestore `connections/{connectionId}/messages` location. Old Realtime Database `chats` history is **not automatically migrated**. If old chats matter, export/back them up before switching. The old teammate chat classes remain in the source but are not wired to the new Chats tab.
8. The Firestore photo feature stores a small JPEG as `photoBase64` inside the student's own profile document, **not in Cloud Storage**. This avoids requiring a paid Firebase Storage bucket but is suitable for a limited academic prototype, not a large deployment.

### Firestore data structures

- `users/{uid}`: `name`, `email`, `emailLowercase`, `role`, optional `photoBase64`.
- `tasks/{taskId}`: `ownerUid`, `assigneeUid`, `groupId` (empty for personal), `title`, `description`, `dueDate` (`yyyy-MM-dd`), `dueTime` (`HH:mm`), `status`, `progressPercent`, `priority`, `assignedTo`, `createdAt`.
- `study_sessions/{uid_day_time}`: `ownerUid`, `taskId`, `taskTitle`, `day`, `time`, `durationMinutes`, `createdAt`.
- `chat_requests/{senderUid_receiverUid}`: `senderUid`, `receiverUid`, `status` (pending, accepted, declined), `createdAt`.
- `connections/{sortedUidPair}`: `memberUids`, `createdAt`; nested `messages/{messageId}`: `senderUid`, `text`, `createdAt`.
- `groups/{groupId}`: `ownerUid`, `name`, `memberUids`, `createdAt`; nested `messages/{messageId}`: `senderUid`, `senderName`, `text`, `isBot`, `createdAt`.
- `notes/{uid}/myNotes/{noteId}`: existing teammates' note structure.

### Two students test collaboration

1. Register/login with **two separate Firebase accounts** on two devices (or use two emulators).
2. Account A → Chats → type **account B's exact email address** → **Find student & request chat**. Account B → Chats → **Accept request**.
3. Both now see the other under **Your connections**. Tap to exchange private messages. Previously unaccepted registered users are not listed.
4. Account A → Chats → **Groups and collaboration** → **Create study group** → choose accepted contact B.
5. From Tasks → **+ Add task** → choose the study group → choose B from the assignment dropdown, and set a due date/time.
6. On B's device → Tasks, the assigned item appears. Mark complete, and confirm A sees the updated status. Open the group to send a message or start a message with `@Unify` (AI consent needed).
7. Check Firestore records and confirm user C cannot open these records. The supplied rules restrict member access, but because this is client-managed prototype group membership, it is **not a production-grade invitation/role system**: existing members can add contacts without a second group-specific acceptance step.

## C. Firebase AI Logic (Unify)

1. Firebase Console → **AI Logic** → **Get started** → **Gemini Developer API**. Select the supported free-tier model, and **do not set up billing** if free usage is required.
2. The chatbot uses `gemini-2.5-flash-lite` in `GeminiAiClient.java`, matching the free tier shown previously for this Firebase project. Model access, deprecation and limits may change: **Google AI Studio → Rate limits → HumanAI-Collab** must show nonzero quota for that precise model. If not, change only `GeminiAiClient.MODEL` to an eligible available model. Gemini free quotas are not unlimited.
3. The new code does not embed a Gemini or OpenAI server API key. It uses Firebase AI Logic SDK and Firebase App Check. Old exposed OpenAI credentials must be revoked at `https://platform.openai.com/` by the owner, even if removed from the latest ZIP; old Git commits may still contain them.
4. The app asks consent before a student's question is sent to Google. **Task titles are only sent if the student separately opts in in Settings.** The app falls back to locally computed task priorities/schedules when Gemini returns an error. Local advice is clearly labeled as local, not as live AI.
5. Group messages beginning with `@Unify` send the question to Gemini **only if that device has AI consent enabled**. Group members can read the resulting bot-labeled message; in this prototype bot messages are written by the requesting student and are **not cryptographically authenticated bot identities**. A trusted backend would be needed for that guarantee.

## D. App Check: emulator (debug)

1. Android Studio's **debug** build uses Firebase App Check's `DebugAppCheckProviderFactory`. Use Run → select emulator → open Logcat → search `tag:DebugAppCheckProvider`.
2. The debug token is printed during startup or on an AI request. Treat it as a credential; **don't share it, screenshot it publicly or upload it to GitHub**.
3. Firebase Console → **Security → App Check → Apps** → find Android application **`com.hacms.app`** (not the older `com.example.human_ai_collaboration_platform` registration) → **Manage debug tokens** → register the emulator's token. If unavailable, the App Check registration/terms problem must be resolved by a Firebase project owner.
4. Run the app again. The token is usually stable on that emulator until its app data is cleared. Each teammate's debug emulator needs its own token. Avoid enabling App Check enforcement for all Firebase products until all legitimate clients are configured.
5. For any leaked debug token, revoke it in the Firebase Console and clear app storage before obtaining a replacement.

## E. Physical phone: two ways

### Quick device development test (debug APK)

1. On the phone open **Settings → About phone → Build number** and tap seven times to enable Developer options (wording varies).
2. Open **Developer options → USB debugging**, then connect the phone to your computer with a data-capable USB cable and approve the debugging prompt. On Windows, install the manufacturer's USB driver if Android Studio does not detect the device.
3. Android Studio → device selector → select your phone → **Run**. It installs a **debug build**. This is easiest to inspect in Logcat, but AI may require that phone's own App Check debug token too. This is normal, because a debug APK deliberately uses the debug provider.

### Share a release build without individual debug tokens

1. In Android Studio choose **Build → Generate Signed Bundle / APK → APK**. Create a **release keystore** or select the group's existing signing key. Store the keystore and passwords outside GitHub, backed up safely.
2. Build **release**. The release source set uses `PlayIntegrityAppCheckProviderFactory` automatically (the debug provider is excluded from release code).
3. Obtain **SHA-256 of the RELEASE signing certificate**, not the debug SHA, e.g. with Android Studio's signing report or `keytool -list -v -keystore your-release-keystore.jks -alias yourAlias`. Register it for **`com.hacms.app`** in Firebase Project Settings and **App Check → Play Integrity**.
4. In the Play Integrity setup, the Firebase project owner may need to link the Google Play Integrity API/Google Cloud project and accept terms. For installations outside Google Play, choose **PLAY_RECOGNIZED: Not required**, **LICENSED: Not required**, and the recommended **Device integrity** setting; these choices must agree with your actual distribution. See official Firebase documentation.
5. Distribute the signed APK to teammates using a trusted file share. They allow **Install unknown apps** for the chosen file manager/browser and install it. If installing over an existing build with a different signature, they may have to uninstall the old app first (local cache will be removed; Firestore accounts and tasks persist).
6. **Physical-device AI without debug tokens is conditional**: Firebase App Check / Play Integrity registration must actually succeed, the device must pass required integrity checks, and the Gemini model must have quota. A sideloaded release can still fail attestation on unsupported or modified phones; the local planner remains available.
7. Do **not** send your keystore, signing passwords or debug tokens with the APK.

## F. Suggested end-to-end testing

- [ ] Fresh install shows animated UniTask logo, the right slogan and the app icon.
- [ ] Email validation, account verification, login, MFA and logout behave according to the Firebase project configuration.
- [ ] Dashboard greets logged-in student, shows proper counts and dates, and clicking avatar opens Profile.
- [ ] Personal task CRUD, due date AND due time, task completion and sorting work; data survives closing/reopening.
- [ ] Group tasks assigned from A appear on B's device and completion status synchronises.
- [ ] Notes can be created, edited and removed, and notes remain after sign-in on another device.
- [ ] Contact search shows only exact matches, requests need acceptance, accepted contacts have persistent chats.
- [ ] Group chat and `@Unify` work with permission; nonmembers cannot read messages in Firestore rules.
- [ ] Gemini gives a real AI answer with registered App Check. Simulate failed requests by disconnecting network; local fallback still answers task/schedule questions.
- [ ] Dark mode persists after force-closing and reopening the app.
- [ ] Profile name and photo update on a second signed-in device.
- [ ] Release APK (not the debug build) can run on a real phone with Play Integrity configured.

## G. Important limitations and troubleshooting

- **No full build/emulator/live Firebase verification was possible in the authoring environment.** Fix any Gradle compile or runtime errors reported by your own Android Studio before sharing the APK. Look in Build Output and Logcat; do not assume a ZIP is production-tested.
- If email/password works but SMS MFA does not, check Firebase Authentication with Identity Platform enablement, SMS region policies and any applicable SMS charges or quotas. Do not disable your teammate's MFA silently.
- If Firestore says PERMISSION_DENIED, compare the live rules with `firebase/firestore.rules`, confirm the signed-in UID, group membership, query structure and app's `com.hacms.app` Firebase registration.
- If Gemini fails or local fallback is always used, check `AI Logic` enabled, model access/RPD, valid App Check token and app package. Full AI availability depends on Google's current service status and free quota.
- If joining a group is not permitted, both people must first be connected; this prototype does **not** yet offer separate accept/decline for a group invitation. Do not publish to general users without addressing group consent and server-authorised membership management.
- Old direct messages in Realtime Database are not automatically migrated to the new Firestore direct-message paths; do not delete that history without an agreed migration.
- The Notes pages retain much of your teammate's XML, so they may not match the reference mockups pixel-for-pixel. Some older Task Board/Realtime Chat classes are retained for preservation but no longer used in navigation.
- Full collaboration read/write roles, push notifications, media sharing, email verification enforcement in rules, anti-spam and abuse protections, a group bot with verified server identity, and large-scale profile image storage would require further development before a public production launch.

## References
- Firebase App Check Play Integrity: https://firebase.google.com/docs/app-check/android/play-integrity-provider
- Firebase App Check debug provider: https://firebase.google.com/docs/app-check/android/debug-provider
- Firebase AI Logic Java setup: https://firebase.google.com/docs/ai-logic/get-started
- Android real devices: https://developer.android.com/studio/run/device
- Firebase Cloud Storage billing conditions: https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024
