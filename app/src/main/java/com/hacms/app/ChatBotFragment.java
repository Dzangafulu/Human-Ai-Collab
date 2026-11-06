package com.hacms.app;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;

/** Unify: real Gemini when consent and quota permit, local priority recommendations otherwise. */
public class ChatBotFragment extends Fragment {
 private LinearLayout timeline;
 private UnifyConversation conversation;
 private final List<Task> tasks=new ArrayList<>();
 private final FirebaseTaskRepository repo=new FirebaseTaskRepository();
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){Context c=requireContext();LinearLayout screen=Ui.vertical(c);screen.setBackgroundColor(Ui.bg(c));
 LinearLayout head=Ui.header(this,"Unify","Plan smarter, stay focused");head.setPadding(20,16,20,8);screen.addView(head);
 timeline=Ui.vertical(c);timeline.setPadding(15,8,15,15);ScrollView area=Ui.scroll(c,timeline);screen.addView(area,new LinearLayout.LayoutParams(-1,0,1));
 conversation=new ViewModelProvider(requireActivity()).get(UnifyConversation.class);
 for(UnifyConversation.Message m:conversation.history)drawBubble(m.text,m.student,m.local);
 TextView fallback=Ui.pill(c,"Use local study recommendations",0xffe7f7f0,0xff146d54);fallback.setOnClickListener(v->answerLocal("What should I focus on today?"));screen.addView(fallback);
 LinearLayout composer=Ui.horizontal(c);composer.setPadding(9,8,9,14);EditText input=Ui.field(c,"Ask Unify anything…");composer.addView(input,new LinearLayout.LayoutParams(0,-2,1));Button send=Ui.button(c,"➤");composer.addView(send);screen.addView(composer);
 send.setOnClickListener(v->{String question=input.getText().toString().trim();if(question.isEmpty())return;input.setText("");bubble(question,true,false);ask(question);});return screen;}
 @Override public void onStart(){super.onStart();repo.loadOnce(new FirebaseTaskRepository.OnTasks(){public void onResult(List<Task> result){tasks.clear();tasks.addAll(result);repo.loadAssigned(new FirebaseTaskRepository.OnTasks(){public void onResult(List<Task> other){for(Task t:other)if(tasks.stream().noneMatch(x->x.getDocumentId()!=null&&x.getDocumentId().equals(t.getDocumentId())))tasks.add(t);}public void onError(String e){} });}public void onError(String e){}});}
 private void bubble(String s,boolean mine,boolean fallback){
   if(conversation!=null)conversation.add(s,mine,fallback);
   if(isAdded()&&timeline!=null)drawBubble(s,mine,fallback);
 }
 private void drawBubble(String s,boolean mine,boolean fallback){if(!isAdded()||timeline==null)return;Context c=requireContext();LinearLayout panel=Ui.panel(c);panel.setBackground(Ui.shape(mine?0xffdce9ff:Ui.card(c),14,0));TextView heading=Ui.label(c,mine?"You":fallback?"Local planner":"Unify",12,true);heading.setTextColor(Ui.BLUE);panel.addView(heading);TextView message=Ui.label(c,s,14,false);if(mine)message.setTextColor(Ui.NAVY);panel.addView(message,Ui.lp(c,6));timeline.addView(panel,Ui.lp(c,12));}
 private void answerLocal(String prompt){bubble(LocalStudyAssistant.recommendation(prompt,tasks),false,true);}
 private void ask(String question){
  String q=question.trim().toLowerCase(Locale.ROOT);
  boolean requestedSchedule=q.startsWith("schedule my") || q.startsWith("create study sessions") ||
      q.startsWith("make study sessions") || q.startsWith("set up study sessions") || q.startsWith("generate study sessions");
  if(requestedSchedule){createRequestedStudySessions(question);return;}
  Context c=requireContext();if(!c.getSharedPreferences("unitask",0).getBoolean("aiConsent",false)){new AlertDialog.Builder(c).setTitle("Allow Gemini to process this message?").setMessage("Your question will be sent to Google's Gemini API. Task titles are only included if you separately enable task sharing in Settings. You can change this choice later.").setNegativeButton("Use local planning",(d,w)->answerLocal(question)).setPositiveButton("Allow Gemini",(d,w)->{c.getSharedPreferences("unitask",0).edit().putBoolean("aiConsent",true).apply();sendGemini(question);}).show();return;}sendGemini(question);}
 private void createRequestedStudySessions(String question){
    if(tasks.isEmpty()){bubble("Please add an unfinished assignment or test first, then try again.",false,true);return;}
    List<Task> matching=new ArrayList<>();String words=question.toLowerCase(Locale.ROOT);
    for(Task t:tasks){
      if(words.contains(t.getTitle().toLowerCase(Locale.ROOT)) ||
         (words.contains("test") && t.getTitle().toLowerCase(Locale.ROOT).contains("test")))matching.add(t);
    }
    if(matching.isEmpty())matching.addAll(tasks);
    List<StudyPlanEngine.Session> plan=StudyPlanEngine.generate(matching,1,18,0);
    if(plan.isEmpty()){bubble("No future sessions could be planned for those tasks.",false,true);return;}
    StudySessionStore.saveGenerated(requireContext(),plan,error->{
      if(!isAdded())return;
      bubble(error==null?"I created and saved "+plan.size()+" study sessions for your tasks. View them on the Home dashboard or in the Study Scheduler.":"I couldn't save the sessions: "+error,false,error!=null);
    });
 }
 private void sendGemini(String question){Context c=requireContext();StringBuilder prompt=new StringBuilder("You are Unify, a practical, supportive student study assistant for UniTask. Give precise, actionable, concise advice. Do not claim to have performed actions in the app. Student question: "+question);
 if(c.getSharedPreferences("unitask",0).getBoolean("shareTasks",false)){prompt.append("\nTasks shared with consent:\n");for(int k=0;k<Math.min(12,tasks.size());k++){Task t=tasks.get(k);prompt.append(t.getTitle()).append(" (due ").append(t.getDueDate()).append(' ').append(t.getDueTime()).append(", priority ").append(t.getPriority()).append(")\n");}}
 new GeminiAiClient().ask(prompt.toString(),androidx.core.content.ContextCompat.getMainExecutor(c),new GeminiAiClient.Callback(){@Override public void success(String answer){bubble(answer,false,false);}@Override public void failure(Throwable e){bubble("Gemini is unavailable (quota, connectivity, App Check or model access). Showing built-in recommendations instead.",false,true);answerLocal(question);}});}
}
