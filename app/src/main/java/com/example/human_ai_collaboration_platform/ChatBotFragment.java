package com.example.human_ai_collaboration_platform;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Existing teammate chat UI; Gemini AI Logic added with local study fallback. */
public class ChatBotFragment extends Fragment {
    private static final String USER_KEY = "user";
    private static final String BOT_KEY = "bot";
    private static final long MIN_INTERVAL_MS = 12000L; // simple per-device throttle, NOT shared quota management
    private final ArrayList<ChatBotModel> messages = new ArrayList<>();
    private ChatBotAdapter adapter;
    private RecyclerView recyclerView;
    private FloatingActionButton sendButton;
    private EditText input;
    private TextView status;
    private CheckBox shareTasks;
    private boolean sending = false;
    private long lastRequest = -MIN_INTERVAL_MS;
    private GeminiAiClient ai;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_chat_bot, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        recyclerView = view.findViewById(R.id.recyclerView);
        sendButton = view.findViewById(R.id.sendButton);
        input = view.findViewById(R.id.messageEditText);
        status = view.findViewById(R.id.aiStatus);
        shareTasks = view.findViewById(R.id.aiShareTasks);
        adapter = new ChatBotAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        SharedPreferences preferences = prefs();
        shareTasks.setChecked(preferences.getBoolean("share_task_context", false));
        shareTasks.setOnCheckedChangeListener((button, checked) ->
                prefs().edit().putBoolean("share_task_context", checked).apply());
        status.setText("Gemini online mode • No tasks shared unless selected");
        view.findViewById(R.id.aiLocalModeButton).setOnClickListener(v -> {
            if (sending) return;
            String question = input.getText().toString().trim();
            if (question.isEmpty()) question = "What should I focus on today?";
            input.setText("");
            addMessage(question, USER_KEY);
            sending = true;
            sendButton.setEnabled(false);
            status.setText("Local study mode • No Gemini request sent");
            showFallback(question, new IllegalStateException("Local mode chosen by user"));
        });
        sendButton.setOnClickListener(v -> {
            String question = input.getText().toString().trim();
            if (question.isEmpty()) { Toast.makeText(requireContext(), "Enter a question", Toast.LENGTH_SHORT).show(); return; }
            if (sending) return;
            long waitMs = MIN_INTERVAL_MS - (SystemClock.elapsedRealtime() - lastRequest);
            if (waitMs > 0) {
                status.setText("Please wait " + ((waitMs + 999)/1000) + " seconds before another AI request.");
                return;
            }
            if (!preferences.getBoolean("gemini_consent", false)) {
                new AlertDialog.Builder(requireContext())
                    .setTitle("Use Gemini AI?")
                    .setMessage("Your messages will be sent to Google's Gemini service for AI responses. " +
                        "On the free tier, Google may use submitted content to improve its services. " +
                        "Do not include private or sensitive information. Task titles are sent only when you select the checkbox. Continue?")
                    .setNegativeButton("Cancel", (d,w) -> { })
                    .setPositiveButton("I agree", (d,w) -> {
                        prefs().edit().putBoolean("gemini_consent", true).apply();
                        sendQuestion(question);
                    }).show();
            } else { sendQuestion(question); }
        });
    }

    private SharedPreferences prefs() {
        String uid = FirebaseAuth.getInstance().getUid();
        return requireContext().getSharedPreferences("gemini_" + (uid == null ? "guest" : uid), Context.MODE_PRIVATE);
    }
    private void addMessage(String text, String sender) {
        if (!isAdded() || recyclerView == null) return;
        messages.add(new ChatBotModel(text, sender));
        adapter.notifyItemInserted(messages.size()-1);
        recyclerView.scrollToPosition(messages.size()-1);
    }
    private void sendQuestion(String question) {
        sending = true;
        sendButton.setEnabled(false);
        input.setText("");
        addMessage(question, USER_KEY);
        status.setText("Gemini is thinking…");
        lastRequest = SystemClock.elapsedRealtime();
        boolean includeTasks = shareTasks.isChecked();
        if (!includeTasks) {
            askGemini(question, Collections.emptyList(), false);
        } else {
            new FirebaseTaskRepository().loadOnce(new FirebaseTaskRepository.OnTasks() {
                @Override public void onResult(List<Task> tasks) { if (isAdded()) askGemini(question, tasks, true); }
                @Override public void onError(String message) {
                    if (isAdded()) { status.setText("Task data unavailable; asking Gemini without it…");
                        askGemini(question, Collections.emptyList(), false); }
                }
            });
        }
    }
    private void askGemini(String question, List<Task> tasks, boolean includedTasks) {
        if (!isAdded()) return;
        final String prompt = buildPrompt(question, tasks, includedTasks);
        try {
            if (ai == null) ai = new GeminiAiClient();
            ai.ask(prompt, ContextCompat.getMainExecutor(requireContext()), new GeminiAiClient.Callback() {
                @Override public void success(@NonNull String answer) {
                    if (!isAdded()) return;
                    addMessage(answer, BOT_KEY);
                    status.setText("Gemini response • " + GeminiAiClient.MODEL);
                    finishRequest();
                }
                @Override public void failure(@NonNull Throwable error) {
                    if (!isAdded()) return;
                    String summary = error.getMessage() == null ? "connection error" : error.getMessage().toLowerCase(Locale.ROOT);
                    // Quota, App Check, model errors and network errors all fail over without hiding the cause.
                    status.setText(summary.contains("429") || summary.contains("quota") || summary.contains("exhaust")
                            ? "Gemini quota reached • Local study mode" : "Gemini unavailable • Local study mode");
                    showFallback(question, error);
                }
            });
        } catch (Exception error) {
            status.setText("Gemini could not start • Local study mode");
            showFallback(question, error);
        }
    }
    private void showFallback(String question, Throwable error) {
        new FirebaseTaskRepository().loadOnce(new FirebaseTaskRepository.OnTasks() {
            @Override public void onResult(List<Task> tasks) {
                if (!isAdded()) return;
                addMessage(LocalStudyAssistant.recommendation(question, tasks), BOT_KEY);
                finishRequest();
            }
            @Override public void onError(String message) {
                if (!isAdded()) return;
                addMessage(LocalStudyAssistant.recommendation(question, Collections.emptyList())
                        + "\n\nTask database currently unavailable.", BOT_KEY);
                finishRequest();
            }
        });
    }
    private void finishRequest() {
        sending = false;
        if (sendButton != null) sendButton.setEnabled(true);
    }
    private String buildPrompt(String question, List<Task> tasks, boolean includeTasks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a concise, constructive study assistant for a university student. ")
              .append("Explain concepts, help plan tasks, break assignments into manageable steps, ")
              .append("and offer realistic time-management advice. Do not claim to have executed actions ")
              .append("or modified task data. Do not invent deadlines. Suggest plans, but the student decides. ")
              .append("Never ask for passwords or sensitive information.\n");
        if (includeTasks) {
            prompt.append("User opted to share the following unfinished tasks (use them only as context):\n");
            int n = 0;
            for (Task task : StudyPlanEngine.ranked(tasks)) {
                if (n++ >= 5) break;
                prompt.append("- ").append(clean(task.getTitle(), 75))
                      .append("; due ").append(clean(task.getDueDate(), 22))
                      .append("; priority ").append(clean(task.getPriority(), 12)).append("\n");
            }
            if (n == 0) prompt.append("No unfinished tasks recorded.\n");
        } else {
            prompt.append("No access to the user's saved tasks. Do not claim you can see their tasks.\n");
        }
        prompt.append("Recent conversation (for context):\n");
        int from = Math.max(0, messages.size()-5);
        for (int i=from; i<messages.size()-1; i++) {
            ChatBotModel m = messages.get(i);
            prompt.append(m.getSender().equals(USER_KEY) ? "Student: " : "Assistant: ")
                  .append(clean(m.getMessage(), 250)).append("\n");
        }
        prompt.append("Student: ").append(clean(question, 1000)).append("\nAssistant:");
        return prompt.toString();
    }
    private String clean(String value, int maxChars) {
        if (value == null) return "";
        String oneLine = value.replace('\n', ' ').replace('\r',' ').trim();
        return oneLine.length()>maxChars ? oneLine.substring(0,maxChars) : oneLine;
    }
}
