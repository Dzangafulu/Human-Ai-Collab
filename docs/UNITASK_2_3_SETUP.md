# UniTask 2.3 — Focus Mode, unread chat indicators, and attachments

## What this package contains

This is the Android Studio **source project** built from the `UniTask_2_2.zip` supplied by the team. It preserves the existing Firebase Authentication with authenticator-app (TOTP) MFA, Tasks, Notes, Dashboard, Scheduler, Unify/Gemini and group collaboration. Added features are a fixed group-chat composer, per-conversation read receipts, unread badges, optional small-file attachments, and opt-in best-effort Focus Mode.

**Important:** This source is not a verified APK. Android SDK/Gradle are unavailable in the build environment where this archive was prepared; a full Android build and device verification must be performed in Android Studio. Do not push to the group's main branch until it builds and the tests below pass.

## 1 — Run in Android Studio

1. Keep a backup of your existing working `UniTask_2_2` Android Studio project and Git branch.
2. Extract `UniTask_2_3_Focus_Chat.zip`. Open the **`UniTask_2_3`** folder containing `settings.gradle.kts` via Android Studio > **File > Open**. Do not import individual Java files.
3. Allow Gradle Sync. Install **Android SDK Platform 36** if prompted; use a compatible Gradle JDK (typically the Android Studio bundled JDK).
4. Run **Build > Make Project** and resolve any local build errors before installing. Select a device and press **Run**. The app package remains **`com.hacms.app`**, so it connects to the existing Firebase app configuration.
5. Sign in using your existing account. **The team's authenticator-app MFA source was not modified.** Verify the existing tasks/notes/Unify still work.

## 2 — Firebase configuration — required for messages and unread counts

No second Firebase project is required. The ZIP contains `firebase/firestore.rules`. Those rules add two new **`read_receipts`** subcollections and validation for small attachments. If your group's live rules have changed since UniTask 2.2, **merge only these additions**; do not blindly replace the live rules.

1. Firebase Console > HumanAI-Collab > **Firestore Database > Rules**.
2. Copy existing rules to a safe file (backup).
3. Merge/update the rules for `connections/{connectionId}/messages`, `groups/{groupId}/messages`, and the new `read_receipts/{uid}` child collection under each. Review the supplied file to ensure it preserves other existing collections.
4. Publish the rules after review.
5. **No collections need manual creation.** `read_receipts` documents are created when a user opens a conversation. Attachments are saved as optional fields on the message documents (`attachmentBase64`, `attachmentName`, `attachmentMime`).
6. **Firestore, not Cloud Storage** is used for attachments; no Cloud Storage bucket or Blaze migration is needed for this implementation. Only images that compress to <=250 KB, and documents <=250 KB, can be sent. Larger documents require a future storage backend. Repeatedly downloading attachments in Firestore can consume your Spark read/bandwidth quotas: test with small files, not hundreds of images.
7. The new read receipt is private to its user. Other group members cannot modify your read status. Unread counts are calculated from the most recent 50 messages per conversation while the app is running, so for very busy conversations the badge may undercount older unread messages.

For production-grade attachments and very large groups, use an appropriate object store and a backend rather than encoding files in Firestore messages.

## 3 — Chat tests (two different accounts/devices)

1. Sign in as student A and student B in two emulators or physical phones. If they are not yet contacts, send a request by email and accept it.
2. Leave student A on the Dashboard. Send a direct message from student B.
3. Student A should see a **Chats badge** without leaving the app. Open Chats and check the **`N new`** indicator beside the relevant person. Open the conversation: the badge should clear after Firestore sync.
4. Repeat for a group message. **The group composer must stay at the bottom** while messages scroll above it, even when many messages are sent.
5. Tap **+** in either chat > Attach image or document. Select a photo or a small PDF, TXT, DOC or DOCX. The recipient can use **Save attachment** to choose a destination using Android's file picker.
6. Close and reopen UniTask. The unread status should reload from Firestore. **There is intentionally no push notification when UniTask is closed.**

### Privacy and free tier

Only members of a connection/group should be able to read its messages and attachments, as enforced by the provided Firestore rules. Attachment data is downloaded whenever the enclosing message document is loaded; do not upload personal or sensitive documents in this prototype.

## 4 — Focus Mode setup on a *physical Android phone*

Option B is **best-effort**, opt-in app monitoring, **not device-owner or system-level app disabling**. The student can revoke permissions in Android settings. On some OEM phones, background restrictions or Accessibility policies can interrupt this service. Focus Mode does **not** guarantee that a selected app cannot open.

1. Open UniTask > **Profile > Settings > Focus Mode**.
2. Turn on **Enable Focus Mode**. Read the distinct accessibility disclosure, then choose **I agree** only if comfortable with this opt-in feature.
3. Tap **Choose apps to avoid**; select e.g. Instagram and YouTube. The selection is saved **on that phone only**.
4. Tap **Grant app monitoring (Accessibility)** and open the phone's Accessibility settings. Under **Installed apps / Downloaded apps / UniTask Focus Mode**, enable the service. On some sideloaded Android builds, the system may ask you to explicitly allow restricted settings from UniTask's App info page. The user must choose whether to allow this; do not bypass it programmatically.
5. Optional: Enable **Silence interruptions during sessions**, then grant **Do Not Disturb access**. Configure **allowed calls** in Android's **Modes / Do Not Disturb** settings yourself; calls and critical notifications depend on that system policy. Do not rely on this for emergency alerting.
6. Optional: Tap **Enable timely starts** and grant **Alarms & reminders**. Without this, Android can delay the automatic start/end alarm. Some devices suppress alarm delivery due to battery optimisation.
7. Open Study Scheduler. Enable the checkbox **Use Focus Mode** for the sessions you're creating, choose a future study start time, set a title/duration, and create/save the session.
8. When the time arrives, try opening one of the selected apps. If Android delivers a `TYPE_WINDOW_STATE_CHANGED` event and the service is permitted, UniTask overlays a “Focus Mode active” screen and offers **Return to study** or **End Focus early**.
9. The student may open UniTask > Home > **View Focus timer**, or Focus Mode Settings > **End current Focus Mode early**. Early exit asks for the account password and (when Firebase requests it) the authenticator-app code. **Forgot password** sends Firebase's normal reset email.
10. After the end time passes, the restriction stops and UniTask attempts to return Do Not Disturb to its previous state. Disable Focus Mode or revoke Accessibility access whenever you want.

### Limitations that should be disclosed in your project report

- Android does not grant third-party apps the same suspend-app powers as a device owner. This version **observes foreground package names only** and attempts to show an Accessibility overlay. It does not read other apps' text or collect screen contents.
- The device owner may disable the Accessibility service, uninstall UniTask, clear app data, reboot, or otherwise bypass the restriction. Password verification only controls the **in-app early exit**, not Android's system permissions.
- Android may delay alarms, stop services, or require extra settings on specific devices. Exact timing, interruption filtering, and overlay behavior cannot be guaranteed.
- On Android 15+, DND uses app-associated rules rather than unlimited control of global system state. Verify on the actual phone and use Android's native DND configuration to allow important calls.
- Focus selections are **local/device-specific**. A session's `focusModeEnabled` flag is in Firestore; installed-app selections and permission grants are not copied between phones.
- The Settings page uses an Accessibility service and a prominent disclosure. If you later publish on Google Play, review Google's Accessibility API policy and developer declaration requirements before release.
- Student authentication and the app's TOTP MFA require a working Firebase account. Forgot-password email delivery is external to UniTask.

## 5 — Firestore changes made by this version

Existing collections stay intact. The new fields are:

```
study_sessions/{sessionId}
  focusModeEnabled: true / false      // only for newly created sessions

connections/{connectionId}/read_receipts/{uid}
  uid: <current student uid>
  lastReadAt: <client-created timestamp in milliseconds>

groups/{groupId}/read_receipts/{uid}
  uid: <current student uid>
  lastReadAt: <client-created timestamp in milliseconds>

connections/{connectionId}/messages/{messageId}
groups/{groupId}/messages/{messageId}
  text, senderUid, createdAt, ...
  attachmentBase64, attachmentName, attachmentMime // optional
```

Read receipts and time ordering rely on client-generated timestamps in the existing code, so devices with incorrect clocks can produce imperfect unread counts. Improving this would require migration to trusted server timestamps.

## 6 — Troubleshooting

- **PERMISSION_DENIED on unread read_receipts:** Confirm the new Firestore Rules blocks have been published for both groups and connections.
- **PERMISSION_DENIED when attaching:** Check the attachment is under 250 KB after image compression, that the user is an accepted member, and that the new rules are live.
- **No unread badge:** Test while signed into **different accounts** and both have network; badge listeners run while UniTask is open. Try entering/leaving Chats to refresh receipts.
- **Focus does not block anything:** Verify Focus Mode toggle, selected packages, session checkbox, Accessibility service status, and that the session is currently active. Test on a physical phone. On Android 13+ a sideloaded app may have 'restricted settings'.
- **Focus starts late:** Grant Alarms & reminders if Android offers it and review battery settings. This cannot be fully guaranteed on ordinary phones.
- **No Do Not Disturb silencing:** Grant Notification Policy access, enable the toggle, and review native Android Modes settings. Don't overwrite your personal notification/call preferences.
- **Password verification asks for second factor:** Enter your authenticator code; the normal TOTP MFA remains enabled.
- **Gemini unavailable:** This update does not change Unify's Gemini provider or App Check requirements; follow your team's existing AI setup documentation.
- **Chat composer still moves:** Confirm you're running version 2.3, not an older install; rebuild/Run and inspect GroupFragment.java.

## 7 — Project hygiene / GitHub

Upload the source code and Gradle wrapper files, but NOT `.gradle/`, `.idea/`, `build/`, `local.properties`, API secrets, debug tokens, or keystores. Do not merge this prototype into `main` until the team has tested chat rules and Focus Mode permissions.

Official platform references:
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- https://developer.android.com/reference/android/app/NotificationManager
- https://support.google.com/googleplay/android-developer/answer/10964491
- https://firebase.google.com/docs/firestore/quotas
