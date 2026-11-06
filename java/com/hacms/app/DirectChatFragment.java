package com.hacms.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/** Approved connections only; scrollable messages and a bottom-anchored composer. */
public class DirectChatFragment extends Fragment {
 private final FirebaseFirestore db=FirebaseFirestore.getInstance();private LinearLayout messages;private ListenerRegistration listener;
 private String connection;private ScrollView messageScroll;
 private final ActivityResultLauncher<Intent> chooseAttachment=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),r->{
  if(r.getResultCode()!=Activity.RESULT_OK||r.getData()==null||connection==null)return;
  MessageAttachment.prepare(requireContext(),r.getData().getData(),new MessageAttachment.Ready(){
    public void ready(Map<String,Object> fields){fields.put("senderUid",FirebaseAuth.getInstance().getUid());fields.put("createdAt",System.currentTimeMillis());send(fields);}
    public void error(String why){if(isAdded())Ui.toast(requireContext(),why);}
  });
 });
 private final ActivityResultLauncher<Intent> saveAttachment=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),r->{
  if(r.getResultCode()==Activity.RESULT_OK&&r.getData()!=null)MessageAttachment.saveSelected(requireContext(),r.getData().getData());
 });
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
  Context c=requireContext();connection=getArguments()==null?"":getArguments().getString("connectionId","");String name=getArguments()==null?"Chat":getArguments().getString("name","Chat");
  LinearLayout screen=Ui.vertical(c);screen.setBackgroundColor(Ui.bg(c));screen.setLayoutParams(new ViewGroup.LayoutParams(-1,-1));
  LinearLayout top=Ui.vertical(c);top.setPadding(Ui.dp(c,18),Ui.dp(c,12),Ui.dp(c,18),Ui.dp(c,10));top.addView(Ui.label(c,name,23,true));top.addView(Ui.muted(c,"Private conversation",12));screen.addView(top);
  messages=Ui.vertical(c);ScrollView scroll=Ui.scroll(c,messages);messageScroll=scroll;screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout bar=Ui.horizontal(c);bar.setPadding(Ui.dp(c,8),Ui.dp(c,6),Ui.dp(c,8),Ui.dp(c,10));
  Button attach=Ui.button(c,"+");bar.addView(attach);attach.setOnClickListener(v->chooseAttachment.launch(MessageAttachment.choose()));
  EditText input=Ui.field(c,"Type a message…");bar.addView(input,new LinearLayout.LayoutParams(0,-2,1));
  Button send=Ui.button(c,"Send");bar.addView(send);screen.addView(bar);
  send.setOnClickListener(v->{String body=input.getText().toString().trim();if(body.isEmpty())return;
   Map<String,Object> value=new HashMap<>();value.put("senderUid",FirebaseAuth.getInstance().getUid());value.put("text",body);value.put("createdAt",System.currentTimeMillis());
   db.collection("connections").document(connection).collection("messages").add(value).addOnSuccessListener(x->input.setText("")).addOnFailureListener(e->Ui.toast(c,e.getMessage()));
  });return screen;
 }
 private void send(Map<String,Object> data){if(connection==null)return;db.collection("connections").document(connection).collection("messages").add(data)
    .addOnFailureListener(e->{if(isAdded())Ui.toast(requireContext(),e.getMessage());});}
 @Override public void onStart(){super.onStart();if(connection==null||connection.isEmpty())return;
  listener=db.collection("connections").document(connection).collection("messages").orderBy("createdAt").limit(100).addSnapshotListener((s,e)->{
   if(!isAdded()||messages==null)return;messages.removeAllViews();
   if(e!=null){messages.addView(Ui.muted(requireContext(),e.getMessage(),12));return;}
   if(s!=null)for(DocumentSnapshot d:s.getDocuments()){
    boolean mine=FirebaseAuth.getInstance().getUid().equals(d.getString("senderUid"));
    LinearLayout card=Ui.panel(requireContext());
    card.addView(Ui.label(requireContext(),mine?"You":"Contact",12,true));
    card.addView(Ui.label(requireContext(),Ui.safe(d.getString("text")),14,false),Ui.lp(requireContext(),5));
    MessageAttachment.render(this,card,d,saveAttachment);messages.addView(card,Ui.lp(requireContext(),8));
   }
   if(s!=null)UnreadMarker.mark(db.collection("connections").document(connection),s);
   if(messageScroll!=null)messageScroll.post(()->messageScroll.fullScroll(View.FOCUS_DOWN));
  });
 }
 @Override public void onStop(){super.onStop();if(listener!=null){listener.remove();listener=null;}}
}
