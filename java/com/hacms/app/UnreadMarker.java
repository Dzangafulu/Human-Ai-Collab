package com.hacms.app;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;
/** Per-user read receipt persisted in Firestore; no backend worker needed. */
public final class UnreadMarker {
 private UnreadMarker(){}
 public static void mark(DocumentReference parent,QuerySnapshot messages){
  String uid=FirebaseAuth.getInstance().getUid();if(uid==null||messages==null)return;
  long latest=0;for(DocumentSnapshot d:messages.getDocuments()){Long when=d.getLong("createdAt");if(when!=null)latest=Math.max(latest,when);}
  if(latest<=0)return;
  DocumentReference ref=parent.collection("read_receipts").document(uid);final long mark=latest;
  ref.get().addOnSuccessListener(existing->{Long previous=existing.getLong("lastReadAt");if(previous!=null&&previous>=mark)return;
   Map<String,Object> entry=new HashMap<>();entry.put("uid",uid);entry.put("lastReadAt",mark);ref.set(entry);
  });
 }
}
