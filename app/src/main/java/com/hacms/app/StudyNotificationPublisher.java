package com.hacms.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;

/** One notification path for WorkManager and alarm fallback; avoids duplicate session alerts. */
public final class StudyNotificationPublisher {
    private StudyNotificationPublisher() { }
    public static synchronized boolean post(Context c,String uid,String key,String title,
                                             String body,String phase,long scheduledAt){
        if(uid==null||!uid.equals(FirebaseAuth.getInstance().getUid()))return false;
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(c,
                Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager==null)return false;
        if(phase!=null && scheduledAt>System.currentTimeMillis()+10_000L)return false;
        String unique=key+"_"+scheduledAt;
        android.content.SharedPreferences delivered=c.getSharedPreferences("unitask_delivered_notifications",0);
        if(phase!=null && delivered.getBoolean(unique,false))return true;
        String channel="unitask_reminders";
        if(Build.VERSION.SDK_INT>=26){
            manager.createNotificationChannel(new NotificationChannel(channel,"UniTask reminders",
                    NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open=new Intent(c,MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if("start".equals(phase))open.putExtra(MainActivity.EXTRA_OPEN_FOCUS_TIMER,true);
        PendingIntent click=PendingIntent.getActivity(c,unique.hashCode(),open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notice=new NotificationCompat.Builder(c,channel)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title==null?"UniTask reminder":title)
                .setContentText(body==null?"You have an upcoming study activity":body)
                .setContentIntent(click).setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        manager.notify(key==null?123:key.hashCode(),notice.build());
        if(phase!=null)delivered.edit().putBoolean(unique,true).apply();
        return true;
    }
}
