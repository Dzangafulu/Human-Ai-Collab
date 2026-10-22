package com.example.human_ai_collaboration_platform;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

/** DEBUG BUILDS ONLY: registration token is in Logcat; never ship this. */
final class AppCheckBootstrap {
    private AppCheckBootstrap() { }
    static void install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());
    }
}
