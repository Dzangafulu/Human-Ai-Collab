package com.hacms.app;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

/** Groups and group messages backed by Firestore. Group tasks are created from the Tasks tab. */
public class GroupFragment extends Fragment {
 private final FirebaseFirestore db=FirebaseFirestore.getInstance();private LinearLayout content;private ListenerRegistration stream;
 private String uid(){return FirebaseAuth.getInstance().getUid();}
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup p,@Nullable Bundle state){Context c=requireContext();LinearLayout root=Ui.vertical(c);String id=getArguments()==null?"":getArguments().getString("groupId","");
 root.addView(Ui.header(this,id.isEmpty()?"Study groups":"Group conversation",id.isEmpty()?"Collaborate and assign work":"Chat together, ask @Unify, complete tasks", true));
 if(id.isEmpty()){Button create=Ui.button(c,"+ Create study group");create.setOnClickListener(v->createGroup());root.addView(create,Ui.lp(c,16));}
 content=Ui.vertical(c);root.addView(content,Ui.lp(c,15));if(!id.isEmpty()){

 LinearLayout bar=Ui.horizontal(c);
 EditText message=Ui.field(c,"Message your group or ask @Unify");bar.addView(message,new LinearLayout.LayoutParams(0,-2,1));
 Button send=Ui.button(c,"Send");bar.addView(send);root.addView(bar,Ui.lp(c,15));send.setOnClickListener(v->{String text=message.getText().toString().trim();if(text.isEmpty())return;post(id,text,false);message.setText("");if(text.toLowerCase(Locale.ROOT).startsWith("@unify"))askUnify(id,text.substring(6).trim());});

 ImageButton member=new ImageButton(c);
 member.setImageResource(android.R.drawable.ic_input_add);
 member.setColorFilter(Color.WHITE);
 member.setBackground(Ui.shape(Ui.BLUE, Ui.dp(c, 12), 100));
 member.setContentDescription("Add a connection to group");
 member.setOnClickListener(v->chooseMembers(ids->{if(ids.isEmpty())return;db.collection("groups").document(id).get().addOnSuccessListener(g->{List<String> m=(List<String>)g.get("memberUids");if(m==null)m=new ArrayList<>();List<String> next=new ArrayList<>(m);for(String x:ids)if(!next.contains(x))next.add(x);db.collection("groups").document(id).update("memberUids",next).addOnSuccessListener(a->Ui.toast(c,"Members added")).addOnFailureListener(e->Ui.toast(c,e.getMessage()));});}));

 LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48));
 mp.setMargins(0, 0, Ui.dp(c, 8), 0);
 bar.addView(member,0, mp);}
 return Ui.scroll(c,root);}

 @Override public void onStart(){super.onStart();String id=getArguments()==null?"":getArguments().getString("groupId","");if(id.isEmpty())stream=db.collection("groups").whereArrayContains("memberUids",uid()).addSnapshotListener((s,e)->{if(!isAdded())return;content.removeAllViews();if(e!=null){content.addView(Ui.muted(requireContext(),e.getMessage(),13));return;}if(s==null||s.isEmpty())content.addView(Ui.muted(requireContext(),"No groups yet. Create one with your approved connections.",14));else for(DocumentSnapshot d:s.getDocuments()){
 TextView row=Ui.pill(requireContext(),"👥  "+Ui.safe(d.getString("name"))+"     ›",Ui.card(requireContext()),Ui.text(requireContext()));content.addView(row,Ui.lp(requireContext(),9));row.setOnClickListener(v->{Bundle b=new Bundle();b.putString("groupId",d.getId());androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.groupFragment,b);});}});
 else stream=db.collection("groups").document(id).collection("messages").orderBy("createdAt").limit(100).addSnapshotListener((s,e)->{if(!isAdded())return;content.removeAllViews();if(e!=null){content.addView(Ui.muted(requireContext(),e.getMessage(),14));return;}if(s!=null)for(DocumentSnapshot d:s.getDocuments()){boolean bot=Boolean.TRUE.equals(d.getBoolean("isBot"));String from=bot?"Unify":uid().equals(d.getString("senderUid"))?"You":Ui.safe(d.getString("senderName"));LinearLayout panel=Ui.panel(requireContext());panel.addView(Ui.muted(requireContext(),from,12));panel.addView(Ui.label(requireContext(),Ui.safe(d.getString("text")),14,false),Ui.lp(requireContext(),5));content.addView(panel,Ui.lp(requireContext(),8));}});
 }
 private void post(String groupId, String body, boolean bot) {
  final long createdAt = System.currentTimeMillis(); // captured now so message order stays correct
  if (bot) { send(groupId, "Unify", body, true, createdAt); return; }

  db.collection("users").document(uid()).get().addOnCompleteListener(t -> {
   String name = null;
   if (t.isSuccessful() && t.getResult() != null) name = t.getResult().getString("name");

   if (name == null || name.trim().isEmpty()) {
    FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
    if (u != null) {
     name = u.getDisplayName();
     if ((name == null || name.trim().isEmpty()) && u.getEmail() != null)
      name = u.getEmail().split("@")[0];
    }
   }
   if (name == null || name.trim().isEmpty()) name = "Member";
   send(groupId, name.trim(), body, false, createdAt);
  });
 }

 private void send(String groupId, String senderName, String body, boolean bot, long createdAt) {
  Map<String, Object> m = new HashMap<>();
  m.put("senderUid", uid());
  m.put("senderName", senderName);
  m.put("text", body);
  m.put("isBot", bot);
  m.put("createdAt", createdAt);
  db.collection("groups").document(groupId).collection("messages").add(m)
          .addOnFailureListener(e -> { if (isAdded()) Ui.toast(requireContext(), e.getMessage()); });
 }
 private void askUnify(String id,String prompt){Context c=requireContext();if(prompt.trim().isEmpty()){post(id,"Use @Unify followed by your question.",true);return;}
 if(!c.getSharedPreferences("unitask",0).getBoolean("aiConsent",false)){post(id,"To use Gemini in group chat, enable AI consent in your Settings. Local study plans remain available.",true);return;}
 new GeminiAiClient().ask("You are Unify, a student study coach responding to a group question. Be concise. Do not assume you can see private accounts. Question: "+prompt, androidx.core.content.ContextCompat.getMainExecutor(c),new GeminiAiClient.Callback(){public void success(String text){if(isAdded())post(id,text,true);}public void failure(Throwable error){if(isAdded())post(id,"Gemini is temporarily unavailable. We can still prioritise your tasks and create a study plan in the Tasks and Scheduler screens.",true);}});}
 private interface OnSelected{void done(List<String> ids);}
 private void chooseMembers(OnSelected callback){Context c=requireContext();db.collection("connections").whereArrayContains("memberUids",uid()).get().addOnSuccessListener(s->{if(s.isEmpty()){Ui.toast(c,"First connect with another student using their email in Chats.");return;}List<String> ids=new ArrayList<>();for(DocumentSnapshot d:s.getDocuments()){Object m=d.get("memberUids");if(m instanceof List)for(Object x:(List<?>)m){String other=String.valueOf(x);if(!other.equals(uid())&&!ids.contains(other))ids.add(other);}}
 if(ids.isEmpty()){Ui.toast(c,"No connections yet");return;}List<String> names=new ArrayList<>(Collections.nCopies(ids.size(),""));for(int i=0;i<ids.size();i++){int index=i;db.collection("users").document(ids.get(i)).get().addOnSuccessListener(d->{names.set(index,Ui.safe(d.getString("name")).isEmpty()?ids.get(index):d.getString("name"));if(!names.contains("")){boolean[] selected=new boolean[ids.size()];new AlertDialog.Builder(c).setTitle("Select connected students").setMultiChoiceItems(names.toArray(new String[0]),selected,(dialog,indexSelected,isOn)->selected[indexSelected]=isOn).setNegativeButton("Cancel",null).setPositiveButton("Continue",(dialog,w)->{List<String> chosen=new ArrayList<>();for(int n=0;n<selected.length;n++)if(selected[n])chosen.add(ids.get(n));callback.done(chosen);}).show();}});}
 }).addOnFailureListener(e->Ui.toast(c,e.getMessage()));}
 private void createGroup(){Context c=requireContext();EditText name=Ui.field(c,"Group name, e.g. Database Project");new AlertDialog.Builder(c).setTitle("Create study group").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Select members",(d,w)->{String text=name.getText().toString().trim();if(text.length()<3){Ui.toast(c,"Group name is too short");return;}chooseMembers(chosen->{if(chosen.isEmpty()){Ui.toast(c,"Choose at least one connection");return;}List<String> members=new ArrayList<>();members.add(uid());members.addAll(chosen);Map<String,Object> m=new HashMap<>();m.put("name",text);m.put("ownerUid",uid());m.put("memberUids",members);m.put("createdAt",System.currentTimeMillis());db.collection("groups").add(m).addOnSuccessListener(x->Ui.toast(c,"Group created. Open it to chat with members.")).addOnFailureListener(e->Ui.toast(c,e.getMessage()));});}).show();}
 @Override public void onStop(){super.onStop();if(stream!=null)stream.remove();}
}
