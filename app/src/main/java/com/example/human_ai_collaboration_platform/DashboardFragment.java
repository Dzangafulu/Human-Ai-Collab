package com.example.human_ai_collaboration_platform;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Dashboard screen based on the provided mobile mockup. */
public class DashboardFragment extends Fragment {
    private final FirebaseTaskRepository repository=new FirebaseTaskRepository();
    private ListenerRegistration taskListener;
    private LinearLayout upcoming, active;
    private TextView todo, doing, done, message, greeting, avatar;
    private final int navy=Color.rgb(25,46,72);
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s) {
        return i.inflate(R.layout.fragment_dashboard,c,false);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        super.onViewCreated(view,state);
        upcoming=view.findViewById(R.id.dashboardUpcomingList);
        active=view.findViewById(R.id.dashboardActiveList);
        todo=view.findViewById(R.id.countTodo);doing=view.findViewById(R.id.countDoing);
        done=view.findViewById(R.id.countDone);message=view.findViewById(R.id.dashboardMessage);
        greeting=view.findViewById(R.id.dashboardGreeting);avatar=view.findViewById(R.id.dashboardAvatar);
        TextView date=view.findViewById(R.id.dashboardDate);
        date.setText(new SimpleDateFormat("EEEE, MMMM d",Locale.getDefault()).format(Calendar.getInstance().getTime()));
        int hour=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        greeting.setText(hour<12?"Good morning":hour<17?"Good afternoon":"Good evening");
        view.findViewById(R.id.dashboardCalendar).setOnClickListener(v->
            NavHostFragment.findNavController(this).navigate(R.id.schedulerFragment));
        String uid=FirebaseAuth.getInstance().getUid();
        if(uid!=null) FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener(doc->{ if(!isAdded())return;
                String name=doc.getString("name");
                if(name!=null&&!name.trim().isEmpty()){
                    String first = name.trim().split("\\s+")[0];
                    greeting.setText(greeting.getText()+", "+first);
                    String[] words = name.trim().split("\\s+");
                    avatar.setText((""+words[0].charAt(0)+(words.length>1?words[words.length-1].charAt(0):' ')).trim().toUpperCase(Locale.getDefault()));
                }
            });
    }
    @Override public void onStart(){ super.onStart();
        taskListener=repository.listen(new FirebaseTaskRepository.OnTasks(){
            public void onResult(List<Task> tasks){ if(isAdded() && getView()!=null)draw(tasks); }
            public void onError(String text){ if(isAdded() && message!=null)message.setText("Could not load tasks: "+text); }
        });
    }
    @Override public void onStop(){ if(taskListener!=null){taskListener.remove();taskListener=null;}super.onStop(); }
    private void draw(List<Task> list){
        int nTodo=0,nDoing=0,nDone=0;
        for(Task t:list){switch(t.getStatus()==null?"":t.getStatus()){
            case "To Do":nTodo++;break;case "In Progress":nDoing++;break;case "Completed":nDone++;break;
        }}
        todo.setText(String.valueOf(nTodo));doing.setText(String.valueOf(nDoing));done.setText(String.valueOf(nDone));
        List<Task> ranked=StudyPlanEngine.ranked(list);
        upcoming.removeAllViews();active.removeAllViews();
        if(ranked.isEmpty()){ addUpcomingLine("No upcoming tasks yet. Add a task to get started."); }
        else for(int i=0;i<Math.min(3,ranked.size());i++){
            Task t=ranked.get(i); addUpcomingLine(t.getTitle()+"  •  "+dueText(t));
        }
        int added=0;
        for(Task t:ranked){
            if(!"In Progress".equalsIgnoreCase(t.getStatus())) continue;
            addActiveCard(t);added++;if(added==2)break;
        }
        if(added==0){
            for(Task t:ranked){addActiveCard(t);added++;if(added==2)break;}
        }
        if(added==0){TextView empty=new TextView(requireContext());empty.setText("Your active tasks will appear here.");empty.setTextColor(Color.GRAY);active.addView(empty);}
        message.setText("Live tasks from Firestore • Tap the calendar icon for your study plan");
    }
    private String dueText(Task t){return "Due: "+t.getDueDate();}
    private void addUpcomingLine(String line){TextView tv=new TextView(requireContext());tv.setText(line);tv.setTextSize(13);tv.setTextColor(Color.WHITE);tv.setPadding(0,dp(3),0,dp(3));upcoming.addView(tv);}
    private void addActiveCard(Task task){
        LinearLayout box=new LinearLayout(requireContext());box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14),dp(14),dp(14),dp(14));box.setBackgroundResource(R.drawable.dashboard_white_card);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);box.setLayoutParams(lp);
        TextView title=new TextView(requireContext());title.setText(task.getTitle());title.setTextSize(15);title.setTextColor(navy);title.setTypeface(null,Typeface.BOLD);box.addView(title);
        TextView meta=new TextView(requireContext());meta.setText(task.getPriority()+" priority  •  "+dueText(task)+"  •  "+task.getStatus());meta.setTextColor(Color.rgb(96,115,140));meta.setTextSize(11);meta.setPadding(0,dp(6),0,dp(8));box.addView(meta);
        ProgressBar progress=new ProgressBar(requireContext(),null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);progress.setProgress("In Progress".equals(task.getStatus())?50:0);
        progress.setProgressTintList(ColorStateList.valueOf(Color.rgb(41,110,245)));
        box.addView(progress,new LinearLayout.LayoutParams(-1,dp(6)));
        // Existing Task Board has no numeric progress: 50% is a visual status indicator only.
        box.setOnClickListener(v->startActivity(new Intent(requireContext(),TaskBoardActivity.class)));
        active.addView(box);
    }
}
