package com.hacms.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

/** Locally cached schedule, enabling the dashboard and timer to advance without a new Firestore event. */
public final class StudySessionTimeline {
    private StudySessionTimeline() { }
    private static String key() {
        String uid = FirebaseAuth.getInstance().getUid();
        return "sessions_" + (uid == null ? "signed_out" : uid);
    }
    public static final class ActiveSession {
        public final String id, title;
        public final long from, until;
        public final boolean focus;
        ActiveSession(String id, String title, long from, long until, boolean focus) {
            this.id = id; this.title = title; this.from = from; this.until = until; this.focus = focus;
        }
    }
    public static void sync(Context context, List<DocumentSnapshot> docs) {
        JSONArray items = new JSONArray();
        long now = System.currentTimeMillis();
        for (DocumentSnapshot d : docs) {
            long start = StudySessionStore.startMillis(d);
            Long duration = d.getLong("durationMinutes");
            long length = duration == null || duration <= 0 ? 60 : duration;
            long end = start + length * 60_000L;
            if (start <= 0 || end <= now) continue;
            String name = d.getString("taskTitle");
            if (name == null || name.trim().isEmpty()) name = "Study session";
            try {
                JSONObject x = new JSONObject();
                x.put("id", d.getId()); x.put("title", name);
                x.put("from", start); x.put("until", end);
                x.put("focus", Boolean.TRUE.equals(d.getBoolean("focusModeEnabled")));
                items.put(x);
            } catch (Exception ignored) { }
        }
        context.getApplicationContext().getSharedPreferences("unitask_study_timeline", 0)
                .edit().putString(key(), items.toString()).apply();
    }
    public static ActiveSession active(Context context) {
        if (FirebaseAuth.getInstance().getUid() == null) return null;
        long now = System.currentTimeMillis();
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences("unitask_study_timeline", 0);
        try {
            JSONArray rows = new JSONArray(preferences.getString(key(), "[]"));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject x = rows.getJSONObject(i);
                long start = x.optLong("from"), end = x.optLong("until");
                if (start <= now && now < end) {
                    return new ActiveSession(x.optString("id"),
                            x.optString("title", "Study session"), start, end, x.optBoolean("focus"));
                }
            }
        } catch (Exception ignored) { }
        return null;
    }
}
