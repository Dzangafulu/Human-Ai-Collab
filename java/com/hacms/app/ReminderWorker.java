package com.hacms.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;

/** Local notification delivery: no Cloud Functions or paid push messaging required. */
public class ReminderWorker extends Worker {
 public ReminderWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
 @NonNull @Override public Result doWork(){
   Context c=getApplicationContext();String account=getInputData().getString("uid");
   if(account==null||!account.equals(FirebaseAuth.getInstance().getUid()))return Result.success();
   if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return Result.success();
   NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
   if(manager==null)return Result.success();
   String channel="unitask_reminders";
   if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(channel,"UniTask reminders",NotificationManager.IMPORTANCE_DEFAULT));
   String title=getInputData().getString("title"),body=getInputData().getString("body");
   NotificationCompat.Builder note=new NotificationCompat.Builder(c,channel)
       .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title==null?"UniTask reminder":title)
       .setContentText(body==null?"You have an upcoming study activity":body)
       .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT);
   manager.notify(getInputData().getString("key")!=null?getInputData().getString("key").hashCode():123, note.build());
   return Result.success();
 }
}
