package com.hacms.app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.util.Base64;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

/** Shared design tokens and helpers. Dark mode follows the app theme. */
public final class Ui {
    private Ui() {}
    public static final int BLUE=0xff2868f3, NAVY=0xff17324f, GREEN=0xff2eb78a, ORANGE=0xfff08e26;
    public static boolean dark(Context c) { return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES; }
    public static int bg(Context c) {return dark(c)?0xff101827:0xfff5f7fc;}
    public static int card(Context c){return dark(c)?0xff202c3f:Color.WHITE;}
    public static int text(Context c){return dark(c)?0xfff7f9ff:0xff142033;}
    public static int secondary(Context c){return dark(c)?0xffafbbca:0xff61718a;}
    public static int stroke(Context c){return dark(c)?0xff42506a:0xffd8e1ee;}
    public static GradientDrawable shape(int fill,int radius,int border){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(radius);if(border!=0)g.setStroke(1,border);return g;}
    public static int dp(Context c,int n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
    public static LinearLayout vertical(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout horizontal(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public static TextView label(Context c,String t,int sp, boolean bold){TextView v=new TextView(c);v.setText(t);v.setTextSize(sp);v.setTextColor(text(c));if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    public static TextView muted(Context c,String t,int sp){TextView v=label(c,t,sp,false);v.setTextColor(secondary(c));return v;}
    public static LinearLayout panel(Context c){LinearLayout x=vertical(c);x.setPadding(dp(c,16),dp(c,15),dp(c,16),dp(c,15));x.setBackground(shape(card(c),dp(c,14),stroke(c)));return x;}
    public static LinearLayout.LayoutParams lp(Context c,int top){LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(-1,-2);x.setMargins(0,dp(c,top),0,0);return x;}
    public static TextView pill(Context c,String text,int fill,int foreground){TextView v=label(c,text,13,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(c,14),dp(c,11),dp(c,14),dp(c,11));v.setBackground(shape(fill,dp(c,12),0));v.setTextColor(foreground);return v;}
    public static Button button(Context c,String t){Button b=new Button(c);b.setText(t);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));return b;}
    public static EditText field(Context c,String hint){EditText e=new EditText(c);e.setSingleLine(true);e.setTextSize(15);e.setHint(hint);e.setTextColor(text(c));e.setHintTextColor(secondary(c));return e;}
    public static ScrollView scroll(Context c,LinearLayout content){ScrollView s=new ScrollView(c);s.setFillViewport(true);s.setBackgroundColor(bg(c));content.setPadding(dp(c,20),dp(c,20),dp(c,20),dp(c,32));s.addView(content);return s;}

    public static void open(Fragment f,int destination){if(f.isAdded())NavHostFragment.findNavController(f).navigate(destination);}

    public static LinearLayout header(Fragment f,String heading,String subtitle){return header(f,heading,subtitle,false);}
    public static LinearLayout header(Fragment f,String heading,String subtitle,boolean showBack){Context c=f.requireContext(); LinearLayout root=vertical(c);LinearLayout row=horizontal(c);LinearLayout words=vertical(c);words.addView(label(c,heading,25,true));words.addView(muted(c,subtitle,13));

        if(showBack){
            TextView back=pill(c,"←",card(c),text(c));
            back.setGravity(Gravity.CENTER);back.setTextSize(22);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(c,46),dp(c,46));
            bp.setMargins(0,0,dp(c,10),0);bp.gravity=Gravity.CENTER_VERTICAL;
            back.setOnClickListener(v->{if(f.isAdded())NavHostFragment.findNavController(f).popBackStack();});
            row.addView(back,bp);
        }

        row.addView(words,new LinearLayout.LayoutParams(0,-2,1));FrameLayout avatar=new FrameLayout(c);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(dp(c,46),dp(c,46));
        TextView initials=pill(c,"U",NAVY,Color.WHITE);initials.setGravity(Gravity.CENTER);avatar.addView(initials,new FrameLayout.LayoutParams(-1,-1));
        ImageView photo=new ImageView(c);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);photo.setVisibility(View.GONE);
        GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(NAVY);photo.setBackground(circle);photo.setClipToOutline(true);avatar.addView(photo,new FrameLayout.LayoutParams(-1,-1));
        String uid=FirebaseAuth.getInstance().getUid();if(uid!=null) FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(doc->{if(!f.isAdded())return;String n=doc.getString("name");
            if(n!=null&&!n.trim().isEmpty()){String[] nameParts=n.trim().split("\\s+");initials.setText((""+nameParts[0].charAt(0)+(nameParts.length>1?nameParts[nameParts.length-1].charAt(0):' ')).trim().toUpperCase());}
            String encoded=doc.getString("photoBase64");if(encoded!=null&&!encoded.isEmpty())try{byte[] b=Base64.decode(encoded,Base64.DEFAULT);photo.setImageBitmap(BitmapFactory.decodeByteArray(b,0,b.length));photo.setVisibility(View.VISIBLE);}catch(Exception ignored){} });
        avatar.setOnClickListener(v->open(f,R.id.profileFragment));row.addView(avatar,ap);root.addView(row);return root;}

    public static void toast(Context c,String s){Toast.makeText(c,s,Toast.LENGTH_LONG).show();}
    public static String safe(String s){return s==null?"":s;}
    public static LinearLayout options (
            Fragment f,String heading,String subtitle){Context c=f.requireContext();LinearLayout root=vertical(c);LinearLayout row=horizontal(c);LinearLayout words=vertical(c);words.addView(label(c,heading,25,true));words.addView(muted(c,subtitle,13));row.addView(words,new LinearLayout.LayoutParams(0,-2,1));FrameLayout avatar=new FrameLayout(c);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(dp(c,46),dp(c,46));
            TextView initials=pill(c,"U",NAVY,Color.WHITE);initials.setGravity(Gravity.CENTER);avatar.addView(initials,new FrameLayout.LayoutParams(-2,-2));

        return root;}
    public static void line(LinearLayout parent,Context c){View v=new View(c);v.setBackgroundColor(stroke(c));parent.addView(v,new LinearLayout.LayoutParams(-1,dp(c,1)));}
}
