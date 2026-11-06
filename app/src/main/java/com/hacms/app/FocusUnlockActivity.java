package com.hacms.app;
import android.os.Bundle;
import android.widget.*;
import android.content.Context;
import android.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.MultiFactorResolver;
import com.google.firebase.auth.MultiFactorInfo;
import com.google.firebase.auth.TotpMultiFactorInfo;
import com.google.firebase.auth.TotpMultiFactorGenerator;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.*;
/** Password-protected app-level early exit. Android system permissions can always be revoked by the owner. */
public class FocusUnlockActivity extends AppCompatActivity {
 @Override protected void onResume(){super.onResume();FocusAccessibilityService.acknowledgeNavigation(false);}
 @Override public void onCreate(Bundle b){super.onCreate(b);Context c=this;LinearLayout layout=Ui.vertical(c);layout.setPadding(32,60,32,30);layout.setBackgroundColor(Ui.bg(c));
 layout.addView(Ui.label(c,"End Focus early",25,true));
 layout.addView(Ui.muted(c,"Enter your UniTask account password. This protects the in-app exit; Android settings remain under your control.",14),Ui.lp(c,18));
 EditText password=Ui.field(c,"Password");password.setInputType(129);layout.addView(password,Ui.lp(c,15));
 Button finish=Ui.button(c,"Verify & End Focus");layout.addView(finish,Ui.lp(c,12));
 Button reset=Ui.button(c,"Forgot password? Send reset email");layout.addView(reset,Ui.lp(c,8));
 Button cancel=Ui.button(c,"Return to study");layout.addView(cancel,Ui.lp(c,8));setContentView(layout);
 cancel.setOnClickListener(v->finish());
 reset.setOnClickListener(v->{FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();if(u==null||u.getEmail()==null){Ui.toast(c,"Sign in to reset your password");return;}FirebaseAuth.getInstance().sendPasswordResetEmail(u.getEmail()).addOnSuccessListener(x->Ui.toast(c,"Password reset email sent")).addOnFailureListener(e->Ui.toast(c,e.getMessage()));});
 finish.setOnClickListener(v->{FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();if(user==null||user.getEmail()==null){Ui.toast(c,"Please sign in first");return;}String pass=password.getText().toString();if(pass.isEmpty()){password.setError("Enter password");return;}
 user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(),pass))
   .addOnSuccessListener(result->verifiedExit())
   .addOnFailureListener(err->{
      if(err instanceof FirebaseAuthMultiFactorException){askSecondFactor(((FirebaseAuthMultiFactorException)err).getResolver());}
      else Ui.toast(c,"Password verification failed: "+err.getMessage());
   });});
 }
 private void verifiedExit(){FocusModeManager.Session s=FocusModeManager.active(this);if(s!=null)FocusModeManager.skip(this,s.id);Ui.toast(this,"Focus Mode ended on this device");finish();}
 private void askSecondFactor(MultiFactorResolver resolver){
  MultiFactorInfo factor=null;for(MultiFactorInfo hint:resolver.getHints())if(hint instanceof TotpMultiFactorInfo){factor=hint;break;}
  if(factor==null){Ui.toast(this,"Additional MFA verification is required. Use your normal UniTask sign-in or device settings.");return;}
  final String factorId=factor.getUid();EditText input=Ui.field(this,"Authenticator's 6-digit code");input.setInputType(2);
  new AlertDialog.Builder(this).setTitle("Verify authenticator app").setMessage("Enter the current code to finish ending Focus Mode early.")
    .setView(input).setNegativeButton("Cancel",null)
    .setPositiveButton("Verify",(d,w)->{
      String code=input.getText().toString().trim();if(code.length()!=6){Ui.toast(this,"Enter a six-digit code");return;}
      resolver.resolveSignIn(TotpMultiFactorGenerator.getAssertionForSignIn(factorId,code))
        .addOnSuccessListener(r->verifiedExit()).addOnFailureListener(e->Ui.toast(this,"Authenticator verification failed: "+e.getMessage()));
    }).show();
 }
}
