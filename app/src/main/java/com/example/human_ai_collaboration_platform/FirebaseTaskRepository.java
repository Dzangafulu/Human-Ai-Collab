package com.example.human_ai_collaboration_platform;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** Bridge from the teammate's original Task model to the shared Firestore tasks collection. */
public class FirebaseTaskRepository {
    public interface OnTasks { void onResult(List<Task> tasks); void onError(String message); }
    public interface OnDone { void finish(String errorOrNull); }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid() { return FirebaseAuth.getInstance().getUid(); }

    public ListenerRegistration listen(OnTasks callback) {
        if (uid() == null) { callback.onError("Please sign in first."); return null; }
        return db.collection("tasks").whereEqualTo("ownerUid", uid())
            .addSnapshotListener((snap, error) -> {
                if (error != null) { callback.onError(error.getMessage()); return; }
                List<Task> tasks = new ArrayList<>();
                if (snap != null) {
                    int i = 1;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Task task = readTask(d, i++);
                        if (task != null) tasks.add(task);
                    }
                }
                callback.onResult(tasks);
            });
    }

    public void loadOnce(OnTasks callback) {
        if (uid() == null) { callback.onError("Please sign in first."); return; }
        db.collection("tasks").whereEqualTo("ownerUid", uid()).get()
            .addOnSuccessListener(snap -> {
                List<Task> tasks = new ArrayList<>(); int i = 1;
                for (DocumentSnapshot d : snap.getDocuments()) {
                    Task task = readTask(d, i++);
                    if (task != null) tasks.add(task);
                }
                callback.onResult(tasks);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private Task readTask(DocumentSnapshot d, int i) {
        Task task = new Task(i,
            getString(d, "title", "Untitled task"),
            getString(d, "description", ""),
            getString(d, "assignedTo", "Not assigned"),
            getString(d, "status", "To Do"),
            getString(d, "priority", "Medium"),
            getString(d, "dueDate", "No due date"));
        task.setDocumentId(d.getId());
        return task;
    }
    private String getString(DocumentSnapshot d, String name, String fallback) {
        String v = d.getString(name); return v == null ? fallback : v;
    }
    private Map<String, Object> values(Task task) {
        Map<String, Object> values = new HashMap<>();
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());
        values.put("assignedTo", task.getAssignedTo());
        values.put("status", task.getStatus());
        values.put("priority", task.getPriority());
        values.put("dueDate", task.getDueDate());
        values.put("ownerUid", uid());
        return values;
    }
    public void create(Task task, OnDone done) {
        if (uid() == null) { done.finish("Please sign in first."); return; }
        Map<String, Object> fields = values(task);
        fields.put("createdAt", System.currentTimeMillis());
        db.collection("tasks").add(fields)
            .addOnSuccessListener(ref -> { task.setDocumentId(ref.getId()); done.finish(null); })
            .addOnFailureListener(e -> done.finish(e.getMessage()));
    }
    public void update(Task task, OnDone done) {
        if (uid() == null || task.getDocumentId() == null) { done.finish("Task is not saved."); return; }
        db.collection("tasks").document(task.getDocumentId()).update(values(task))
            .addOnSuccessListener(x -> done.finish(null))
            .addOnFailureListener(e -> done.finish(e.getMessage()));
    }
    public void delete(Task task, OnDone done) {
        if (uid() == null || task.getDocumentId() == null) { done.finish("Task is not saved."); return; }
        db.collection("tasks").document(task.getDocumentId()).delete()
            .addOnSuccessListener(x -> done.finish(null))
            .addOnFailureListener(e -> done.finish(e.getMessage()));
    }
}
