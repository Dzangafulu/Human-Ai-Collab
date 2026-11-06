package com.hacms.app;

import android.app.AlarmManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.*;

/** Explicit and optional disclosure before requesting sensitive Android special access. */
public class FocusSettingsFragment extends Fragment {
 private LinearLayout root;
 private Button monitoringButton;
 private TextView monitoringStatus;
 private boolean monitoringIsGranted(Context context){
   AccessibilityManager manager=(AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
   if(manager==null)return false;
   for(AccessibilityServiceInfo info:manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)){
     String id=info.getId();
     if(id!=null && id.startsWith(context.getPackageName()+"/")
          && id.contains("FocusAccessibilityService"))return true;
   }
   return false;
 }
 private void showMonitoringStatus(){
   if(!isAdded()||monitoringStatus==null||monitoringButton==null)return;
   boolean granted=monitoringIsGranted(requireContext());
   monitoringStatus.setText(granted?"App monitoring enabled by Android. This permission is retained until you revoke it, uninstall the app or the phone disables the service.":
     "App monitoring is not enabled. The timer works, but selected apps cannot be blocked until you grant Accessibility permission.");
   monitoringButton.setText(granted?"App monitoring enabled · check settings":"Grant app monitoring (Accessibility)");
 }
 @Override public void onResume(){super.onResume();showMonitoringStatus();}
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState){
  Context c=requireContext();root=Ui.vertical(c);root.addView(Ui.header(this,"Focus Mode","A study aid, not an unbreakable app lock",true));
  LinearLayout card=Ui.panel(c);
  Switch toggle=new Switch(c);toggle.setText("Enable Focus Mode");toggle.setTextColor(Ui.text(c));toggle.setChecked(FocusModeManager.enabled(c));card.addView(toggle);
  toggle.setOnCheckedChangeListener((button,enabled)->{
    if(enabled&&!FocusModeManager.consent(c)){toggle.setChecked(false);new AlertDialog.Builder(c).setTitle("Focus Mode and Accessibility disclosure")
      .setMessage("If you enable UniTask in Android Accessibility settings, UniTask monitors which app is in the foreground, ONLY during opted-in study sessions. It compares app package names with your local distraction list and attempts to display a blocking screen. It does not inspect screen text, collect typed content, or send app activity to Firebase. The service can be disabled in Android Settings at any time. App restrictions are best-effort and can be bypassed. Continue?")
      .setNegativeButton("Not now",null).setPositiveButton("I agree",(d,w)->{FocusModeManager.setConsent(c);toggle.setChecked(true);}).show();return;}
    FocusModeManager.setEnabled(c,enabled);
  });
  card.addView(Ui.muted(c,"Selected apps and permissions are stored on this phone. Schedules come from your saved study sessions. Calls remain allowed according to your Android Do Not Disturb settings.",13),Ui.lp(c,12));
  Button apps=Ui.button(c,"Choose apps to avoid");apps.setOnClickListener(v->chooseApps());card.addView(apps,Ui.lp(c,12));
  monitoringStatus=Ui.muted(c,"Checking Android permission...",13);
  card.addView(monitoringStatus,Ui.lp(c,10));
  monitoringButton=Ui.button(c,"Grant app monitoring (Accessibility)");
  Button accessibility=monitoringButton;
  accessibility.setOnClickListener(v->{if(monitoringIsGranted(c)){Ui.toast(c,"Monitoring is already enabled. No new permission needed.");return;}new AlertDialog.Builder(c).setTitle("Enable app monitoring")
    .setMessage("Android Settings → Accessibility → Installed apps → UniTask Focus Mode → Enable. Only opt in if you understand the on-screen disclosure. This is not required for the timer alone.")
    .setPositiveButton("Open Settings",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("Cancel",null).show();});card.addView(accessibility,Ui.lp(c,8));
  Switch mute=new Switch(c);mute.setText("Silence interruptions during sessions");mute.setTextColor(Ui.text(c));mute.setChecked(FocusModeManager.mute(c));card.addView(mute,Ui.lp(c,18));mute.setOnCheckedChangeListener((v,on)->FocusModeManager.setMute(c,on));
  Button dnd=Ui.button(c,"Allow Do Not Disturb access");dnd.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));card.addView(dnd,Ui.lp(c,8));
  card.addView(Ui.muted(c,"Set permitted calls in Android Settings → Modes / Do Not Disturb. UniTask will not override an already active DND mode.",12),Ui.lp(c,8));
  Button alarm=Ui.button(c,"Enable timely starts (Alarms & reminders)");alarm.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=31){Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+c.getPackageName()));startActivity(i);}else Ui.toast(c,"No extra alarm permission needed on this Android version");});card.addView(alarm,Ui.lp(c,8));
  Button early=Ui.button(c,"End current Focus Mode early");early.setOnClickListener(v->startActivity(new Intent(c,FocusUnlockActivity.class)));card.addView(early,Ui.lp(c,10));
  root.addView(card,Ui.lp(c,20));showMonitoringStatus();root.addView(Ui.muted(c,"Restrictions require Accessibility permission and may be delayed or bypassed by Android. Disabling this service or rebooting your phone can change behavior. Not intended for parental or device administration.",12),Ui.lp(c,16));return Ui.scroll(c,root);
 }
 private void chooseApps(){Context c=requireContext();Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  PackageManager pm=c.getPackageManager();List<ResolveInfo> installed=pm.queryIntentActivities(query,0);
  TreeMap<String,String> packages=new TreeMap<>();for(ResolveInfo info:installed){if(info.activityInfo==null)continue;String pkg=info.activityInfo.packageName;if(pkg.equals(c.getPackageName()))continue;
    packages.put(pkg,info.loadLabel(pm).toString());}
  List<String> ids=new ArrayList<>(packages.keySet());if(ids.isEmpty()){Ui.toast(c,"No launchable apps found");return;}
  String[] names=new String[ids.size()];boolean[] selected=new boolean[ids.size()];Set<String> blocked=FocusModeManager.blocked(c);
  for(int i=0;i<ids.size();i++){names[i]=packages.get(ids.get(i));selected[i]=blocked.contains(ids.get(i));}
  new AlertDialog.Builder(c).setTitle("Select distracting apps on this phone")
    .setMultiChoiceItems(names,selected,(dialog,index,on)->selected[index]=on)
    .setNegativeButton("Cancel",null).setPositiveButton("Save selection",(dialog,w)->{Set<String> next=new HashSet<>();for(int i=0;i<ids.size();i++)if(selected[i])next.add(ids.get(i));FocusModeManager.setBlocked(c,next);Ui.toast(c,"Saved "+next.size()+" restricted apps");}).show();
 }
}
