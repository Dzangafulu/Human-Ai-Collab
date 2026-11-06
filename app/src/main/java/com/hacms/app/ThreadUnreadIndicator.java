package com.hacms.app;

import android.view.View;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

/** Show a blue unread count on an individual connection/group row. Live only while row is attached. */
public final class ThreadUnreadIndicator {
 private ThreadUnreadIndicator(){}
 public static void bind(TextView row,DocumentReference thread,String label){
  row.setText(label);
  row.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
   ListenerRegistration messagesListener,receiptListener;QuerySnapshot messages;long seen;
   final String user=FirebaseAuth.getInstance().getUid();
   private void draw(){if(user==null)return;int unread=0;
     if(messages!=null)for(DocumentSnapshot doc:messages.getDocuments()){
       Long time=doc.getLong("createdAt");if(time!=null&&time>seen&&!user.equals(doc.getString("senderUid")))unread++;
     }
     row.setText(unread>0?label+"  ·  "+unread+" new":label);
   }
   @Override public void onViewAttachedToWindow(View view){if(user==null)return;
    receiptListener=thread.collection("read_receipts").document(user).addSnapshotListener((doc,e)->{if(e==null){Long when=doc==null?null:doc.getLong("lastReadAt");seen=when==null?0:when;draw();}});
    messagesListener=thread.collection("messages").orderBy("createdAt",Query.Direction.DESCENDING).limit(50).addSnapshotListener((snap,e)->{if(e==null){messages=snap;draw();}});
   }
   @Override public void onViewDetachedFromWindow(View view){if(messagesListener!=null){messagesListener.remove();messagesListener=null;}if(receiptListener!=null){receiptListener.remove();receiptListener=null;}}
  });
 }
}
