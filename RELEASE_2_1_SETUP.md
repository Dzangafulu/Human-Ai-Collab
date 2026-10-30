# UniTask 2.1 — Installation, Firebase setup and test instructions

## Important limits
This package is an updated **Android Studio source project**. It has not been compiled into a tested APK in this environment, and Firebase console changes cannot be made by copying Java files. Do not upload the project's old embedded OpenAI keys or App Check debug tokens. Real Firebase SMS verification cannot be guaranteed free: Firebase currently lists verification-code SMS sending as **Blaze-only/pay as you go**, and true SMS MFA requires Firebase Authentication with Identity Platform. Test phone numbers in the Firebase console do not receive SMS; their verification codes are intentionally fixed. The code can handle real phone numbers only after the Firebase project has SMS MFA and permitted regions correctly configured.

## A. Install/update Android Studio
1. Save your current working project, including the Java fixes you made after installing the previous ZIP.
2. Extract `UniTask_2_1_Sessions_Notifications.zip`. **Open the root folder containing `settings.gradle.kts`** using Android Studio → File → Open. This is an independent copy, so you can compare it against your original before replacing anything.
3. Allow Gradle Sync. The project retains `com.hacms.app`, SDK 36, the existing Firebase `google-services.json` and the existing database rules document. The new dependency `androidx.work:work-runtime:2.11.2` supports local reminders and `androidx.lifecycle:lifecycle-viewmodel:2.9.4` retains the Unify conversation while tabs change.
4. Select Build → Make Project. Choose an emulator or physical device and press Run. Grant the notification permission prompt on Android 13 and later.
5. If Gradle requires any missing SDK components, install them through Tools → SDK Manager.

## B. What changed in the application
- Home icon uses an outlined **house**. Unify no longer has the decorative star emoji in its heading, AI-message labels or group labels.
- Dashboard lists **upcoming sessions below the study plan button**, live from the signed-in user's `study_sessions` records. Old sessions disappear from the displayed list after the start time passes (checked approximately once per minute). Old Firestore records are **not automatically deleted**.
- Study Scheduler now supports **manual named sessions**, date picker, arbitrary start time picker, and 30/60/90/120-minute duration.
- Automatic planning uses unfinished tasks, deadlines and a configurable starting time, labels sessions with their task name and **saves the generated sessions to Firestore immediately**. No separate Save button. Rerunning a generated plan updates existing matching date/time slots rather than duplicating them.
- In Unify, a command such as `Schedule my Science test` or `Create study sessions for my assignments` uses the local task planner to **save real sessions**, independent of Gemini quota. It does not pretend Gemini has completed an action. The tasks must already exist in Firestore.
- Local Android notifications are created for saved study sessions (**about 10 minutes before**) and upcoming task deadlines (**about 24 hours before**). WorkManager delivery is **approximate**, and Android battery restrictions may delay reminders. The app asks for notification permission on Android 13+; notification scheduling uses no paid Cloud Functions or FCM backend.
- Unify's in-app message history stays in an Activity-scoped ViewModel when switching between navigation tabs, but it is **not permanently saved** across process termination, sign-out or device changes.
- Phone entry accepts a SA 10-digit number starting in 0 (converts to +27) or E.164 number starting with +. The SMS code must be six digits, and the UI supports resending. **Real SMS delivery still depends on Firebase settings** (see below).
- Fixed earlier source issues: the Dashboard greeting view cast, the contact-name lambda capture, and the `words` variable shadowing in `Ui.java`.

## C. Firebase: real numbers and SMS MFA (critical)
1. Go to https://console.firebase.google.com → project **HumanAI-Collab** → **Authentication**.
2. Open **Sign-in method**. If the project is still using only free Firebase Authentication, **real SMS MFA is not automatically available**. Follow Google's documented **Firebase Authentication with Identity Platform** upgrade process if you intend to enable multi-factor phone enrollment; check charges with your team before enabling it.
3. In the **SMS multi-factor authentication** settings, verify that SMS MFA is enabled. Check the **SMS region policy** and allow South Africa (and other intended regions) rather than leaving all regions blocked.
4. Under Project settings → Your apps, verify Android package `com.hacms.app` has the correct SHA-1 and SHA-256 fingerprints from the signing certificate (`.\gradlew.bat :app:signingReport` on Windows). App Check Play Integrity is separate from phone authentication.
5. In Authentication → Sign-in method → **Phone numbers for testing**, any registered test numbers are fictitious: the configured code is fixed and **no real SMS is sent**. Do not use a real student's number as a fake test number. Remove test-number reliance for production; real numbers work only if the project/provider/SMS quotas permit it.
6. SMS MFA currently remains **required** by your team's LoginActivity for verified users. If you choose to stay on a free plan that cannot send real SMS, you will need a **different agreed authentication design** (for example, email-only with optional different MFA), not merely another Java code patch.
7. To test a newly enrolled account, verify its email, log in with that account's password, enter its own eligible phone number, request the SMS, and enter **the code actually received**. Sign out/in again to test the second-factor challenge.

Sources: https://firebase.google.com/docs/auth/android/multi-factor ; https://firebase.google.com/docs/auth/limits ; https://firebase.google.com/docs/auth/android/phone-auth

## D. Change Firebase verification emails from HumanAI-Collab to UniTask
This change is made **in Firebase Console, not in the Android source**:
1. Open Project settings → **General**, look for **Public-facing name**, set it to `UniTask`, and save. This is the name used in emails sent to users.
2. Go to Authentication → **Templates**, review Email address verification and Password reset templates. Set the sender display name and email subject to use UniTask where the template allows it. Do not remove the verification action-link placeholders.
3. Send a fresh test verification email. Previous emails already sent will not be changed.
See: https://support.google.com/firebase/answer/9137752 and https://support.google.com/firebase/answer/7000714

## E. Firestore and database updates
No manual task/session document is needed. The existing `study_sessions` collection is reused. New saved records contain:
- `ownerUid` (String), `taskId` (String), `taskTitle` (String)
- `day` (`yyyy-MM-dd` String), `time` (`HH:mm` String)
- `startAtMillis` (Number), `durationMinutes` (Number), `createdAt` (Number)

Old saved sessions without `startAtMillis` still display because the app parses their existing `day`/`time`. Existing Firestore owner-based `study_sessions` rules remain appropriate; **do not overwrite your team's other collection rules**. No special composite index is required for the existing owner UID equality query. Task records are unchanged. Do not manually delete old data just because a session has passed; hiding is handled client-side.

## F. Gemini / App Check
The existing Gemini integration and automatic local fallback are retained. The model is updated to the free-tier-eligible `gemini-3.5-flash-lite` because the prior Gemini 2.5 Flash-Lite has limited legacy availability. Verify in Google AI Studio that **your actual project has a nonzero quota for this model**; otherwise live responses will fall back to local planning. This patch does **not** add a Gemini server key. Existing debug-token/Play Integrity registration for `com.hacms.app` is still needed according to Firebase enforcement. Gemini availability depends on the **actual model and current project's API quotas**. It is not necessary to call Gemini to produce basic task-derived study sessions.

## G. Test cases in order
1. Login using an account already verified and (where configured) MFA-enrolled.
2. Home: confirm bottom-navigation **house icon**, normal text **Unify**, Dashboard shows no saved sessions initially.
3. Study Scheduler → enter `Science revision`, select tomorrow, choose a non-prescribed time e.g. `20:25`, choose 90 minutes, press **Add study session**. Confirm it appears below the button on Home and in Firebase Firestore → `study_sessions`.
4. Create a Task named `Biology test` due several days away, with a due time. In Scheduler choose 2 sessions/day, choose `17:15`, press **Generate and save study plan**. Confirm new titled sessions appear in Home and the database without pressing another Save button.
5. With notifications allowed, create a study session **15 minutes from now**. A notification should be scheduled for roughly 5 minutes later (the requested 10-min lead). WorkManager notifications may be delayed; test on an awake device and verify notification permissions.
6. Unify: send a text, switch to Tasks and back; the text should still be present. Type `Schedule my Biology test` and verify Firestore session creation. The chat conversation does not persist if you completely kill the app.
7. Register another student on an actual number only after the required Firebase SMS MFA setup. **Do not assume free Spark projects can deliver live SMS.**
8. Sign out/in, verify Firebase account, tasks and schedules are still there. On another device log in using the same account to see cloud data.

## H. Troubleshooting
- **BUILD error**: open Build Output, click the first red error and verify Java file/line. Share that error if it occurs. Static source/XML and Java-only planning checks passed here, but an Android emulator build was not available to run.
- **MFA: one number/one code**: confirm whether it is listed under `Phone numbers for testing`; those are fixed codes and cannot be used as live SMS. Check Authentication with Identity Platform, SMS region policy, SHA fingerprint and pricing.
- **Firestore PERMISSION_DENIED**: confirm user is logged in and your `study_sessions` rule allows only the authenticated `ownerUid`.
- **No reminder**: check Android Settings → Apps → UniTask → Notifications, and Android's battery/background restrictions. A permanently force-stopped app may not deliver until opened again.
- **No Gemini response**: verify Firebase AI Logic, Play Integrity/App Check on package `com.hacms.app`, model quota and Logcat. Local plan creation should still work.
