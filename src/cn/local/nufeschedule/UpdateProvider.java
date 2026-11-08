package cn.local.nufeschedule;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Shares only the verified update APK with the system installer. */
public class UpdateProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 static File apk(Context c){return new File(c.getCacheDir(),"app-update.apk");}
 static Uri uri(Context c){return Uri.parse("content://"+c.getPackageName()+".updates/app-update.apk");}
 private void check(Uri uri){if(!uri.equals(uri(getContext())))throw new IllegalArgumentException("Unknown update file");}
 public String getType(Uri uri){check(uri);return "application/vnd.android.package-archive";}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{check(uri);if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(apk(getContext()),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){check(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] values=new Object[columns.length];for(int i=0;i<columns.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))values[i]="nufe-local.apk";else if(OpenableColumns.SIZE.equals(columns[i]))values[i]=apk(getContext()).length();}cursor.addRow(values);return cursor;}
 public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 public int update(Uri uri,ContentValues values,String s,String[] args){throw new UnsupportedOperationException();}
 public int delete(Uri uri,String s,String[] args){throw new UnsupportedOperationException();}
}
