package com.hacms.app;
import android.app.*;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Tasks tab: personal tasks and group work assigned to this student, persisted in Firestore. */
public class TasksFragment extends Fragment {
 private LinearLayout items,top;private String filter="All";
 private final Map<String,Task> owned=new LinkedHashMap<>(),assigned=new LinkedHashMap<>();
 private ListenerRegistration a,b;
 private final FirebaseTaskRepository repository=new FirebaseTaskRepository();
 private final List<DocumentSnapshot> groups=new ArrayList<>();
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater in,@Nullable ViewGroup par,@Nullable Bundle state){Context c=requireContext();LinearLayout root=Ui.vertical(c);root.addView(Ui.header(this,"To do","Your priorities, clearly sorted"));
 LinearLayout tabs=Ui.horizontal(c);for(String f:new String[]{"All","Urgent","This week"}){TextView v=Ui.pill(c,f,Ui.card(c),Ui.text(c));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(2,0,2,0);tabs.addView(v,lp);v.setOnClickListener(x->{filter=f;render();});}root.addView(tabs,Ui.lp(c,18));
 Button add=Ui.button(c,"+ Add task");root.addView(add,Ui.lp(c,12));add.setOnClickListener(v->editDialog(null));
 items=Ui.vertical(c);root.addView(items,Ui.lp(c,15));return Ui.scroll(c,root);}
 @Override public void onStart(){super.onStart();String uid=FirebaseAuth.getInstance().getUid();if(uid==null)return;FirebaseFirestore db=FirebaseFirestore.getInstance();
 a=db.collection("tasks").whereEqualTo("ownerUid",uid).addSnapshotListener((snap,e)->{if(e!=null){if(isAdded())Ui.toast(requireContext(),e.getMessage());return;}owned.clear();if(snap!=null)for(DocumentSnapshot d:snap.getDocuments())owned.put(d.getId(),FirebaseTaskRepository.parse(d));render();});
 b=db.collection("tasks").whereEqualTo("assigneeUid",uid).addSnapshotListener((snap,e)->{assigned.clear();if(snap!=null)for(DocumentSnapshot d:snap.getDocuments())assigned.put(d.getId(),FirebaseTaskRepository.parse(d));render();});
 db.collection("groups").whereArrayContains("memberUids",uid).get().addOnSuccessListener(snap->{groups.clear();groups.addAll(snap.getDocuments());});}
 private void render(){if(!isAdded()||items==null)return;Context c=requireContext();items.removeAllViews();Map<String,Task> all=new LinkedHashMap<>(owned);all.putAll(assigned);List<Task> list=new ArrayList<>(all.values());Collections.sort(list,(x,y)->(x.getDueDate()+x.getDueTime()).compareTo(y.getDueDate()+y.getDueTime()));int count=0;
 for(Task t:list){String status=Ui.safe(t.getStatus());if(filter.equals("Urgent")&&!t.getPriority().equalsIgnoreCase("High"))continue;
 if(filter.equals("This week")&&!near(t.getDueDate()))continue;count++;
 LinearLayout card=Ui.panel(c);LinearLayout row=Ui.horizontal(c);TextView title=Ui.label(c,t.getTitle(),16,true);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView dot=Ui.label(c,t.getPriority().equals("High")?"●":"●",18,true);dot.setTextColor(t.getPriority().equals("High")?Ui.ORANGE:Ui.BLUE);row.addView(dot);card.addView(row);
 card.addView(Ui.muted(c,(t.getGroupId().isEmpty()?"Personal task":"Group assignment")+"  •  "+t.getDueDate()+"  "+t.getDueTime(),12),Ui.lp(c,5));card.addView(Ui.muted(c,t.getDescription(),12),Ui.lp(c,5));card.addView(Ui.muted(c,"Status: "+status+"   ·   "+t.getPriority()+" priority",12),Ui.lp(c,5));
 LinearLayout actions=Ui.horizontal(c);TextView done=Ui.pill(c,status.equalsIgnoreCase("Completed")?"Completed ✓":"Mark complete",0xffe1f6ec,0xff116a4c);done.setOnClickListener(v->{t.setStatus("Completed");t.setProgressPercent(100);repository.update(t,e->{if(e!=null)Ui.toast(c,e);});});actions.addView(done);
 if(FirebaseAuth.getInstance().getUid()!=null && FirebaseAuth.getInstance().getUid().equals(t.getOwnerUid())){
 TextView edit=Ui.pill(c,"Edit",0xffe8f0ff,Ui.BLUE);edit.setOnClickListener(v->editDialog(t));LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-2,-2);ep.leftMargin=8;actions.addView(edit,ep);
 TextView del=Ui.pill(c,"Delete",0xffffe7e8,0xffa42a34);del.setOnClickListener(v->new AlertDialog.Builder(c).setTitle("Delete task?").setMessage(t.getTitle()).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->repository.delete(t,e->{if(e!=null)Ui.toast(c,e);})).show());LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-2,-2);dp.leftMargin=8;actions.addView(del,dp);}
 card.addView(actions,Ui.lp(c,12));items.addView(card,Ui.lp(c,10));}
 if(count==0)items.addView(Ui.muted(c,"No tasks here yet. Tap + Add task to create your first one.",15));}
 private boolean near(String date){try{Date d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(date);return d!=null&&d.getTime()>=System.currentTimeMillis()-86400000L&&d.getTime()<System.currentTimeMillis()+7L*86400000;}catch(Exception e){return false;}}
 private void editDialog(Task old){Context c=requireContext();LinearLayout form=Ui.vertical(c);form.setPadding(22,6,22,6);EditText title=Ui.field(c,"Task title");EditText desc=Ui.field(c,"Description");EditText date=Ui.field(c,"Due date (YYYY-MM-DD)");EditText time=Ui.field(c,"Due time (HH:mm)");date.setFocusable(false);time.setFocusable(false);
 if(old!=null){title.setText(old.getTitle());desc.setText(old.getDescription());date.setText(old.getDueDate());time.setText(old.getDueTime());}else {date.setText(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()));time.setText("23:59");}
 date.setOnClickListener(v->{Calendar now=Calendar.getInstance();new DatePickerDialog(c,(picker,y,m,d)->date.setText(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)),now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();});
 time.setOnClickListener(v->{Calendar now=Calendar.getInstance();new TimePickerDialog(c,(picker,h,m)->time.setText(String.format(Locale.US,"%02d:%02d",h,m)),now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true).show();});
 for(EditText ed:new EditText[]{title,desc,date,time})form.addView(ed);
 EditText progress=Ui.field(c,"Completion percentage (0-100)");progress.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);progress.setText(old==null?"0":String.valueOf(old.getProgressPercent()));
 form.addView(progress);
 Spinner statuses=new Spinner(c);String[] states={"To Do","In Progress","Completed"};statuses.setAdapter(new ArrayAdapter<>(c,android.R.layout.simple_spinner_dropdown_item,states));
 Spinner priority=new Spinner(c);String[] priorities={"Low","Medium","High"};priority.setAdapter(new ArrayAdapter<>(c,android.R.layout.simple_spinner_dropdown_item,priorities));form.addView(Ui.muted(c,"Status",12));form.addView(statuses);form.addView(Ui.muted(c,"Priority",12));form.addView(priority);
 if(old!=null){for(int i=0;i<states.length;i++)if(states[i].equals(old.getStatus()))statuses.setSelection(i);for(int i=0;i<priorities.length;i++)if(priorities[i].equals(old.getPriority()))priority.setSelection(i);}
 List<String> groupNames=new ArrayList<>(),groupIds=new ArrayList<>();groupNames.add("Personal task (only me)");groupIds.add("");for(DocumentSnapshot g:groups){groupNames.add(Ui.safe(g.getString("name")));groupIds.add(g.getId());}
 Spinner group=new Spinner(c);group.setAdapter(new ArrayAdapter<>(c,android.R.layout.simple_spinner_dropdown_item,groupNames));form.addView(Ui.muted(c,"Task belongs to",12),Ui.lp(c,8));form.addView(group);
 Spinner assignedTo=new Spinner(c);List<String> names=new ArrayList<>(),uids=new ArrayList<>();names.add("Myself");uids.add("");assignedTo.setAdapter(new ArrayAdapter<>(c,android.R.layout.simple_spinner_dropdown_item,names));form.addView(Ui.muted(c,"Assign to group member",12),Ui.lp(c,8));form.addView(assignedTo);
 group.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> a){}public void onItemSelected(android.widget.AdapterView<?> a,View v,int pos,long id){names.clear();uids.clear();names.add("Myself");uids.add("");assignedTo.setAdapter(new ArrayAdapter<>(c,android.R.layout.simple_spinner_dropdown_item,names));if(pos==0)return;Object members=groups.get(pos-1).get("memberUids");if(!(members instanceof List))return;for(Object member:(List<?>)members){String uid=String.valueOf(member);FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(d->{if(!isAdded())return;uids.add(uid);names.add(Ui.safe(d.getString("name")).isEmpty()?uid:d.getString("name"));assignedTo.setAdapter(new ArrayAdapter<>(c,android.R.layout.simple_spinner_dropdown_item,new ArrayList<>(names)));if(old!=null&&uid.equals(old.getAssigneeUid()))assignedTo.setSelection(uids.indexOf(uid));});}}});
 if(old!=null){int index=groupIds.indexOf(old.getGroupId());group.setSelection(index<0?0:index);group.setEnabled(false);}
 ScrollView sc=new ScrollView(c);sc.addView(form);new AlertDialog.Builder(c).setTitle(old==null?"Create Task":"Edit Task").setView(sc).setNegativeButton("Cancel",null).setPositiveButton("Save",(dialog,which)->{
 String text=title.getText().toString().trim();if(text.isEmpty()){Ui.toast(c,"Task title required");return;}
 Task t=old==null?new Task(0,text,desc.getText().toString(),"",states[statuses.getSelectedItemPosition()],priorities[priority.getSelectedItemPosition()],date.getText().toString()):old;
 t.setTitle(text);t.setDescription(desc.getText().toString());t.setStatus(states[statuses.getSelectedItemPosition()]);t.setPriority(priorities[priority.getSelectedItemPosition()]);t.setDueDate(date.getText().toString());t.setDueTime(time.getText().toString());
 try{t.setProgressPercent(Integer.parseInt(progress.getText().toString().trim()));}catch(Exception e){t.setProgressPercent(0);}
 if(t.getStatus().equals("Completed"))t.setProgressPercent(100);
 if(old==null){t.setGroupId(groupIds.get(group.getSelectedItemPosition()));String chosen=uids.get(Math.min(assignedTo.getSelectedItemPosition(),uids.size()-1));t.setAssigneeUid(chosen.isEmpty()?FirebaseAuth.getInstance().getUid():chosen);t.setAssignedTo(names.get(Math.min(assignedTo.getSelectedItemPosition(),names.size()-1)));repository.create(t,err->{if(err!=null)Ui.toast(c,err);});}
 else{
 if(!t.getGroupId().isEmpty() && assignedTo.getSelectedItemPosition()>=0 && assignedTo.getSelectedItemPosition()<uids.size()) {
 t.setAssigneeUid(uids.get(assignedTo.getSelectedItemPosition()).isEmpty()?FirebaseAuth.getInstance().getUid():uids.get(assignedTo.getSelectedItemPosition()));
 t.setAssignedTo(names.get(Math.min(assignedTo.getSelectedItemPosition(),names.size()-1)));
 }
 repository.update(t,err->{if(err!=null)Ui.toast(c,err);});}
 }).show();}
 @Override public void onStop(){super.onStop();if(a!=null)a.remove();if(b!=null)b.remove();}
}
