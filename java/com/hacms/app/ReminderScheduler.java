package com.hacms.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

/** Durable, approximate reminders using Android WorkManager. */
public final class ReminderScheduler {
 private ReminderScheduler(){}
 private static void schedule(Context context,String key,String uid,long start,long lead,String heading,String description){
   if(uid==null||start<=System.currentTimeMillis())return;
   Context c=context.getApplicationContext();SharedPreferences p=c.getSharedPreferences("unitask_reminder_queue",0);
   String memo="scheduled_"+key;
   if(p.getLong(memo,0L)==start)return;
   long trigger=Math.max(System.currentTimeMillis()+5000L,start-lead);
   Data input=new Data.Builder().putString("key",key).putString("uid",uid).putString("title",heading).putString("body",description).build();
   OneTimeWorkRequest work=new OneTimeWorkRequest.Builder(ReminderWorker.class)
       .setInitialDelay(Math.max(1000L,trigger-System.currentTimeMillis()),TimeUnit.MILLISECONDS)
       .setInputData(input).addTag("unitask").build();
   WorkManager.getInstance(c).enqueueUniqueWork(key,ExistingWorkPolicy.REPLACE,work);
   p.edit().putLong(memo,start).apply();
 }
 public static void scheduleStudy(Context context,String uid,String id,String title,long start){
   schedule(context,"session_"+uid+"_"+id,uid,start,10*60_000L,
       "Study session starting soon",title+" starts in about 10 minutes.");
 }
 public static void cancelStudy(Context context,String uid,String id){cancel(context,"session_"+uid+"_"+id);}
 public static void scheduleTask(Context context,String uid,Task task){
   if(uid==null||task.getDocumentId()==null)return;
   String key="task_"+uid+"_"+task.getDocumentId();
   if("completed".equalsIgnoreCase(task.getStatus())||"done".equalsIgnoreCase(task.getStatus())){cancel(context,key);return;}
   String d=task.getDueDate();if(d==null)return;
   long start=StudySessionStore.timeFrom(d,task.getDueTime());
   if(start==0L)return;
   schedule(context,key,uid,start,24*60*60_000L,"Task deadline approaching",task.getTitle()+" is due "+d+" "+task.getDueTime());
 }
 public static void cancelTask(Context context,String uid,String id){cancel(context,"task_"+uid+"_"+id);}
 private static void cancel(Context context,String key){
   WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(key);
   context.getSharedPreferences("unitask_reminder_queue",0).edit().remove("scheduled_"+key).apply();
 }
}
