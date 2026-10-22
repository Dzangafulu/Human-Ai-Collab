package com.example.human_ai_collaboration_platform;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/** Release builds must be registered in Firebase App Check with Play Integrity. */
final class AppCheckBootstrap {
    private AppCheckBootstrap() { }
    static void install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
    }
}
