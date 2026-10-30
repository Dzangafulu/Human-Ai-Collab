package com.hacms.app;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
/** Release builds: Play Integrity instead of the debug provider. */
public final class AppCheckBootstrap {
 private AppCheckBootstrap(){}
 public static void install(){FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());}
}
