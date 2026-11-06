package com.hacms.app;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/** Contacts visible only after a recipient accepts a chat request. Search is by exact email, never a user directory. */
public class ContactsFragment extends Fragment {
 private LinearLayout content;private final FirebaseFirestore db=FirebaseFirestore.getInstance();
 private ListenerRegistration contacts,requests;
 private String uid(){return FirebaseAuth.getInstance().getUid();}
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater in,@Nullable ViewGroup p,@Nullable Bundle state){Context c=requireContext();LinearLayout root=Ui.vertical(c);root.addView(Ui.header(this,"Chats","Your connections and study groups"));
  EditText email=Ui.field(c,"Find someone by exact email address");email.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);root.addView(email,Ui.lp(c,17));
  Button search=Ui.button(c,"Find student & request chat");search.setOnClickListener(v->request(email.getText().toString().trim().toLowerCase(Locale.ROOT)));root.addView(search,Ui.lp(c,7));
  Button group=Ui.button(c,"Groups and collaboration");group.setOnClickListener(v->Ui.open(this,R.id.groupFragment));root.addView(group,Ui.lp(c,12));
  content=Ui.vertical(c);root.addView(content,Ui.lp(c,18));ensureSections();return Ui.scroll(c,root);}
 @Override public void onStart(){super.onStart();String u=uid();if(u==null)return; contacts=db.collection("connections").whereArrayContains("memberUids",u).addSnapshotListener((s,e)->{if(!isAdded())return;renderContacts(s,e);});
  requests=db.collection("chat_requests").whereEqualTo("receiverUid",u).addSnapshotListener((s,e)->{if(!isAdded())return;renderRequests(s,e);});}
 private final LinearLayout[] sections=new LinearLayout[2];
 private void ensureSections(){if(content==null)return;content.removeAllViews();for(int i=0;i<2;i++){sections[i]=Ui.vertical(requireContext());content.addView(Ui.label(requireContext(),i==0?"Your connections":"Pending chat requests",18,true),Ui.lp(requireContext(),10));content.addView(sections[i],Ui.lp(requireContext(),7));}}
 private void renderContacts(QuerySnapshot snap,Exception e){if(!isAdded()||content==null)return;if(sections[0]==null)ensureSections();sections[0].removeAllViews();if(e!=null){sections[0].addView(Ui.muted(requireContext(),e.getMessage(),13));return;}if(snap==null||snap.isEmpty()){sections[0].addView(Ui.muted(requireContext(),"No connections yet. Search for an email to send a request.",13));return;}
  for(DocumentSnapshot d:snap.getDocuments()){Object arr=d.get("memberUids");if(!(arr instanceof List))continue;List<?> mem=(List<?>)arr;String other="";for(Object uid:mem)if(!uid.equals(uid()))other=String.valueOf(uid);String friend=other;db.collection("users").document(friend).get().addOnSuccessListener(profile->{if(!isAdded())return;String displayName=Ui.safe(profile.getString("name"));final String title=displayName.isEmpty()?Ui.safe(profile.getString("email")):displayName;TextView row=Ui.pill(requireContext(),"●  "+title+"    ›",Ui.card(requireContext()),Ui.text(requireContext()));ThreadUnreadIndicator.bind(row,db.collection("connections").document(d.getId()),"●  "+title+"    ›");sections[0].addView(row,Ui.lp(requireContext(),8));row.setOnClickListener(v->{Bundle b=new Bundle();b.putString("connectionId",d.getId());b.putString("name",title);androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.chatConversationFragment,b);});});}}
 private void renderRequests(QuerySnapshot snap,Exception e){if(!isAdded()||content==null)return;if(sections[1]==null)ensureSections();sections[1].removeAllViews();if(e!=null){sections[1].addView(Ui.muted(requireContext(),e.getMessage(),13));return;}int count=0;if(snap!=null)for(DocumentSnapshot d:snap.getDocuments())if("pending".equals(d.getString("status"))){count++;String from=d.getString("senderUid");db.collection("users").document(from).get().addOnSuccessListener(u->{if(!isAdded())return;LinearLayout card=Ui.panel(requireContext());card.addView(Ui.label(requireContext(),Ui.safe(u.getString("name"))+" wants to chat",14,true));Button accept=Ui.button(requireContext(),"Accept request");accept.setOnClickListener(v->accept(d.getId(),from));card.addView(accept,Ui.lp(requireContext(),6));Button reject=Ui.button(requireContext(),"Decline");reject.setOnClickListener(v->db.collection("chat_requests").document(d.getId()).update("status","declined"));card.addView(reject);sections[1].addView(card,Ui.lp(requireContext(),8));});}
  if(count==0)sections[1].addView(Ui.muted(requireContext(),"No pending requests.",13));}
 private void request(String email){if(email.isEmpty()||!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){Ui.toast(requireContext(),"Enter a valid email address");return;}String me=uid();db.collection("users").whereEqualTo("emailLowercase",email).limit(1).get().addOnSuccessListener(s->{if(s.isEmpty()){ // Backward compatibility with existing signups not yet indexed.
  db.collection("users").whereEqualTo("email",email).limit(1).get().addOnSuccessListener(second->{if(second.isEmpty())Ui.toast(requireContext(),"No student found with that email");else send(second.getDocuments().get(0).getId());}).addOnFailureListener(e->Ui.toast(requireContext(),e.getMessage()));
 }else send(s.getDocuments().get(0).getId());}).addOnFailureListener(e->Ui.toast(requireContext(),e.getMessage()));}
 /**
  * Checks only connection documents that the signed-in user is allowed to read.
  * Fetching the not-yet-existing connections/{pair} document is denied by the
  * membership-only Firestore rule and previously failed without an error UI.
  */
 private void send(String to) {
  String me = uid();
  if (me == null) {
   if (isAdded()) Ui.toast(requireContext(), "Please sign in before requesting a chat");
   return;
  }
  if (me.equals(to)) {
   if (isAdded()) Ui.toast(requireContext(), "You cannot add yourself");
   return;
  }

  // This query is permitted by the connections rule because every result
  // has the current user in memberUids. No unauthorized missing-doc get().
  db.collection("connections").whereArrayContains("memberUids", me).get()
          .addOnSuccessListener(existing -> {
           if (!isAdded()) return;
           for (DocumentSnapshot connection : existing.getDocuments()) {
            Object members = connection.get("memberUids");
            if (members instanceof List && ((List<?>) members).contains(to)) {
             Ui.toast(requireContext(), "You are already connected to this student");
             return;
            }
           }
           checkOutgoingRequest(me, to);
          })
          .addOnFailureListener(e -> chatError("Couldn't check your connections", e));
 }

 private void checkOutgoingRequest(String me, String to) {
  // Only request documents owned by this sender can be returned.
  db.collection("chat_requests").whereEqualTo("senderUid", me).get()
          .addOnSuccessListener(outgoing -> {
           if (!isAdded()) return;
           for (DocumentSnapshot request : outgoing.getDocuments()) {
            if (!to.equals(request.getString("receiverUid"))) continue;
            String status = request.getString("status");
            if ("pending".equals(status)) {
             Ui.toast(requireContext(), "Chat request already sent. Waiting for a response.");
            } else if ("declined".equals(status)) {
             Ui.toast(requireContext(), "A previous request was declined. Ask that student to send you a request.");
            } else {
             Ui.toast(requireContext(), "A previous request already exists. Check your connections.");
            }
            return;
           }
           checkIncomingRequest(me, to);
          })
          .addOnFailureListener(e -> chatError("Couldn't check sent requests", e));
 }

 private void checkIncomingRequest(String me, String to) {
  // Avoid sending a duplicate when this student has already requested us.
  db.collection("chat_requests").whereEqualTo("receiverUid", me).get()
          .addOnSuccessListener(incoming -> {
           if (!isAdded()) return;
           for (DocumentSnapshot request : incoming.getDocuments()) {
            if (to.equals(request.getString("senderUid"))
                    && "pending".equals(request.getString("status"))) {
             Ui.toast(requireContext(), "This student already sent you a request. Accept it below.");
             return;
            }
           }
           createChatRequest(me, to);
          })
          .addOnFailureListener(e -> chatError("Couldn't check received requests", e));
 }

 private void createChatRequest(String me, String to) {
  String requestId = me + "_" + to;
  Map<String,Object> request = new HashMap<>();
  request.put("senderUid", me);
  request.put("receiverUid", to);
  request.put("status", "pending");
  request.put("createdAt", System.currentTimeMillis());
  db.collection("chat_requests").document(requestId).set(request)
          .addOnSuccessListener(unused -> {
           if (isAdded()) Ui.toast(requireContext(), "Chat request sent");
          })
          .addOnFailureListener(e -> chatError("Couldn't send chat request", e));
 }

 private void chatError(String message, Exception error) {
  android.util.Log.e("UniTaskChatRequest", message, error);
  if (isAdded()) Ui.toast(requireContext(), message + ": "
          + (error == null ? "Unknown error" : String.valueOf(error.getMessage())));
 }
 private void accept(String requestId,String from){String me=uid();String pair=pair(me,from);Map<String,Object> m=new HashMap<>();m.put("memberUids",Arrays.asList(me,from));m.put("createdAt",System.currentTimeMillis());db.collection("connections").document(pair).set(m).addOnSuccessListener(v->db.collection("chat_requests").document(requestId).update("status","accepted").addOnSuccessListener(x->Ui.toast(requireContext(),"Request accepted"))).addOnFailureListener(e->Ui.toast(requireContext(),e.getMessage()));}
 public static String pair(String x,String y){return x.compareTo(y)<0?x+"_"+y:y+"_"+x;}
 @Override public void onStop(){super.onStop();if(contacts!=null)contacts.remove();if(requests!=null)requests.remove();}
}
