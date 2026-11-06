package com.hacms.app;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;

/** Visible focus timer and voluntary early exit; never traps the device owner. */
public class FocusStatusFragment extends Fragment {
 private TextView status;private Button early;private Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable timer=new Runnable(){@Override public void run(){
   if(status!=null&&isAdded()){FocusModeManager.Session focused=FocusModeManager.active(requireContext());
    StudySessionTimeline.ActiveSession current=StudySessionTimeline.active(requireContext());
    if(early!=null)early.setVisibility(focused!=null?View.VISIBLE:View.GONE);
    if(focused==null && current==null)status.setText("No study session is currently active.");
    else {long end=focused!=null?focused.until:current.until;
     String title=focused!=null?focused.title:current.title;
     long time=Math.max(0,(end-System.currentTimeMillis())/1000);
     status.setText(title+"\n\n"+String.format(java.util.Locale.US,"%02d:%02d:%02d remaining",time/3600,(time/60)%60,time%60)
         +(focused!=null?"\n\nFocus Mode is on":"\n\nStudy session in progress"));}
    handler.postDelayed(this,1000);
   }
 }};
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup parent,@Nullable Bundle b){
   android.content.Context c=requireContext();LinearLayout root=Ui.vertical(c);root.addView(Ui.header(this,"Focus Mode","Keep your study session on track",true));
   status=Ui.label(c,"Checking scheduled session...",22,true);status.setGravity(Gravity.CENTER);root.addView(status,Ui.lp(c,40));
   early=Ui.button(c,"End Focus early (verify password)");early.setOnClickListener(v->startActivity(new Intent(c,FocusUnlockActivity.class)));root.addView(early,Ui.lp(c,30));
   Button dashboard=Ui.button(c,"Return to dashboard");
   dashboard.setOnClickListener(v->Ui.open(this,R.id.dashboardFragment));
   root.addView(dashboard,Ui.lp(c,12));
   Button settings=Ui.button(c,"Focus Mode settings");settings.setOnClickListener(v->Ui.open(this,R.id.focusSettingsFragment));root.addView(settings,Ui.lp(c,12));
   root.addView(Ui.muted(c,"This is a best-effort focus tool. You can always change Android system permissions in Settings.",12),Ui.lp(c,22));
   return Ui.scroll(c,root);
 }
 @Override public void onStart(){super.onStart();handler.post(timer);} @Override public void onStop(){handler.removeCallbacks(timer);super.onStop();}
}
