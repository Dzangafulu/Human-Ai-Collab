package com.hacms.app;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

/** Backwards-compatible Firestore study sessions, including old day/time documents. */
public final class StudySessionStore {
    private StudySessionStore() {}
    public interface Result { void done(String error); }
    public static long timeFrom(String day,String time) {
        if(day==null||time==null)return 0L;
        try {
            SimpleDateFormat df=new SimpleDateFormat(day.matches("\\d{4}-\\d{2}-\\d{2}") ? "yyyy-MM-dd HH:mm" : "d/M/yyyy HH:mm",Locale.US);df.setLenient(false);
            Date date=df.parse(day+" "+time);return date==null?0L:date.getTime();
        } catch(ParseException e){return 0L;}
    }
    public static long startMillis(DocumentSnapshot document) {
        Object stored=document.get("startAtMillis");
        if(stored instanceof Number)return ((Number)stored).longValue();
        return timeFrom(document.getString("day"),document.getString("time"));
    }
    private static Map<String,Object> fields(String uid,String taskId,String title,String day,String time,int duration){
        Map<String,Object> r=new HashMap<>();r.put("ownerUid",uid);
        r.put("taskId",taskId==null?"":taskId);
        r.put("taskTitle",title==null?"Study session":title.trim());
        r.put("day",day);r.put("time",time);r.put("startAtMillis",timeFrom(day,time));
        r.put("durationMinutes",duration);r.put("createdAt",System.currentTimeMillis());return r;
    }
    public static void createManual(Context context,String title,String day,String time,int duration,Result cb){
        String uid=FirebaseAuth.getInstance().getUid();if(uid==null){cb.done("Please log in");return;}
        long when=timeFrom(day,time);if(when<=System.currentTimeMillis()){cb.done("Select a future date and time");return;}
        FirebaseFirestore.getInstance().collection("study_sessions")
          .add(fields(uid,"",title,day,time,duration)).addOnSuccessListener(ref->{
               ReminderScheduler.scheduleStudy(context,uid,ref.getId(),title,when);cb.done(null);
          }).addOnFailureListener(e->cb.done(e.getMessage()));
    }
    public static void saveGenerated(Context context,List<StudyPlanEngine.Session> sessions,Result cb){
        String uid=FirebaseAuth.getInstance().getUid();if(uid==null){cb.done("Please log in");return;}
        FirebaseFirestore db=FirebaseFirestore.getInstance();WriteBatch batch=db.batch();
        int count=0;
        for(StudyPlanEngine.Session s:sessions){
            if(s.sortTime<=System.currentTimeMillis())continue;
            String id=uid+"_"+s.day.replace("-","")+"_"+s.time.replace(":", "");
            // Deterministic slot ID prevents duplicate sessions on repeated generation.
            batch.set(db.collection("study_sessions").document(id),fields(uid,s.taskId,s.title,s.day,s.time,60));count++;
        }
        if(count==0){cb.done("No future study sessions could be created");return;}
        batch.commit().addOnSuccessListener(v->{
            for(StudyPlanEngine.Session s:sessions){
                if(s.sortTime<=System.currentTimeMillis())continue;
                String id=uid+"_"+s.day.replace("-","")+"_"+s.time.replace(":", "");
                ReminderScheduler.scheduleStudy(context,uid,id,s.title,s.sortTime);
            }
            cb.done(null);
        }).addOnFailureListener(e->cb.done(e.getMessage()));
    }
}
