package com.hacms.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import com.google.firebase.firestore.DocumentSnapshot;

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
 private static PendingIntent alarmIntent(Context context,String uid,String id,String phase,
                                          int flags, String title,long at,boolean focus){
   String key="session_"+uid+"_"+id+"_"+phase;
   Intent action=new Intent(context,SessionAlarmReceiver.class)
       .setAction("com.hacms.app.SESSION_"+phase)
       .putExtra("uid",uid).putExtra("key",key).putExtra("phase",phase)
       .putExtra("scheduledAt",at)
       .putExtra("title",phase.equals("start")?"Study session started":"Study session finished")
       .putExtra("body",phase.equals("start")?title+" is underway. Tap to view the timer.":
          (focus?title+" has finished. Focus restrictions have ended.":title+" has finished. Great work!"));
   return PendingIntent.getBroadcast(context,key.hashCode(),action,flags|PendingIntent.FLAG_IMMUTABLE);
 }
 private static void scheduleBoundaryAlarm(Context context,String uid,String id,String title,
                                           String phase,boolean focus,long when){
   if(when<=System.currentTimeMillis())return;
   AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
   if(manager==null)return;
   PendingIntent pending=alarmIntent(context,uid,id,phase,PendingIntent.FLAG_UPDATE_CURRENT,
           title,when,focus);
   try{
     if(Build.VERSION.SDK_INT<31||manager.canScheduleExactAlarms()){
       manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);
     }else{
       manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);
     }
   }catch(SecurityException ignored){
     manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);
   }
 }
 private static void scheduleBoundary(Context context,String uid,String id,String title,long at,
                                      String phase,boolean focus,long end){
   long now=System.currentTimeMillis();
   // If the app opens seconds after the session began, still show the start notice.
   if(at < now - 60_000L)return;
   String key="session_"+uid+"_"+id+"_"+phase;
   SharedPreferences prefs=context.getApplicationContext().getSharedPreferences("unitask_reminder_queue",0);
   String memo="scheduled_"+key;
   if(prefs.getLong(memo,0)==at)return;
   Data input=new Data.Builder().putString("key",key).putString("uid",uid)
       .putString("sessionId",id).putString("phase",phase)
       .putBoolean("focus",focus).putLong("scheduledAt",at).putLong("sessionEnd",end)
       .putString("title",phase.equals("start")?"Study session started":"Study session finished")
       .putString("body",phase.equals("start")?title+" is underway. Tap to view the timer.":
             (focus?title+" has finished. Focus restrictions have ended.":title+" has finished. Great work!"))
       .build();
   OneTimeWorkRequest work=new OneTimeWorkRequest.Builder(ReminderWorker.class)
       .setInitialDelay(Math.max(0L,at-now),TimeUnit.MILLISECONDS).setInputData(input).addTag("unitask").build();
   WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(key,ExistingWorkPolicy.REPLACE,work);
   prefs.edit().putLong(memo,at).apply();
   scheduleBoundaryAlarm(context,uid,id,title,phase,focus,at);
 }
 public static void scheduleStudy(Context context,String uid,String id,String title,long start){
   scheduleStudy(context,uid,id,title,start,60,false);
 }
 public static void scheduleStudy(Context context,String uid,String id,String title,long start,long durationMinutes,boolean focus){
   if(uid==null||start<=0)return;
   long end=start+Math.max(1,durationMinutes)*60_000L;
   if(end<=System.currentTimeMillis())return;
   // Existing 10-minute warning, plus a start and an end event.
   if(start>System.currentTimeMillis()+60_000L){
     schedule(context,"session_"+uid+"_"+id,uid,start,10*60_000L,
       "Study session starting soon",title+" starts in about 10 minutes.");
   }
   scheduleBoundary(context,uid,id,title,start,"start",focus,end);
   scheduleBoundary(context,uid,id,title,end,"end",focus,end);
 }
 public static void syncStudy(Context context,String uid,List<DocumentSnapshot> docs){
   if(uid==null)return;
   SharedPreferences p=context.getApplicationContext().getSharedPreferences("unitask_reminder_queue",0);
   String k="studies_"+uid;
   Set<String> previous=new HashSet<>(p.getStringSet(k,new HashSet<>()));
   Set<String> current=new HashSet<>();
   for(DocumentSnapshot d:docs){
     current.add(d.getId()); // Only truly deleted records cancel pending notifications.
     long start=StudySessionStore.startMillis(d);
     Long duration=d.getLong("durationMinutes");
     long length=duration==null||duration<1?60:duration;
     if(start<=0 || start+length*60_000L<=System.currentTimeMillis())continue;
     String name=d.getString("taskTitle");
     if(name==null||name.trim().isEmpty())name="Study session";
     scheduleStudy(context,uid,d.getId(),name,start,length,Boolean.TRUE.equals(d.getBoolean("focusModeEnabled")));
   }
   for(String old:previous){if(!current.contains(old))cancelStudy(context,uid,old);}
   p.edit().putStringSet(k,current).apply();
 }
 /** Rearm exact/inexact alarm triggers after boot or permission changes; WorkManager is already durable. */
 public static void rearmCachedBoundaries(Context context){
   String uid=com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
   if(uid==null)return;
   String json=context.getSharedPreferences("unitask_study_timeline",0)
                   .getString("sessions_"+uid,"[]");
   try{
     org.json.JSONArray rows=new org.json.JSONArray(json);
     for(int i=0;i<rows.length();i++){
       org.json.JSONObject x=rows.getJSONObject(i);
       String id=x.optString("id"),title=x.optString("title","Study session");
       long from=x.optLong("from"),until=x.optLong("until");
       boolean focus=x.optBoolean("focus",false);
       if(from>System.currentTimeMillis())scheduleBoundaryAlarm(context,uid,id,title,"start",focus,from);
       if(until>System.currentTimeMillis())scheduleBoundaryAlarm(context,uid,id,title,"end",focus,until);
     }
   }catch(org.json.JSONException ignored){}
 }
 public static void cancelStudy(Context context,String uid,String id){
   cancel(context,"session_"+uid+"_"+id);
   cancel(context,"session_"+uid+"_"+id+"_start");
   cancel(context,"session_"+uid+"_"+id+"_end");
   AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
   if(manager!=null){
     for(String phase:new String[]{"start","end"}){
       PendingIntent pending=alarmIntent(context,uid,id,phase,PendingIntent.FLAG_NO_CREATE,"",0,false);
       if(pending!=null){manager.cancel(pending);pending.cancel();}
     }
   }
 }
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
