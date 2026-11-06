package com.hacms.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/** Device-local opt-in focus configuration. NEVER collects accessibility event contents. */
public final class FocusModeManager {
    private FocusModeManager() { }
    private static final String PREFS = "unitask_focus";
    static SharedPreferences prefs(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static boolean enabled(Context c) { return prefs(c).getBoolean("enabled", false); }
    public static boolean mute(Context c) { return prefs(c).getBoolean("mute", false); }
    public static Set<String> blocked(Context c) { return new HashSet<>(prefs(c).getStringSet("blocked", new HashSet<>())); }
    public static void setEnabled(Context c, boolean b) { prefs(c).edit().putBoolean("enabled", b).apply(); if(!b) restoreDnd(c); }
    public static void setMute(Context c, boolean b) { prefs(c).edit().putBoolean("mute", b).apply(); if(!b) restoreDnd(c); }
    public static void setBlocked(Context c, Set<String> apps) { prefs(c).edit().putStringSet("blocked",new HashSet<>(apps)).apply(); }
    public static boolean consent(Context c) { return prefs(c).getBoolean("consent", false); }
    public static void setConsent(Context c) { prefs(c).edit().putBoolean("consent", true).apply(); }

    public static final class Session {
        public final String id, title; public final long from, until;
        Session(String id, String title, long from,long until){this.id=id;this.title=title;this.from=from;this.until=until;}
    }
    private static String accountKey() { String uid=FirebaseAuth.getInstance().getUid();return uid==null?"signed_out":uid; }
    private static String scheduleKey(){return "schedule_"+accountKey();}
    private static String skippedKey(){return "skipped_"+accountKey();}
    public static Session active(Context c) {
        if(!enabled(c)) return null;
        String json = prefs(c).getString(scheduleKey(), "[]");
        long now=System.currentTimeMillis();
        try { JSONArray array=new JSONArray(json);Set<String> skipped=prefs(c).getStringSet(skippedKey(),new HashSet<>());
            for(int i=0;i<array.length();i++) {JSONObject x=array.getJSONObject(i);
                String id=x.optString("id");long from=x.optLong("from"),until=x.optLong("until");
                if(from<=now&&now<until&&!skipped.contains(id))return new Session(id,x.optString("title","Study"),from,until);
            }
        } catch(Exception ignored) { }
        return null;
    }
    public static void skip(Context c,String sessionId) {
        Set<String> skipped=new HashSet<>(prefs(c).getStringSet(skippedKey(),new HashSet<>()));
        skipped.add(sessionId);prefs(c).edit().putStringSet(skippedKey(),skipped).apply();restoreDnd(c);
    }
    public static void sync(Context c, java.util.List<DocumentSnapshot> docs) {
        String key=scheduleKey();JSONArray result=new JSONArray();long now=System.currentTimeMillis();
        for(DocumentSnapshot d:docs){if(!Boolean.TRUE.equals(d.getBoolean("focusModeEnabled")))continue;
            long start=StudySessionStore.startMillis(d);Long len=d.getLong("durationMinutes");long end=start+(len==null?60:len)*60000L;
            if(end<=now || start<=0)continue;
            try{JSONObject x=new JSONObject();x.put("id",d.getId());x.put("title",d.getString("taskTitle"));x.put("from",start);x.put("until",end);result.put(x);}catch(Exception ignored){}
            scheduleAlarm(c,start,d.getId()+"_start");scheduleAlarm(c,end,d.getId()+"_end");
        }
        prefs(c).edit().putString(key,result.toString()).apply();refreshDnd(c);
    }
    private static void scheduleAlarm(Context c,long when,String id){
        if(when<=System.currentTimeMillis())return;
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(manager==null)return;
        Intent intent=new Intent(c,FocusAlarmReceiver.class).setAction("com.hacms.app.FOCUS_TIMER").putExtra("id",id);
        PendingIntent pending=PendingIntent.getBroadcast(c,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=31&&manager.canScheduleExactAlarms()){
            try {manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);return;}catch(SecurityException ignored){}
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);
    }
    public static void rescheduleCached(Context c){
        try{JSONArray list=new JSONArray(prefs(c).getString(scheduleKey(),"[]"));
            for(int i=0;i<list.length();i++){JSONObject x=list.getJSONObject(i);scheduleAlarm(c,x.getLong("from"),x.getString("id")+"_start");scheduleAlarm(c,x.getLong("until"),x.getString("id")+"_end");}
        }catch(Exception ignored){}
        refreshDnd(c);
    }
    public static void refreshDnd(Context c) {
        if(!enabled(c)||!mute(c)||active(c)==null){restoreDnd(c);return;}
        NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(n==null||!n.isNotificationPolicyAccessGranted())return;
        try {if(!prefs(c).getBoolean("changed_dnd",false)) {
                int old=n.getCurrentInterruptionFilter();if(old!=NotificationManager.INTERRUPTION_FILTER_ALL)return; // Never override a user's existing DND mode.
                prefs(c).edit().putBoolean("changed_dnd",true).apply();
                n.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            }
        } catch(SecurityException ignored) {}
    }
    public static void restoreDnd(Context c){
        if(!prefs(c).getBoolean("changed_dnd",false))return;
        NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(n!=null && n.isNotificationPolicyAccessGranted()){
            try{n.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);}catch(SecurityException ignored){}
        }
        prefs(c).edit().putBoolean("changed_dnd",false).apply();
    }
}
