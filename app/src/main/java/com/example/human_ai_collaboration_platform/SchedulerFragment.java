package com.example.human_ai_collaboration_platform;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** New study planner. Preserves teammates' existing screens and task form. */
public class SchedulerFragment extends Fragment {
    private final FirebaseTaskRepository tasks = new FirebaseTaskRepository();
    private final List<StudyPlanEngine.Session> preview = new ArrayList<>();
    private LinearLayout list;
    private TextView info;
    private Button save;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        return inflater.inflate(R.layout.fragment_scheduler,container,false);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        super.onViewCreated(view,state);
        list=view.findViewById(R.id.schedulerList); info=view.findViewById(R.id.schedulerInfo);
        save=view.findViewById(R.id.schedulerSave);
        Spinner count=view.findViewById(R.id.schedulerSessions), hours=view.findViewById(R.id.schedulerHours);
        count.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,new String[]{"1 session","2 sessions","3 sessions"}));
        hours.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,new String[]{"16:00","17:00","18:00","19:00"}));
        hours.setSelection(2);
        view.findViewById(R.id.schedulerBack).setOnClickListener(v->NavHostFragment.findNavController(this).navigateUp());
        view.findViewById(R.id.schedulerGenerate).setOnClickListener(v->{
            info.setText("Loading tasks...");
            tasks.loadOnce(new FirebaseTaskRepository.OnTasks(){
                public void onResult(List<Task> records){
                    if(!isAdded())return;
                    preview.clear();preview.addAll(StudyPlanEngine.generate(records,count.getSelectedItemPosition()+1,16+hours.getSelectedItemPosition()));
                    save.setEnabled(!preview.isEmpty());
                    info.setText(preview.isEmpty()?"No unfinished tasks found. Add tasks in the Task Board first.":"Preview: "+preview.size()+" sessions. Tap Save to store the plan.");
                    show(preview);
                }
                public void onError(String error){if(isAdded()) info.setText("Failed to load tasks: "+error);}
            });
        });
        save.setOnClickListener(v->savePlan());
        loadSaved();
    }
    private void loadSaved(){
        String uid=FirebaseAuth.getInstance().getUid();if(uid==null)return;
        FirebaseFirestore.getInstance().collection("study_sessions").whereEqualTo("ownerUid",uid).get()
          .addOnSuccessListener(snap->{if(!isAdded() || !preview.isEmpty())return;
             List<StudyPlanEngine.Session> records=new ArrayList<>();
             for(DocumentSnapshot d:snap.getDocuments()){
                String day=d.getString("day"),time=d.getString("time"),title=d.getString("taskTitle");
                if(day!=null && time!=null && title!=null)records.add(new StudyPlanEngine.Session(d.getString("taskId"),title,day,time,0));
             }
             records.sort((a,b)->(a.day+a.time).compareTo(b.day+b.time));
             if(!records.isEmpty()){info.setText("Saved sessions (tap Generate to prepare a new plan)");show(records);}
          });
    }
    private void show(List<StudyPlanEngine.Session> sessions){
        list.removeAllViews();
        for(StudyPlanEngine.Session s:sessions){
            LinearLayout card=new LinearLayout(requireContext());card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(14),dp(16),dp(14));
            card.setBackgroundResource(R.drawable.dashboard_white_card);
            LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.bottomMargin=dp(10);card.setLayoutParams(layout);
            TextView task=new TextView(requireContext());task.setText(s.title);task.setTextSize(16);task.setTypeface(null,Typeface.BOLD);task.setTextColor(Color.rgb(23,50,79));card.addView(task);
            TextView meta=new TextView(requireContext());meta.setText(s.day+"  •  "+s.time+"  •  60 min");meta.setTextColor(Color.rgb(99,116,140));meta.setPadding(0,dp(5),0,0);card.addView(meta);list.addView(card);
        }
    }
    private void savePlan(){
        String uid=FirebaseAuth.getInstance().getUid();if(uid==null||preview.isEmpty())return;
        save.setEnabled(false);info.setText("Saving sessions...");
        FirebaseFirestore db=FirebaseFirestore.getInstance(); WriteBatch batch=db.batch();
        for(StudyPlanEngine.Session s:preview){
            Map<String,Object> record=new HashMap<>();
            record.put("ownerUid",uid);record.put("taskId",s.taskId);
            record.put("taskTitle",s.title);record.put("day",s.day);record.put("time",s.time);
            record.put("durationMinutes",60);record.put("createdAt",System.currentTimeMillis());
            // One document per user/date/start time. Saving again updates matching slots.
            String docId=uid+"_"+s.day.replace("-","")+"_"+s.time.replace(":","");
            batch.set(db.collection("study_sessions").document(docId),record);
        }
        batch.commit().addOnSuccessListener(x->{if(isAdded())info.setText("Study plan saved successfully in Firebase.");})
          .addOnFailureListener(e->{if(isAdded()){info.setText("Could not save plan: "+e.getMessage());save.setEnabled(true);}});
    }
}
