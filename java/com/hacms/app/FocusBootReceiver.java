package com.hacms.app;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class FocusBootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())){FocusModeManager.rescheduleCached(c);ReminderScheduler.rearmCachedBoundaries(c);}}
}
