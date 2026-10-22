package com.example.human_ai_collaboration_platform;

import android.app.Application;
import com.google.firebase.FirebaseApp;

/** Registers the correct App Check provider before Firebase AI Logic is used. */
public final class StudentApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        AppCheckBootstrap.install();
    }
}
