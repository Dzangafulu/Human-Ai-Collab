package com.hacms.app;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/** Firestore-backed personal and assigned tasks. Each document has an ownerUid and optional groupId/assigneeUid. */
public class FirebaseTaskRepository {
 public interface OnTasks {void onResult(List<Task> data); void onError(String error);}
 public interface OnDone {void finish(String error);}
 private final FirebaseFirestore db=FirebaseFirestore.getInstance();
 private String uid(){return FirebaseAuth.getInstance().getUid();}
 public static Task parse(DocumentSnapshot d){Task t=new Task(0, str(d,"title","Untitled"),str(d,"description",""),str(d,"assignedTo",""),str(d,"status","To Do"),str(d,"priority","Medium"),str(d,"dueDate",""));
 Object progress=d.get("progressPercent");if(progress instanceof Number)t.setProgressPercent(((Number)progress).intValue());
 t.setDocumentId(d.getId());t.setGroupId(str(d,"groupId",""));t.setOwnerUid(str(d,"ownerUid",""));t.setAssigneeUid(str(d,"assigneeUid",""));t.setDueTime(str(d,"dueTime","23:59"));return t;}
 private static String str(DocumentSnapshot d,String key,String fallback){String s=d.getString(key);return s==null?fallback:s;}
 public ListenerRegistration listen(OnTasks cb){if(uid()==null){cb.onError("Sign in first");return null;}return db.collection("tasks").whereEqualTo("ownerUid",uid()).addSnapshotListener((snap,e)->{if(e!=null){cb.onError(e.getMessage());return;}List<Task> l=new ArrayList<>();if(snap!=null)for(DocumentSnapshot d:snap.getDocuments())l.add(parse(d));cb.onResult(l);});}
 public void loadOnce(OnTasks cb){if(uid()==null){cb.onError("Sign in first");return;}db.collection("tasks").whereEqualTo("ownerUid",uid()).get().addOnSuccessListener(s->{List<Task> out=new ArrayList<>();for(DocumentSnapshot d:s.getDocuments())out.add(parse(d));cb.onResult(out);}).addOnFailureListener(e->cb.onError(e.getMessage()));}
 public void loadAssigned(OnTasks cb){if(uid()==null){cb.onError("Sign in first");return;}db.collection("tasks").whereEqualTo("assigneeUid",uid()).get().addOnSuccessListener(s->{List<Task> out=new ArrayList<>();for(DocumentSnapshot d:s.getDocuments())out.add(parse(d));cb.onResult(out);}).addOnFailureListener(e->cb.onError(e.getMessage()));}
 private Map<String,Object> values(Task t){Map<String,Object> m=new HashMap<>();m.put("progressPercent",t.getProgressPercent());m.put("title",t.getTitle());m.put("description",t.getDescription());m.put("assignedTo",t.getAssignedTo());m.put("status",t.getStatus());m.put("priority",t.getPriority());m.put("dueDate",t.getDueDate());m.put("dueTime",t.getDueTime());m.put("ownerUid",t.getOwnerUid().isEmpty()?uid():t.getOwnerUid());m.put("groupId",t.getGroupId());m.put("assigneeUid",t.getAssigneeUid());return m;}
 public void create(Task t,OnDone done){if(uid()==null){done.finish("Sign in first");return;}Map<String,Object> map=values(t);map.put("createdAt",System.currentTimeMillis());db.collection("tasks").add(map).addOnSuccessListener(ref->{t.setDocumentId(ref.getId());done.finish(null);}).addOnFailureListener(e->done.finish(e.getMessage()));}
 public void update(Task t,OnDone done){if(t.getDocumentId()==null){done.finish("Task not yet saved");return;}Map<String,Object> fields=values(t);fields.remove("ownerUid");fields.remove("groupId");db.collection("tasks").document(t.getDocumentId()).update(fields).addOnSuccessListener(a->done.finish(null)).addOnFailureListener(e->done.finish(e.getMessage()));}
 public void delete(Task t,OnDone done){if(t.getDocumentId()==null){done.finish("Task not saved");return;}db.collection("tasks").document(t.getDocumentId()).delete().addOnSuccessListener(a->{ReminderScheduler.cancelTask(com.google.firebase.FirebaseApp.getInstance().getApplicationContext(),uid(),t.getDocumentId());done.finish(null);}).addOnFailureListener(e->done.finish(e.getMessage()));}
}
