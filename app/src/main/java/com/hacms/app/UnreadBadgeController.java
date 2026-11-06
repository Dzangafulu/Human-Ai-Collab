package com.hacms.app;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.*;
/** Live unread message indicator on the existing Chats bottom navigation item. */
public final class UnreadBadgeController {
 private final BottomNavigationView navigation;
 private final FirebaseFirestore db=FirebaseFirestore.getInstance();
 private final List<ListenerRegistration> streams=new ArrayList<>();private final Map<String,Integer> counts=new HashMap<>();
 private String uid;
 public UnreadBadgeController(BottomNavigationView view){navigation=view;}
 public void start(){stop();uid=FirebaseAuth.getInstance().getUid();if(uid==null)return;
  streams.add(db.collection("connections").whereArrayContains("memberUids",uid).addSnapshotListener((snapshot,e)->{
   if(e!=null||snapshot==null)return;subscribeThreads("connections",snapshot);
  }));
  streams.add(db.collection("groups").whereArrayContains("memberUids",uid).addSnapshotListener((snapshot,e)->{
   if(e!=null||snapshot==null)return;subscribeThreads("groups",snapshot);
  }));
 }
 // Separate thread subscriptions are rebuilt when membership changes. Limit to a prototype-sized collection.
 private final Map<String,List<ListenerRegistration>> threads=new HashMap<>();
 private void subscribeThreads(String collection,QuerySnapshot snapshots){
  List<ListenerRegistration> prior=threads.remove(collection);if(prior!=null){for(ListenerRegistration r:prior)r.remove();streams.removeAll(prior);}
  counts.keySet().removeIf(k->k.startsWith(collection+"/"));
  List<ListenerRegistration> next=new ArrayList<>();int max=0;
  for(DocumentSnapshot doc:snapshots.getDocuments()){
   if(++max>30)break;
   String key=collection+"/"+doc.getId();DocumentReference parent=db.collection(collection).document(doc.getId());
   final long[] seen={0};final QuerySnapshot[] messages={null};
   ListenerRegistration seenReg=parent.collection("read_receipts").document(uid).addSnapshotListener((r,e)->{
    if(e==null){Long time=r==null?null:r.getLong("lastReadAt");seen[0]=time==null?0:time;calculate(key,messages[0],seen[0]);}
   });next.add(seenReg);streams.add(seenReg);
   ListenerRegistration messageReg=parent.collection("messages").orderBy("createdAt",Query.Direction.DESCENDING).limit(50).addSnapshotListener((s,e)->{
    if(e==null){messages[0]=s;calculate(key,s,seen[0]);}
   });next.add(messageReg);streams.add(messageReg);
  }
  threads.put(collection,next);update();
 }
 private void calculate(String key,QuerySnapshot data,long lastRead){if(data==null)return;int n=0;
  for(DocumentSnapshot doc:data.getDocuments()){
   Long time=doc.getLong("createdAt");if(time!=null&&time>lastRead&&!uid.equals(doc.getString("senderUid")))n++;
  }
  counts.put(key,n);update();
 }
 private void update(){int total=0;for(Integer n:counts.values())total+=n;
  if(total>0){com.google.android.material.badge.BadgeDrawable badge=navigation.getOrCreateBadge(R.id.chatFragment);
   badge.setVisible(true);badge.setNumber(Math.min(total,99));}else navigation.removeBadge(R.id.chatFragment);
 }
 public void stop(){for(ListenerRegistration r:streams)r.remove();streams.clear();threads.clear();counts.clear();navigation.removeBadge(R.id.chatFragment);}
}
