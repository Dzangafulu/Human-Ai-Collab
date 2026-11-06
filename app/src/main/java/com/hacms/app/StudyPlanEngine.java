package com.hacms.app;

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
        String time=t.getDueTime();if(time==null || !time.matches("[0-9]{2}:[0-9]{2}"))time="23:59";
        SimpleDateFormat fmt=new SimpleDateFormat(date.matches("\\d{4}-\\d{2}-\\d{2}") ? "yyyy-MM-dd HH:mm" : "d/M/yyyy HH:mm", Locale.US);
        fmt.setLenient(false);
        try { Date parsed=fmt.parse(date+" "+time); return parsed==null?Long.MAX_VALUE:parsed.getTime(); }
        catch (ParseException e) { return Long.MAX_VALUE; }
    }
    public static List<Task> ranked(List<Task> all) {
        List<Task> open=new ArrayList<>();
        for(Task t:all) if (!"Completed".equalsIgnoreCase(t.getStatus()) && !"Done".equalsIgnoreCase(t.getStatus())) open.add(t);
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
        return generate(tasks,slotsPerDay,startHour,0);
    }
    /** Generate labelled sessions up to a week ahead, never after the corresponding task deadline. */
    public static List<Session> generate(List<Task> tasks,int slotsPerDay,int hour,int minute){
        slotsPerDay=Math.max(1,Math.min(3,slotsPerDay));
        hour=Math.max(0,Math.min(23,hour));minute=Math.max(0,Math.min(59,minute));
        List<Task> sorted=ranked(tasks);
        List<Session> output=new ArrayList<>();if(sorted.isEmpty())return output;
        Calendar current=Calendar.getInstance();Calendar day=(Calendar)current.clone();
        day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);
        day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);
        SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        int taskIndex=0;
        for(int offset=0;offset<7;offset++){
            Calendar candidate=(Calendar)day.clone();candidate.add(Calendar.DAY_OF_YEAR,offset);
            for(int slot=0;slot<slotsPerDay;slot++){
                Calendar start=(Calendar)candidate.clone();start.set(Calendar.HOUR_OF_DAY,hour);start.set(Calendar.MINUTE,minute);
                start.add(Calendar.MINUTE,slot*60);
                long begin=start.getTimeInMillis();if(begin<=current.getTimeInMillis()+60000L)continue;
                // Find the next task that isn't already due; repeated sessions support revision.
                Task selected=null;
                for(int n=0;n<sorted.size();n++){
                    Task t=sorted.get((taskIndex+n)%sorted.size());
                    long due=dueMillis(t);
                    if(due==Long.MAX_VALUE || due>begin+3600000L){selected=t;taskIndex=(taskIndex+n+1)%sorted.size();break;}
                }
                if(selected==null)continue;
                String taskTitle=selected.getTitle();
                output.add(new Session(selected.getDocumentId()==null?"":selected.getDocumentId(),
                  "Study for: "+taskTitle,dateFormat.format(start.getTime()),
                  String.format(Locale.US,"%02d:%02d",start.get(Calendar.HOUR_OF_DAY),start.get(Calendar.MINUTE)),begin));
            }
        }
        return output;
    }
}
