package com.hacms.app;

import android.app.Application;
import com.google.firebase.FirebaseApp;

/** Registers the correct App Check provider before Firebase AI Logic is used. */
public final class StudentApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        boolean dark = getSharedPreferences("unitask", MODE_PRIVATE).getBoolean("dark", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(dark
            ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        FirebaseApp.initializeApp(this);
        AppCheckBootstrap.install();
    }
}
