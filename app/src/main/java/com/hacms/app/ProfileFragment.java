package com.hacms.app;
import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
public class ProfileFragment extends Fragment {
 private ImageView photo;private EditText name;private String imageBase64="";
 private ActivityResultLauncher<String> picker;
 @Override public void onCreate(Bundle b){super.onCreate(b);picker=registerForActivityResult(new ActivityResultContracts.GetContent(),uri->{if(uri==null)return;try{
 Bitmap original=BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(uri));if(original==null)return;
 int width=original.getWidth(),height=original.getHeight(),size=Math.min(width,height);
 Bitmap crop=Bitmap.createBitmap(original,(width-size)/2,(height-size)/2,size,size);Bitmap small=Bitmap.createScaledBitmap(crop,160,160,true);
 ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,65,out);imageBase64=Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);photo.setImageBitmap(small);
 }catch(Exception e){Ui.toast(requireContext(),"Could not read photo: "+e.getMessage());}});}
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup p,@Nullable Bundle b){Context c=requireContext();LinearLayout root=Ui.vertical(c);root.addView(Ui.header(this,"Your profile","Your account and personal settings", true));photo=new ImageView(c);photo.setImageResource(R.drawable.unitask_icon);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
 LinearLayout.LayoutParams pic=new LinearLayout.LayoutParams(Ui.dp(c,90),Ui.dp(c,90));pic.gravity=Gravity.CENTER_HORIZONTAL;pic.topMargin=Ui.dp(c,24);root.addView(photo,pic);
 Button choose=Ui.button(c,"Choose profile photo");choose.setOnClickListener(v->picker.launch("image/*"));root.addView(choose,Ui.lp(c,12));
 name=Ui.field(c,"Display name");root.addView(name,Ui.lp(c,12));FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
 root.addView(Ui.muted(c,u==null?"":Ui.safe(u.getEmail()),14),Ui.lp(c,9));
 Button save=Ui.button(c,"Save profile");root.addView(save,Ui.lp(c,20));
 if(u!=null){FirebaseFirestore db=FirebaseFirestore.getInstance();db.collection("users").document(u.getUid()).get().addOnSuccessListener(d->{if(!isAdded())return;name.setText(d.getString("name"));String x=d.getString("photoBase64");if(x!=null&&!x.isEmpty()){imageBase64=x;try{byte[] raw=Base64.decode(x,Base64.DEFAULT);photo.setImageBitmap(BitmapFactory.decodeByteArray(raw,0,raw.length));}catch(Exception ignored){}}});
 save.setOnClickListener(v->{String n=name.getText().toString().trim();if(n.length()<2){name.setError("Enter your name");return;}Map<String,Object> update=new HashMap<>();update.put("name",n);update.put("emailLowercase",Ui.safe(u.getEmail()).toLowerCase(Locale.ROOT));if(!imageBase64.isEmpty())update.put("photoBase64",imageBase64);db.collection("users").document(u.getUid()).set(update,SetOptions.merge()).addOnSuccessListener(x->Ui.toast(c,"Profile saved")).addOnFailureListener(e->Ui.toast(c,e.getMessage()));});}
 Button reset=Ui.button(c,"Send password reset email");reset.setOnClickListener(v->{FirebaseUser current=FirebaseAuth.getInstance().getCurrentUser();if(current==null||current.getEmail()==null)return;FirebaseAuth.getInstance().sendPasswordResetEmail(current.getEmail()).addOnSuccessListener(x->Ui.toast(c,"Password reset email sent")).addOnFailureListener(e->Ui.toast(c,e.getMessage()));});root.addView(reset,Ui.lp(c,10));
 Button settings=Ui.button(c,"Settings");settings.setOnClickListener(v->Ui.open(this,R.id.settingsFragment));root.addView(settings,Ui.lp(c,8));
 root.addView(Ui.muted(c,"Photos are compressed before storage in your profile document. Do not upload sensitive photographs.",12),Ui.lp(c,12));return Ui.scroll(c,root);}
}
