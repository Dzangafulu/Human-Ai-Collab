package com.hacms.app;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class FocusAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent){FocusModeManager.rescheduleCached(context);}
}
