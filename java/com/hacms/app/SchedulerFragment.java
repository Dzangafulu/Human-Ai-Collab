package com.hacms.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** A student's self-directed sessions + AI-ready task-based study planning. */
public class SchedulerFragment extends Fragment {
 private final FirebaseTaskRepository tasks=new FirebaseTaskRepository();
 private ListenerRegistration subscription;
 private final List<DocumentSnapshot> savedRecords=new ArrayList<>();
 private final android.os.Handler expirationHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable expirationRefresh=new Runnable(){@Override public void run(){
  if(isAdded()){show(savedRecords);expirationHandler.postDelayed(this,60000L);}
 }};
 private LinearLayout list;
 private TextView info;
 private Button manualDate,manualTime,autoTime;
 private EditText subject;
 private Spinner duration,slots;
 private String chosenDate;
 private int chosenHour=18,chosenMinute=0,autoHour=18,autoMinute=0;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup p,@Nullable Bundle b){return i.inflate(R.layout.fragment_scheduler,p,false);}
 @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
  list=view.findViewById(R.id.schedulerList);info=view.findViewById(R.id.schedulerInfo);
  subject=view.findViewById(R.id.schedulerSubject);manualDate=view.findViewById(R.id.schedulerPickDate);
  manualTime=view.findViewById(R.id.schedulerPickTime);autoTime=view.findViewById(R.id.schedulerAutoTime);
  duration=view.findViewById(R.id.schedulerDuration);slots=view.findViewById(R.id.schedulerSessions);
  duration.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,new String[]{"30 minutes","60 minutes","90 minutes","120 minutes"}));
  duration.setSelection(1);
  slots.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,new String[]{"1 session","2 sessions","3 sessions"}));
  Calendar now=Calendar.getInstance();now.add(Calendar.DAY_OF_YEAR,1);
  chosenDate=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(now.getTime());
  manualDate.setText("Date: "+chosenDate);manualTime.setText("Start time: 18:00");autoTime.setText("Start sessions at 18:00");
  manualDate.setOnClickListener(v->{Calendar c=Calendar.getInstance();DatePickerDialog picker=new DatePickerDialog(requireContext(),(dp,y,m,d)->{
    chosenDate=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);manualDate.setText("Date: "+chosenDate);
  },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
  picker.getDatePicker().setMinDate(System.currentTimeMillis()-86400000L);picker.show();});
  manualTime.setOnClickListener(v->new TimePickerDialog(requireContext(),(picker,h,m)->{
    chosenHour=h;chosenMinute=m;manualTime.setText(String.format(Locale.US,"Start time: %02d:%02d",h,m));
  },chosenHour,chosenMinute,true).show());
  autoTime.setOnClickListener(v->new TimePickerDialog(requireContext(),(picker,h,m)->{
    autoHour=h;autoMinute=m;autoTime.setText(String.format(Locale.US,"Start sessions at %02d:%02d",h,m));
  },autoHour,autoMinute,true).show());
  view.findViewById(R.id.schedulerBack).setOnClickListener(v->NavHostFragment.findNavController(this).navigateUp());
  view.findViewById(R.id.schedulerManualAdd).setOnClickListener(v->addManual());
  view.findViewById(R.id.schedulerGenerate).setOnClickListener(v->generate());
 }
 @Override public void onStart(){super.onStart();String uid=FirebaseAuth.getInstance().getUid();if(uid==null)return;
  subscription=FirebaseFirestore.getInstance().collection("study_sessions").whereEqualTo("ownerUid",uid)
   .addSnapshotListener((snap,e)->{
      if(!isAdded())return;
      if(e!=null){info.setText("Could not load sessions: "+e.getMessage());return;}
      savedRecords.clear();if(snap!=null)savedRecords.addAll(snap.getDocuments());
      savedRecords.sort((a,b)->Long.compare(StudySessionStore.startMillis(a),StudySessionStore.startMillis(b)));
      show(savedRecords);
   });
   expirationHandler.postDelayed(expirationRefresh,60000L);
 }
 @Override public void onStop(){super.onStop();if(subscription!=null){subscription.remove();subscription=null;}expirationHandler.removeCallbacks(expirationRefresh);}
 private void addManual(){
   String title=subject.getText().toString().trim();if(title.isEmpty()){subject.setError("Enter a subject or session name");return;}
   String time=String.format(Locale.US,"%02d:%02d",chosenHour,chosenMinute);
   int[] lengths={30,60,90,120};int minutes=lengths[duration.getSelectedItemPosition()];
   info.setText("Saving your session...");
   StudySessionStore.createManual(requireContext(),title,chosenDate,time,minutes,error->{
       if(!isAdded())return;info.setText(error==null?"Session saved to Firebase; a reminder is scheduled.":"Could not save: "+error);
       if(error==null)subject.setText("");
   });
 }
 private void generate(){
   info.setText("Finding unfinished tasks...");
   tasks.loadOnce(new FirebaseTaskRepository.OnTasks(){
    public void onResult(List<Task> records){
      if(!isAdded())return;
      tasks.loadAssigned(new FirebaseTaskRepository.OnTasks(){
        public void onResult(List<Task> assigned){
          if(!isAdded())return;
          Map<String,Task> unique=new LinkedHashMap<>();
          for(Task t:records)unique.put(t.getDocumentId(),t);
          for(Task t:assigned)unique.put(t.getDocumentId(),t);
          List<StudyPlanEngine.Session> plan=StudyPlanEngine.generate(new ArrayList<>(unique.values()),slots.getSelectedItemPosition()+1,autoHour,autoMinute);
          if(plan.isEmpty()){info.setText("No available future sessions. Add tasks or choose a different time.");return;}
          info.setText("Saving "+plan.size()+" study sessions...");
          StudySessionStore.saveGenerated(requireContext(),plan,error->{if(isAdded())info.setText(error==null?plan.size()+" sessions saved automatically to Firebase.":"Save failed: "+error);});
        }
        public void onError(String e){if(isAdded())info.setText("Could not load assigned tasks: "+e);}
      });
    }
    public void onError(String e){if(isAdded())info.setText("Could not load tasks: "+e);}
   });
 }
 private void show(List<DocumentSnapshot> docs){
   list.removeAllViews();int count=0;long now=System.currentTimeMillis();
   for(DocumentSnapshot doc:docs){
     long when=StudySessionStore.startMillis(doc);if(when<=now)continue;count++;
     LinearLayout card=Ui.panel(requireContext());
     String name=doc.getString("taskTitle");if(name==null)name="Study session";
     card.addView(Ui.label(requireContext(),name,16,true));
     String day=doc.getString("day"),time=doc.getString("time");
     Long dur=doc.getLong("durationMinutes");
     card.addView(Ui.muted(requireContext(),day+"  ·  "+time+"  ·  "+(dur==null?60:dur)+" minutes",13),Ui.lp(requireContext(),6));
     TextView remove=Ui.pill(requireContext(),"Remove session",0xfffce8e8,0xffb23b3b);
     remove.setOnClickListener(v->FirebaseFirestore.getInstance().collection("study_sessions").document(doc.getId()).delete()
        .addOnSuccessListener(done->{ReminderScheduler.cancelStudy(requireContext(),FirebaseAuth.getInstance().getUid(),doc.getId());}));
     card.addView(remove,Ui.lp(requireContext(),8));list.addView(card,Ui.lp(requireContext(),10));
     ReminderScheduler.scheduleStudy(requireContext(),FirebaseAuth.getInstance().getUid(),doc.getId(),name,when);
   }
   if(count==0)list.addView(Ui.muted(requireContext(),"No future sessions yet. Create one above.",14));
 }
}
