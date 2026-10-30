package com.hacms.app;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/** Private messages stored under accepted connection documents; no global list of registered users. */
public class DirectChatFragment extends Fragment {
 private final FirebaseFirestore db=FirebaseFirestore.getInstance();private LinearLayout messages;private ListenerRegistration listener;
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){Context c=requireContext();String connection=getArguments()==null?"":getArguments().getString("connectionId","");String name=getArguments()==null?"Chat":getArguments().getString("name","Chat");
 LinearLayout screen=Ui.vertical(c);screen.setBackgroundColor(Ui.bg(c));LinearLayout top=Ui.vertical(c);top.setPadding(18,12,18,10);top.addView(Ui.label(c,name,23,true));top.addView(Ui.muted(c,"Private conversation",12));screen.addView(top);
 messages=Ui.vertical(c);ScrollView scroll=Ui.scroll(c,messages);screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout bar=Ui.horizontal(c);bar.setPadding(10,6,10,10);EditText input=Ui.field(c,"Type a message…");bar.addView(input,new LinearLayout.LayoutParams(0,-2,1));Button send=Ui.button(c,"Send");bar.addView(send);screen.addView(bar);send.setOnClickListener(v->{String body=input.getText().toString().trim();if(body.isEmpty())return;Map<String,Object> value=new HashMap<>();value.put("senderUid",FirebaseAuth.getInstance().getUid());value.put("text",body);value.put("createdAt",System.currentTimeMillis());db.collection("connections").document(connection).collection("messages").add(value).addOnSuccessListener(x->input.setText("")).addOnFailureListener(e->Ui.toast(c,e.getMessage()));});return screen;}
 @Override public void onStart(){super.onStart();String conn=getArguments()==null?"":getArguments().getString("connectionId","");listener=db.collection("connections").document(conn).collection("messages").orderBy("createdAt").limit(100).addSnapshotListener((s,e)->{if(!isAdded()||messages==null)return;messages.removeAllViews();if(e!=null){messages.addView(Ui.muted(requireContext(),e.getMessage(),12));return;}if(s!=null)for(DocumentSnapshot d:s.getDocuments()){boolean mine=FirebaseAuth.getInstance().getUid().equals(d.getString("senderUid"));TextView bubble=Ui.pill(requireContext(),Ui.safe(d.getString("text")),mine?0xffdce9ff:Ui.card(requireContext()),mine?Ui.NAVY:Ui.text(requireContext()));messages.addView(bubble,Ui.lp(requireContext(),8));}});}
 @Override public void onStop(){super.onStop();if(listener!=null)listener.remove();}
}
