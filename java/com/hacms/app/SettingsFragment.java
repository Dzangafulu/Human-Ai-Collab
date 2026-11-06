package com.hacms.app;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
public class SettingsFragment extends Fragment {
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater inf,@Nullable ViewGroup p,@Nullable Bundle s){Context c=requireContext();LinearLayout l=Ui.vertical(c);l.addView(Ui.header(this,"Settings","Personalise your UniTask workspace", true));
 LinearLayout card=Ui.panel(c);Switch dark=new Switch(c);dark.setText("Dark mode");dark.setTextColor(Ui.text(c));dark.setChecked(c.getSharedPreferences("unitask",0).getBoolean("dark",false));card.addView(dark);
 dark.setOnCheckedChangeListener((v,checked)->{c.getSharedPreferences("unitask",0).edit().putBoolean("dark",checked).apply();AppCompatDelegate.setDefaultNightMode(checked?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO);});
 Switch share=new Switch(c);share.setText("Share task details with Unify AI");share.setTextColor(Ui.text(c));share.setChecked(c.getSharedPreferences("unitask",0).getBoolean("shareTasks",false));card.addView(share);
 share.setOnCheckedChangeListener((v,b)->c.getSharedPreferences("unitask",0).edit().putBoolean("shareTasks",b).apply());
 Switch consent=new Switch(c);consent.setText("Allow Gemini processing of my messages");consent.setTextColor(Ui.text(c));consent.setChecked(c.getSharedPreferences("unitask",0).getBoolean("aiConsent",false));card.addView(consent);
 consent.setOnCheckedChangeListener((v,b)->c.getSharedPreferences("unitask",0).edit().putBoolean("aiConsent",b).apply());
 card.addView(Ui.muted(c,"AI features use Google's Gemini API (subject to free-tier limits). Local planning stays available if the AI service is unavailable.",12),Ui.lp(c,12));l.addView(card,Ui.lp(c,20));
 Button focus=Ui.button(c,"Focus Mode · manage distracting apps");focus.setOnClickListener(v->Ui.open(this,R.id.focusSettingsFragment));l.addView(focus,Ui.lp(c,14));
 Button profile=Ui.button(c,"Edit profile");profile.setOnClickListener(v->Ui.open(this,R.id.profileFragment));l.addView(profile,Ui.lp(c,14));
 Button signout=Ui.button(c,"Log out");signout.setOnClickListener(v->{FirebaseAuth.getInstance().signOut();startActivity(new Intent(c,LoginActivity.class));requireActivity().finish();});l.addView(signout,Ui.lp(c,12));return Ui.scroll(c,l);}
}
