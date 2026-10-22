# FINAL TEAM APP — Step-by-step Android Studio + Firebase Gemini setup

**Start with this guide.** This ZIP contains the **full integrated team application**, not just an AI fragment. The chatbot now makes *real* Firebase AI Logic → Gemini Developer API requests; the original team interface remains. If the API is unavailable or hits a quota, the Android app uses the pre-existing Java study-priority/scheduler logic, clearly labeled as **Local study mode**. It is not a real conversational AI when running locally.

## 0) What is and is not proven

- Java and XML source were integrated and mechanically checked, with a Java-only fallback/unit smoke test.
- **NOT proven:** Android/Gradle compilation, emulator launch, live Firebase AI response, runtime Firestore/Chat permissions. These require your Android Studio installation and access to your Firebase Console.
- The model configured in `GeminiAiClient.java` is `gemini-3.5-flash-lite`, which Google's current Firebase model documentation describes as compatible with the Gemini Developer API free tier. **The exact RPM/RPD for this newer model in YOUR project have NOT been confirmed by the screenshots**; the screenshots showed 20 RPD for Gemini 2.5 Flash and Flash-Lite. A newer model can have different quotas, including zero if not granted to your project. In AI Studio turn on **All models** and check the 3.5 Flash-Lite row; do not assume you have the 20 RPD quota for 3.5.
- No payment method, Blaze plan, OpenAI key, Gemini Developer API key, Groq or own server is required for this design if a supported free Gemini model is available.
- The project's original Gradle toolchain uses AGP 9.4.1 / Gradle 9.6 / SDK 37. If your Android Studio cannot support it, you will need a compatible version; do not blindly change teammate versions.

## 1) Protect the old OpenAI key FIRST

1. Ask the owner of the OpenAI API account to open https://platform.openai.com/api-keys .
2. **Revoke/delete the old exposed key** from `ChatBotFragment.java` in the *original* uploaded project. Generate a new key only if you later create a secure server integration (this app does not use OpenAI).
3. Check OpenAI usage/billing for unrecognized requests. Removing the key from the new ZIP does **not** revoke it or remove it from old GitHub commits.
4. If the original key was pushed to GitHub, coordinate credential/history cleanup with the repository owner; do not upload the old project ZIPs again.
5. Never place Gemini or OpenAI server/API secrets in Android Java, XML, `local.properties`, or GitHub. `app/google-services.json` is ordinary Firebase *client configuration*, NOT a service-account private key, but still follow your team's repository policy.

## 2) Confirm existing Firebase project and collections

1. Open https://console.firebase.google.com/ and select **HumanAI-Collab** (project ID `humanai-collab`).
2. Keep Firebase **Spark / no-cost**, DO NOT click Set up billing or upgrade to Blaze for this setup.
3. Authentication → Sign-in method → verify **Email/Password** enabled (already shown in screenshots).
4. Firestore → Data → existing `users` collection (shown in screenshots).
5. Realtime Database → Data → existing `chats` node (shown in screenshots).
6. Do **not** create another Firebase project; do **not** delete these collections.
7. Firestore Tasks and `study_sessions` are created automatically on first successful save; rules must permit appropriate signed-in writes (Step 4).

## 3) Activate Firebase AI Logic (required for Gemini)

1. In the SAME Firebase project, open **AI services → AI Logic** (may appear under Build / AI services).
2. Click **Get started**.
3. Select **Gemini Developer API**. **Do not select Agent Platform Gemini API / Vertex AI**; that path can require billing.
4. Follow the guided setup to enable APIs for the project. When registering/checking the Android app, use package/application ID exactly `com.example.human_ai_collaboration_platform` (check Project Settings → Your apps).
5. Keep the original matching `app/google-services.json` from the ZIP. If Firebase instructs you to update it, download the config under Project settings → Your apps → this Android app and *replace only* `app/google-services.json`, then sync Gradle.
6. If the setup enforces **App Check**, that's expected. The new code sets debug provider for debug builds and Play Integrity for release builds. Finish Step 6 before expecting Gemini calls to work.
7. Check free rate limits at https://aistudio.google.com/ → Rate Limit → your project → All models. Find `Gemini 3.5 Flash-Lite` (or another verified free-tier model). Keep the Spark plan. **If its RPD is 0 or you cannot access it, tell me the exact model rows before changing model names**.

**You do not need to generate, copy, or paste a Gemini API key into Android Studio.** Firebase AI Logic uses its proxy service, which handles the Gemini credential server-side.

## 4) Firestore rules for tasks and schedules (already needed in previous integration)

1. Firebase Console → **Firestore Database → Rules**.
2. Copy the existing rules into a backup file on your computer (e.g. `firestore_rules_backup.txt`).
3. Open this ZIP's `docs/FIRESTORE_RULES_ADD_THESE_BLOCKS.txt`. Merge the two `match /tasks/{taskId}` and `match /study_sessions/{sessionId}` blocks into the existing `match /databases/{database}/documents { ... }` scope, leaving the `users` block intact.
4. Press **Publish**. This changes live permissions; ask the team before changing rules in a shared project.
5. Realtime Database chat rules are **separate**. Do not paste Firestore rules into Realtime Database.
6. The app's tasks query uses `whereEqualTo("ownerUid", signedInUid)`; each signed-in user can read/write only their own tasks/sessions under the provided rules.
7. If you see `PERMISSION_DENIED`, check both the rules and that you are signed in. Avoid `allow read, write: if true`.

## 5) Open the complete project in Android Studio

1. Download `HumanAI_Gemini_Integrated_AndroidStudio.zip` and **Extract All** (do not open the ZIP directly). Put it in `Documents/AndroidStudioProjects/` or another writable directory on Windows.
2. Launch Android Studio → **File → Open** (or Open on the start screen).
3. Choose the extracted folder `HumanAI_Gemini_Integrated_AndroidStudio` containing `settings.gradle.kts`, `gradlew.bat`, `gradle/`, and `app/`. **Do NOT select the `app` folder alone.**
4. Click **Trust Project** only if you trust the code. Wait for Gradle sync and SDK downloads.
5. This project uses teammates' AGP **9.4.1**, Gradle **9.6**, compile SDK **37**, min SDK 24, Java 11 source compatibility. Use an Android Studio version supporting AGP 9.4.1, install SDK Platform 37 via Tools → SDK Manager, and use a compatible Gradle JDK (at least JDK 17; choose the embedded JDK if compatible).
6. Verify `app/build.gradle.kts` contains `firebase-ai`, `firebase-appcheck-debug` (debug build), `firebase-appcheck-playintegrity` (release build), and `guava` Java futures. **They are already added; don't add duplicates.**
7. Select **File → Sync Project with Gradle Files**; then **Build → Make Project**. If the sync/build fails, open **Build Output** and copy the **first meaningful error** rather than editing dependency versions at random.
8. The Android app ID must stay `com.example.human_ai_collaboration_platform` unless you also update Firebase registration and `google-services.json`.

## 6) Register App Check DEBUG token (IMPORTANT for emulator testing)

Firebase AI Logic now often enforces App Check; without registration the Gemini API call may fail even though login/Firestore work.

1. In Android Studio, select **Build Variants → app → debug** (default for Run).
2. Create emulator: **Tools → Device Manager → + → Create Virtual Device**. Use a Pixel profile and a recent Google APIs / Google Play image, or use a physical Android phone with USB debugging.
3. Run the app **once** (green triangle). In Android Studio → **View → Tool Windows → Logcat**, select the device and app process.
4. Search Logcat for **`DebugAppCheckProvider`** or **`Enter this debug secret`**; the SDK normally prints a debug token for this installation.
5. Keep that token private. Firebase Console → **Security → App Check → Apps** → find the Android app with package `com.example.human_ai_collaboration_platform` → **Manage debug tokens** → **Add debug token** → paste it → Save. Exact console menu wording can vary; under the three-dot menu of your Android app, look for Manage debug tokens.
6. If the Android app isn't registered for App Check yet, register it first in App Check; the production attestation option is **Play Integrity**. For emulator testing, the registered **debug token** is what the debug build uses.
7. Rerun the app and try the assistant. Do not paste the debug token into Java/XML, messages, screenshots, GitHub, or the ZIP. Do NOT distribute the debug build as production.
8. For later production/public distribution: in Firebase App Check, register **Play Integrity** for the Android app and its signing fingerprint. The release build uses the Play Integrity provider. Its integration needs to be tested on real devices before release. App Check is a security layer, not absolute prevention of abuse.

Official: https://firebase.google.com/docs/app-check/android/debug-provider and https://firebase.google.com/docs/app-check/android/play-integrity-provider

## 7) Run and test — full app

1. Choose a running emulator/device and **app** run configuration; click **Run**.
2. On login/signup use a **test account** through Firebase Authentication. Login screen and user registration are your teammate's original implementations.
3. Main bottom navigation opens Dashboard / Tasks / Assistant / chat.
4. Open Tasks → **Task Board** (tap the Task Board label) → add tasks with a due date. Add at least two tasks, e.g. "Database assignment" due soon and "Java revision" due later. Check Firebase Console → Firestore → `tasks` for documents with `ownerUid`.
5. Go to Dashboard → confirm updated counts and task cards. Tap the calendar icon → Scheduler → Generate, review, Save → Firestore `study_sessions`.
6. Go to **Assistant**. The first send shows a **consent dialog**: AI messages go to Google's service. Select **I agree** to use real Gemini. This consent is saved locally per signed-in Firebase UID. If you cancel, nothing goes to Gemini.
7. Type: **"What is Java inheritance? Give a simple example."** With AI Logic/App Check/model configured, a real Gemini response should appear. Status under the top bar shows the live model.
8. To test AI with your task data, check **"Include my task titles and deadlines in Gemini replies"**. Then send: **"What should I focus on tonight?"** Gemini receives up to 5 unfinished task titles/deadlines/priorities for tailoring. Uncheck to stop sending this context. It is **off by default**.
9. If Gemini returns quota exceeded, network failure, App Check rejection, unavailable model, or another error, the chatbot status changes to **Local study mode** and shows local Task Board recommendations. For general knowledge questions, it correctly says subject explanations are unavailable in local mode. This fallback doesn't generate free-form subject explanations.
10. **Test fallback without exhausting quota:** enter a task-related question and tap **Use local study recommendations (no AI request)**, or leave the question blank for the default priority suggestion. This reads Firestore but does not send a Gemini API request or require accepting Gemini consent.
11. The app prevents overlapping requests and adds a 12-second **per-device** delay between AI requests to conserve quota. This does NOT create a server-enforced per-user daily quota. Your project's Gemini RPD/RPM/TPM are shared by all users.
12. Test the teammate's **student-to-student messaging** with TWO signed-in accounts/devices. This is Realtime Database, separate from Gemini.
13. Close/relaunch the app, sign back in, check task persistence and saved schedules.

## 8) How to recognize the mode in the UI

- **Gemini is thinking…**: network request running.
- **Gemini response • gemini-3.5-flash-lite**: answer came from the AI model.
- **Gemini quota reached • Local study mode**: quota response detected; local task rule generator used instead.
- **Gemini unavailable • Local study mode**: AI failed due to an error; task recommendation generator used.
- If no tasks or Firestore is unavailable: the local mode tells users what information is missing.
- **Local study mode cannot answer general subject questions**. This is deliberate, to avoid pretending a static algorithm is real AI.

## 9) Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Gradle sync fails / unsupported AGP | Android Studio / JDK / SDK mismatch | Upgrade Android Studio / install platform 37 and compatible JDK; retain team versions until diagnosed |
| No matching google-services client | Firebase Android package differs from `applicationId` | Use `com.example.human_ai_collaboration_platform` or obtain matching `google-services.json` |
| AI always falls back, permission 403 / App Check | AI Logic not enabled or App Check token not allowlisted | Firebase AI Logic Get Started; register emulator debug token; inspect Logcat |
| AI always falls back, 404 model | Model name not available to your project | Check AI Studio → Rate Limit → All models and Firebase supported models; verify before choosing another |
| AI falls back, 429 quota | Shared per-model RPM/RPD/TPM exhausted / overload | Wait for corresponding quota; do not enable billing unless team approves |
| Task Board says permission denied | Firestore `tasks` rules not published or UID mismatch | Apply supplied rules and verify auth |
| Scheduler cannot save | Missing `study_sessions` rules | Merge supplied rules and sign in |
| No progress percentages | Current Task Board stores statuses only | In-progress bar is an illustrative status indicator, not a measured 50% complete |
| Chat peer messages fail | Realtime Database rules / second user | Review Chat permissions; unrelated to Gemini |
| AI messages count much higher than expected | All Android users share quota | Reduce test load; run local planner; view actual quota in AI Studio |

## 10) GitHub submission (without overwriting team work)

1. Push the NEW integrated project to a **separate branch** (for example `feature/gemini-final`). Do **not** copy the old secret-containing chatbot source from a previous ZIP.
2. Do not push `local.properties`, `.gradle/`, build outputs, tokens, `*.keystore`, or passwords. `.gitignore` is included.
3. Open a Pull Request, list changed/new files from `docs/GEMINI_CHANGELOG.md`; ask your teammates to review; merge only when agreed.
4. The `app/google-services.json` in this ZIP is from the team project, and is client-side config, not an OpenAI/Gemini server key; follow team policy before committing it.
5. If you want, share **the first Build Output error** or a screenshot of Logcat with secrets redacted, and we can debug the final build without rewriting other teammates' screens.

## Official sources (current documentation)

- Firebase AI Logic start: https://firebase.google.com/docs/ai-logic/get-started
- Supported model list and deprecations: https://firebase.google.com/docs/ai-logic/models
- Gemini quotas: https://firebase.google.com/docs/ai-logic/quotas
- Firebase AI Logic Spark pricing: https://firebase.google.com/docs/ai-logic/pricing
- App Check debug: https://firebase.google.com/docs/app-check/android/debug-provider
- App Check production: https://firebase.google.com/docs/app-check/android/play-integrity-provider
- Firebase AI Logic errors: https://firebase.google.com/docs/ai-logic/error-codes
