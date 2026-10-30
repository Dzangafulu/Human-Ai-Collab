package com.hacms.app;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
/** 1.1-second animated brand screen, no network request. */
public class SplashActivity extends AppCompatActivity {
  @Override public void onCreate(Bundle b){super.onCreate(b);
    LinearLayout content=Ui.vertical(this);content.setGravity(Gravity.CENTER);content.setBackgroundColor(Ui.bg(this));content.setPadding(Ui.dp(this,22),0,Ui.dp(this,22),0);
    ImageView image=new ImageView(this);image.setImageResource(R.drawable.unitask_lockup);image.setScaleType(ImageView.ScaleType.FIT_CENTER);content.addView(image,new LinearLayout.LayoutParams(-1,Ui.dp(this,155)));
    TextView motto=Ui.muted(this,"Study smarter, achieve more",16);motto.setGravity(Gravity.CENTER);content.addView(motto);
    image.setAlpha(0f);image.setTranslationY(Ui.dp(this,20));image.animate().alpha(1f).translationY(0).setDuration(700).start();
    motto.setAlpha(0f);motto.animate().alpha(1).setStartDelay(350).setDuration(500).start();setContentView(content);
    new Handler(Looper.getMainLooper()).postDelayed(()->{ startActivity(new Intent(this,LoginActivity.class));finish(); },1350);
  }
}
