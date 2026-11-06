package com.hacms.app;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DashboardFragment extends Fragment {
 private LinearLayout upcoming,active,countRow,all,sessionsList;
 private TextView sessionBanner;
 private ListenerRegistration savedSessions;
 private final android.os.Handler refreshHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private List<DocumentSnapshot> cachedSessions=new ArrayList<>();
 private final Runnable expiryRefresh=new Runnable(){@Override public void run(){
  if(isAdded()) {
   renderActiveSessionBanner();
   renderSavedSessions(cachedSessions);
   refreshHandler.postDelayed(this,5000L);
  }
 }};
 private void renderActiveSessionBanner(){
   if(!isAdded()||sessionBanner==null)return;
   StudySessionTimeline.ActiveSession session=StudySessionTimeline.active(requireContext());
   if(session==null){
     // Firestore can deliver a fresh snapshot before its local schedule cache
     // is updated; consult the visible rows too.
     long now=System.currentTimeMillis();
     for(DocumentSnapshot d:cachedSessions){
       long start=StudySessionStore.startMillis(d);
       Long duration=d.getLong("durationMinutes");
       long end=start+(duration==null||duration<1?60:duration)*60_000L;
       if(start>0&&start<=now&&now<end){
         String title=d.getString("taskTitle");
         sessionBanner.setText("Study session started: "+(title==null?"Study session":title)+"  → View timer");
         sessionBanner.setVisibility(View.VISIBLE);
         return;
       }
     }
     sessionBanner.setVisibility(View.GONE);
   } else {
     sessionBanner.setText("Study session started: "+session.title+"  → View timer");
     sessionBanner.setVisibility(View.VISIBLE);
   }
 }
 private ListenerRegistration mine,assigned;
 private final Map<String,Task> displayed=new LinkedHashMap<>();
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater inf,@Nullable ViewGroup parent,@Nullable Bundle b){Context c=requireContext();all=Ui.vertical(c);
 LinearLayout header=Ui.header(this,"Good morning","UniTask · Study smarter, achieve more");all.addView(header);
 sessionBanner=Ui.pill(c,"Study session started  → View timer",0xffdff3ea,0xff17324f);
 sessionBanner.setVisibility(View.GONE);
 sessionBanner.setOnClickListener(v->Ui.open(this,R.id.focusStatusFragment));
 all.addView(sessionBanner,Ui.lp(c,14));
 LinearLayout headerRow=(LinearLayout)header.getChildAt(0);
 LinearLayout titleColumn=(LinearLayout)headerRow.getChildAt(0);
 TextView greeting=(TextView)titleColumn.getChildAt(0);
 int hour=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);greeting.setText(hour<12?"Good morning":hour<17?"Good afternoon":"Good evening");
 String uid=FirebaseAuth.getInstance().getUid();if(uid!=null)FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(d->{if(!isAdded())return;String n=d.getString("name");if(n!=null&&!n.trim().isEmpty())greeting.append(", "+n.trim().split("\\s+")[0]);});
 LinearLayout important=Ui.vertical(c);important.setPadding(Ui.dp(c,18),Ui.dp(c,16),Ui.dp(c,18),Ui.dp(c,16));important.setBackground(Ui.shape(Ui.NAVY,Ui.dp(c,18),0));
 LinearLayout title=Ui.horizontal(c);TextView label=Ui.label(c,"Upcoming tasks",17,true);label.setTextColor(-1);title.addView(label,new LinearLayout.LayoutParams(0,-2,1));TextView icon=Ui.pill(c,"▣",Ui.NAVY,-1);icon.setOnClickListener(v->Ui.open(this,R.id.schedulerFragment));title.addView(icon);important.addView(title);
 upcoming=Ui.vertical(c);important.addView(upcoming);all.addView(important,Ui.lp(c,18));
 all.addView(Ui.label(c,"Active tasks",22,false),Ui.lp(c,24));active=Ui.vertical(c);all.addView(active,Ui.lp(c,10));
 TextView overview=Ui.label(c,"Task overview",19,false);overview.setTextColor(Ui.ORANGE);all.addView(overview,Ui.lp(c,18));countRow=Ui.horizontal(c);all.addView(countRow,Ui.lp(c,14));
 TextView plan=Ui.pill(c,"Generate today's study plan  →",Ui.BLUE,-1);plan.setOnClickListener(v->Ui.open(this,R.id.schedulerFragment));all.addView(plan,Ui.lp(c,22));
 all.addView(Ui.label(c,"My upcoming study sessions",19,true),Ui.lp(c,22));
 sessionsList=Ui.vertical(c);all.addView(sessionsList,Ui.lp(c,10));
 return Ui.scroll(c,all);}
 @Override public void onStart(){super.onStart();String uid=FirebaseAuth.getInstance().getUid();if(uid==null)return;FirebaseFirestore db=FirebaseFirestore.getInstance();
 mine=db.collection("tasks").whereEqualTo("ownerUid",uid).addSnapshotListener((s,e)->{if(e!=null){Ui.toast(requireContext(),"Could not load tasks: "+e.getMessage());return;}synchronize("owned",s);});
 assigned=db.collection("tasks").whereEqualTo("assigneeUid",uid).addSnapshotListener((s,e)->{if(e==null)synchronize("assigned",s);});observeSessions();refreshHandler.removeCallbacks(expiryRefresh);
 refreshHandler.post(expiryRefresh);}
 private final Map<String,Map<String,Task>> sources=new HashMap<>();
 private void observeSessions(){
  String uid=FirebaseAuth.getInstance().getUid(); if(uid==null)return;
  savedSessions=FirebaseFirestore.getInstance().collection("study_sessions")
    .whereEqualTo("ownerUid",uid).addSnapshotListener((snap,error)->{
     if(!isAdded()||sessionsList==null)return; sessionsList.removeAllViews();
     if(error!=null){sessionsList.addView(Ui.muted(requireContext(),"Could not load sessions: "+error.getMessage(),13));return;}
     cachedSessions.clear(); if(snap!=null)cachedSessions.addAll(snap.getDocuments());
     StudySessionTimeline.sync(requireContext(), cachedSessions);
     renderActiveSessionBanner();
     renderSavedSessions(cachedSessions);
    });
 }
 private void renderSavedSessions(List<DocumentSnapshot> rows){
     if(!isAdded()||sessionsList==null)return;
     sessionsList.removeAllViews();
     List<DocumentSnapshot> sorted=new ArrayList<>(rows);
     sorted.sort((a,b)->Long.compare(StudySessionStore.startMillis(a),StudySessionStore.startMillis(b)));
     String uid=FirebaseAuth.getInstance().getUid();
     int shown=0;
     long now=System.currentTimeMillis();
     for(DocumentSnapshot d:sorted){
      long when=StudySessionStore.startMillis(d);
      Long duration=d.getLong("durationMinutes");
      long length=duration==null||duration<1?60:duration;
      long until=when+length*60_000L;
      if(until<=now || when<=0)continue;
      boolean ongoing=when<=now;
      String title=d.getString("taskTitle");if(title==null||title.trim().isEmpty())title="Study session";
      LinearLayout card=Ui.panel(requireContext());
      card.addView(Ui.label(requireContext(),(ongoing?"● In progress: ":"")+title,15,true));
      String day=d.getString("day"),time=d.getString("time");
      card.addView(Ui.muted(requireContext(),ongoing?"Tap to view the remaining time":
              (day==null?"":day)+"  •  "+(time==null?"":time),12),Ui.lp(requireContext(),5));
      if(ongoing)card.setOnClickListener(v->Ui.open(this,R.id.focusStatusFragment));
      sessionsList.addView(card,Ui.lp(requireContext(),7));shown++;
      ReminderScheduler.scheduleStudy(requireContext(),uid,d.getId(),title,when,length,
              Boolean.TRUE.equals(d.getBoolean("focusModeEnabled")));
      if(shown>=6)break;
     }
     if(shown==0)sessionsList.addView(Ui.muted(requireContext(),"No upcoming study sessions. Create one in the Study Scheduler.",13));
 }
 private void synchronize(String key,QuerySnapshot s){if(s==null||!isAdded())return;Map<String,Task> list=new LinkedHashMap<>();for(DocumentSnapshot d:s.getDocuments())list.put(d.getId(),FirebaseTaskRepository.parse(d));sources.put(key,list);displayed.clear();for(Map<String,Task> each:sources.values())displayed.putAll(each);render();}
 private void render(){if(!isAdded()||upcoming==null)return;Context c=requireContext();upcoming.removeAllViews();active.removeAllViews();countRow.removeAllViews();List<Task> list=new ArrayList<>(displayed.values());Collections.sort(list,(a,b)->(a.getDueDate()+" "+a.getDueTime()).compareTo(b.getDueDate()+" "+b.getDueTime()));int todo=0,progress=0,done=0,up=0,act=0;
 for(Task t:list){ReminderScheduler.scheduleTask(c,FirebaseAuth.getInstance().getUid(),t);String status=t.getStatus().toLowerCase(Locale.ROOT);if(status.contains("complet")||status.contains("done"))done++;else if(status.contains("progress"))progress++;else todo++;
 if(up<3&&!status.contains("complet")){TextView line=Ui.label(c,"• "+t.getTitle()+" · "+t.getDueDate()+" "+t.getDueTime(),13,false);line.setTextColor(0xffeef5ff);upcoming.addView(line,Ui.lp(c,5));up++;}
 if(act<3&&status.contains("progress")){LinearLayout card=Ui.panel(c);card.addView(Ui.label(c,t.getTitle(),15,true));card.addView(Ui.muted(c,t.getDueDate()+" · "+t.getPriority()+" priority",12),Ui.lp(c,6));ProgressBar bar=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(t.getProgressPercent());card.addView(bar,Ui.lp(c,12));active.addView(card,Ui.lp(c,10));act++;}}
 if(up==0){TextView empty=Ui.label(c,"No upcoming tasks — add a task to get started.",13,false);empty.setTextColor(-1);upcoming.addView(empty,Ui.lp(c,8));}if(act==0)active.addView(Ui.muted(c,"Your active tasks will appear here.",13));
 String[] labs={"To do","In progress","Done"};int[] vals={todo,progress,done};int[] fill={0xffe7f0ff,0xfffff1df,0xffe2f6ef};for(int i=0;i<3;i++){LinearLayout col=Ui.vertical(c);col.setGravity(Gravity.CENTER);col.setPadding(4,14,4,14);col.setBackground(Ui.shape(fill[i],Ui.dp(c,12),0));TextView value=Ui.label(c,""+vals[i],20,false);value.setTextColor(Ui.NAVY);col.addView(value);TextView label=Ui.muted(c,labs[i],11);label.setTextColor(Ui.NAVY);col.addView(label);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(c,67),1);lp.setMargins(Ui.dp(c,3),0,Ui.dp(c,3),0);countRow.addView(col,lp);}}
 @Override public void onResume(){super.onResume();renderActiveSessionBanner();renderSavedSessions(cachedSessions);}
 @Override public void onStop(){super.onStop();if(mine!=null)mine.remove();if(assigned!=null)assigned.remove();if(savedSessions!=null)savedSessions.remove();refreshHandler.removeCallbacks(expiryRefresh);}
}
