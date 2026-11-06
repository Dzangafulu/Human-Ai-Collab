package com.hacms.app;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
/** Debug builds only: each emulator requires its own registered debug token. */
public final class AppCheckBootstrap {
 private AppCheckBootstrap(){}
 public static void install(){FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());}
}
