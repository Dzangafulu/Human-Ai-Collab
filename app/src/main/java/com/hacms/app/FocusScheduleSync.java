package com.hacms.app;
import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
/** Tracks future focus-enabled Firestore sessions while the signed-in app is active. Cached locally for offline monitoring. */
public final class FocusScheduleSync {
 private ListenerRegistration subscription;
 public void start(Context context){stop();String uid=FirebaseAuth.getInstance().getUid();if(uid==null)return;Context app=context.getApplicationContext();
  subscription=FirebaseFirestore.getInstance().collection("study_sessions").whereEqualTo("ownerUid",uid).addSnapshotListener((snap,error)->{
    if (error == null && snap != null) {
      StudySessionTimeline.sync(app, snap.getDocuments());
      ReminderScheduler.syncStudy(app, uid, snap.getDocuments());
      FocusModeManager.sync(app, snap.getDocuments());
    }
  });
 }
 public void stop(){if(subscription!=null){subscription.remove();subscription=null;}}
}
