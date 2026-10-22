package com.example.human_ai_collaboration_platform;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Standalone deterministic prototype planner; no AI API and no training required. */
public class StudyPlanEngine {
    public static class Session {
        public final String taskId, title, day, time;
        public final long sortTime;
        public Session(String id, String title, String day, String time, long sortTime) {
            this.taskId=id; this.title=title; this.day=day; this.time=time; this.sortTime=sortTime;
        }
    }
    public static long dueMillis(Task t) {
        String date=t.getDueDate();
        if (date==null || date.equals("No due date")) return Long.MAX_VALUE;
        SimpleDateFormat fmt=new SimpleDateFormat("d/M/yyyy", Locale.US);
        fmt.setLenient(false);
        try { Date parsed=fmt.parse(date); return parsed==null?Long.MAX_VALUE:parsed.getTime(); }
        catch (ParseException e) { return Long.MAX_VALUE; }
    }
    public static List<Task> ranked(List<Task> all) {
        List<Task> open=new ArrayList<>();
        for(Task t:all) if (!"Completed".equalsIgnoreCase(t.getStatus())) open.add(t);
        Collections.sort(open,(a,b)-> {
            int d=Long.compare(dueMillis(a),dueMillis(b));
            if(d!=0)return d;
            return Integer.compare(priority(b),priority(a));
        });
        return open;
    }
    private static int priority(Task t) {
        if("High".equalsIgnoreCase(t.getPriority()))return 3;
        if("Medium".equalsIgnoreCase(t.getPriority()))return 2;
        return 1;
    }
    public static List<Session> generate(List<Task> tasks, int slotsPerDay, int startHour) {
        slotsPerDay=Math.max(1,Math.min(slotsPerDay,3));
        startHour=Math.max(6,Math.min(startHour,20));
        List<Session> sessions=new ArrayList<>();
        List<Task> ranked=ranked(tasks);
        Calendar today=Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY,0);today.set(Calendar.MINUTE,0);
        today.set(Calendar.SECOND,0);today.set(Calendar.MILLISECOND,0);
        SimpleDateFormat dayFmt=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        SimpleDateFormat dayMillisFmt=new SimpleDateFormat("EEEE, MMM d",Locale.US);
        int taskIndex=0;
        for(int day=0;day<7 && taskIndex<ranked.size();day++) {
            Calendar date=(Calendar)today.clone();date.add(Calendar.DAY_OF_YEAR,day);
            for(int slot=0;slot<slotsPerDay && taskIndex<ranked.size();slot++) {
                Task t=ranked.get(taskIndex++);
                String hm=String.format(Locale.US,"%02d:00",startHour+slot);
                sessions.add(new Session(t.getDocumentId()==null?"":t.getDocumentId(),t.getTitle(),
                    dayFmt.format(date.getTime()),hm,date.getTimeInMillis()+(startHour+slot)*3600000L));
            }
        }
        return sessions;
    }
}
