package com.hacms.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Scheduled locally; delivers even when the UniTask UI is not running, unless Android force-stopped the app. */
public final class SessionAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent){
        if(intent==null)return;
        StudyNotificationPublisher.post(context,intent.getStringExtra("uid"),intent.getStringExtra("key"),
                intent.getStringExtra("title"),intent.getStringExtra("body"),
                intent.getStringExtra("phase"),intent.getLongExtra("scheduledAt",0));
        FocusModeManager.refreshDnd(context);
    }
}
