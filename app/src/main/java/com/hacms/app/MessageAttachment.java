package com.hacms.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.DocumentSnapshot;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/** Small Firestore-backed attachments ONLY: no Cloud Storage or paid backend required. */
public final class MessageAttachment {
 private MessageAttachment() {}
 public static final int MAX_BYTES=250*1024;
 public interface Ready { void ready(Map<String,Object> fields); void error(String why); }
 public static Intent choose(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);
  i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/jpeg","image/png","image/webp","application/pdf","text/plain","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document"});return i;}
 public static void prepare(Context c,Uri uri,Ready cb){
  if(uri==null){cb.error("No file selected");return;}
  String mime=c.getContentResolver().getType(uri);if(mime==null)mime="application/octet-stream";
  if(!(mime.startsWith("image/")||mime.equals("application/pdf")||mime.equals("text/plain")||mime.equals("application/msword")||mime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))){cb.error("Supported: JPEG/PNG/WebP, PDF, TXT, DOC, DOCX");return;}
  String name="attachment";try(Cursor cursor=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
   if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);
  }catch(Exception ignored){}
  final String type=mime;final String filename=name.length()>90?name.substring(name.length()-90):name;
  try(InputStream input=c.getContentResolver().openInputStream(uri)){
   if(input==null){cb.error("Could not read file");return;}
   ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int total=0,r;
   if(type.startsWith("image/")){
     out.write(compressImage(c.getContentResolver(),uri));
   }else{
     while((r=input.read(buffer))!=-1){total+=r;if(total>MAX_BYTES){cb.error("Documents must be at most 250 KB on this free-tier prototype.");return;}out.write(buffer,0,r);}
   }
   Map<String,Object> m=new HashMap<>();m.put("attachmentName",type.startsWith("image/")?filename+".jpg":filename);m.put("attachmentMime",type.startsWith("image/")?"image/jpeg":type);m.put("attachmentBase64",Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP));
   m.put("text","📎 "+filename);cb.ready(m);
  }catch(Exception ex){cb.error("Could not attach file: "+ex.getMessage());}
 }
 private static byte[] compressImage(ContentResolver resolver,Uri uri) throws java.io.IOException {
   BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
   try(InputStream is=resolver.openInputStream(uri)){BitmapFactory.decodeStream(is,null,bounds);}
   int sample=1;while(bounds.outWidth/sample>1280||bounds.outHeight/sample>1280)sample*=2;
   BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=sample;
   Bitmap image;try(InputStream is=resolver.openInputStream(uri)){image=BitmapFactory.decodeStream(is,null,opts);}
   if(image==null)throw new java.io.IOException("Cannot decode selected image");
   try{for(int quality=85;quality>=30;quality-=10){ByteArrayOutputStream out=new ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.JPEG,quality,out);if(out.size()<=MAX_BYTES)return out.toByteArray();}
    throw new java.io.IOException("The selected image is too large even after compression");
   }finally{image.recycle();}
 }
 public static void render(Fragment fragment,LinearLayout panel,DocumentSnapshot doc,ActivityResultLauncher<Intent> saveAs){
  String encoded=doc.getString("attachmentBase64");if(encoded==null||encoded.isEmpty())return;
  Context c=fragment.requireContext();String name=doc.getString("attachmentName");String mime=doc.getString("attachmentMime");if(name==null)name="attachment";
  if(mime!=null&&mime.startsWith("image/")){
   try{byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);
    BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=2;Bitmap bmp=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
    ImageView preview=new ImageView(c);preview.setImageBitmap(bmp);preview.setAdjustViewBounds(true);preview.setMaxHeight(Ui.dp(c,180));panel.addView(preview,Ui.lp(c,8));
   }catch(Exception ignored){}
  }
  Button download=Ui.button(c,"Save "+name);final String outputName=name;
  download.setOnClickListener(v->{AttachmentSaveState.encoded=encoded;Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.setType(mime==null?"application/octet-stream":mime);
    intent.addCategory(Intent.CATEGORY_OPENABLE);intent.putExtra(Intent.EXTRA_TITLE,outputName);saveAs.launch(intent);});
  panel.addView(download,Ui.lp(c,5));
 }
 /** A single pending save per app process is sufficient for a student prototype. */
 private static final class AttachmentSaveState {private static String encoded;}
 public static void saveSelected(Context c,Uri target){if(target==null||AttachmentSaveState.encoded==null)return;
  try(OutputStream output=c.getContentResolver().openOutputStream(target)){
   if(output!=null){output.write(Base64.decode(AttachmentSaveState.encoded,Base64.DEFAULT));Ui.toast(c,"Attachment saved");}
  }catch(Exception e){Ui.toast(c,"Save failed: "+e.getMessage());}
  finally{AttachmentSaveState.encoded=null;}
 }
}
