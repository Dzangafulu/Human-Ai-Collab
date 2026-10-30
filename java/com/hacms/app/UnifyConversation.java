package com.hacms.app;

import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

/** Chat stays in memory as long as MainActivity lives; no permanent cloud chat storage. */
public final class UnifyConversation extends ViewModel {
 public static class Message {
   public final String text;public final boolean student,local;
   Message(String text,boolean student,boolean local){this.text=text;this.student=student;this.local=local;}
 }
 public final List<Message> history=new ArrayList<>();
 public UnifyConversation(){history.add(new Message("Hi! I'm Unify. Ask me to explain a subject, prioritise your work, or create a study plan.",false,false));}
 public void add(String text,boolean student,boolean local){history.add(new Message(text,student,local));}
}
