package cn.local.nufeschedule;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Public GitHub Releases updater. No account, token, or timetable is sent. */
public final class AppUpdate {
 private final Activity activity;
 private static volatile boolean busy;
 private static final long LIMIT=100L*1024*1024;
 public AppUpdate(Activity a){activity=a;}
 private SharedPreferences prefs(){return activity.getSharedPreferences("app_updates",0);}
 public static String version(Context c){try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}catch(Exception e){return "未知";}}
 private boolean alive(){return !activity.isFinishing()&&!activity.isDestroyed();}
 private void ui(Runnable action){activity.runOnUiThread(()->{if(alive())action.run();});}
 private void message(String text){if(alive())new AlertDialog.Builder(activity).setMessage(text).setPositiveButton("知道了",null).show();}
 private static final String REPOSITORY="haoranbai314-spec/nufe-local-schedule";
 public void check(){check(false);}
 public void checkAutomatically(){check(true);}
 private static HttpURLConnection connect(String address)throws Exception{URL url=new URL(address);for(int redirects=0;redirects<6;redirects++){if(!"https".equalsIgnoreCase(url.getProtocol())||url.getUserInfo()!=null)throw new IOException("更新只支持 HTTPS 地址");HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","NufeLocal-Updater");int status=c.getResponseCode();if(status==301||status==302||status==303||status==307||status==308){String next=c.getHeaderField("Location");c.disconnect();if(next==null)throw new IOException("下载跳转地址缺失");url=new URL(url,next);continue;}if(status!=200){c.disconnect();throw new IOException(status==404?"没有找到正式发布版本，请检查仓库及 Releases":status==403||status==429?"请求过于频繁，请稍后重试":"服务器返回 "+status);}return c;}throw new IOException("下载跳转次数过多");}
 private static byte[] read(InputStream input,long limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("更新信息过大");out.write(buffer,0,n);}return out.toByteArray();}
 private static int compare(String a,String b)throws IOException{String[] x=a.replaceFirst("^[vV]","").split("\\."),y=b.replaceFirst("^[vV]","").split("\\.");if(!a.matches("[vV]?[0-9]+(\\.[0-9]+){1,3}")||!b.matches("[vV]?[0-9]+(\\.[0-9]+){1,3}"))throw new IOException("版本号需使用 v0.19.0 这样的格式");for(int i=0;i<Math.max(x.length,y.length);i++){long left=i<x.length?Long.parseLong(x[i]):0,right=i<y.length?Long.parseLong(y[i]):0;if(left!=right)return Long.compare(left,right);}return 0;}
 private void check(boolean automatic){String repo=REPOSITORY;if(busy){if(!automatic)message("正在检查或下载，请稍候。");return;}busy=true;ProgressDialog progress=automatic?null:ProgressDialog.show(activity,"检查更新","正在连接…",true,false);new Thread(()->{JSONObject release=null;Exception failure=null;try{HttpURLConnection c=connect("https://api.github.com/repos/"+repo+"/releases/latest");try(InputStream in=c.getInputStream()){release=new JSONObject(new String(read(in,1024*1024),java.nio.charset.StandardCharsets.UTF_8));}finally{c.disconnect();}}catch(Exception e){failure=e;}JSONObject result=release;Exception error=failure;busy=false;ui(()->{if(progress!=null)progress.dismiss();if(error!=null){if(!automatic)message("检查失败："+error.getMessage()+"\n请检查网络连接；部分网络可能无法连接 GitHub。");return;}try{String tag=result.getString("tag_name");if(compare(tag,version(activity))<=0){if(!automatic)message("当前已是最新正式版本（"+version(activity)+"）。");return;}JSONArray assets=result.getJSONArray("assets");JSONObject apk=null;for(int i=0;i<assets.length();i++){JSONObject item=assets.getJSONObject(i);if("nufe-local.apk".equals(item.optString("name"))){apk=item;break;}}if(apk==null)throw new IOException("该版本未附带 nufe-local.apk 安装包");String url=apk.getString("browser_download_url");if(!url.startsWith("https://github.com/"+repo+"/releases/download/"))throw new IOException("安装包地址不属于当前发布仓库");long size=apk.optLong("size",0);if(size<=0||size>LIMIT)throw new IOException("安装包大小异常");String notes=result.optString("body","");if(notes.length()>5000)notes=notes.substring(0,5000);String download=url;new AlertDialog.Builder(activity).setTitle("发现新版 "+tag).setMessage("当前版本 "+version(activity)+"\n\n"+(notes.isEmpty()?"发布者未填写更新说明。":notes)+"\n\n下载后需要在系统窗口确认安装，课表会保留。").setNegativeButton("暂不更新",null).setPositiveButton("下载并安装",(d,w)->download(download,size,tag)).show();}catch(Exception e){if(!automatic)message("无法更新："+e.getMessage());}});} ,"check-app-update").start();}
 private final class DownloadProgress {
  final ProgressDialog dialog;long last=-1;
  DownloadProgress(){dialog=new ProgressDialog(activity);dialog.setTitle("下载新版");dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);dialog.setMax(100);dialog.setIndeterminate(true);dialog.setMessage("正在连接下载服务器…");dialog.setCancelable(false);dialog.show();}
  void stage(String text){last=-1;ui(()->{dialog.setIndeterminate(true);dialog.setProgress(0);dialog.setMessage(text);});}
  void bytes(long received,long total){long now=android.os.SystemClock.elapsedRealtime();if(last>=0&&now-last<120&&received!=total)return;last=now;int percent=UpdateTransfer.percent(received,total);String amount=String.format(java.util.Locale.CHINA,"已下载 %.1f KB",received/1024.0);if(total>0)amount+=String.format(java.util.Locale.CHINA," / %.1f KB",total/1024.0);String text=amount;ui(()->{dialog.setIndeterminate(percent<0);if(percent>=0)dialog.setProgress(percent);dialog.setMessage(text);});}
 }
 private void download(String url,long expected,String tag){
  if(busy)return;busy=true;DownloadProgress progress=new DownloadProgress();
  new Thread(()->{
   File part=new File(activity.getCacheDir(),"app-update.part");Exception failure=null;
   try{
    try{
     HttpURLConnection connection=connect(url);
     try(InputStream input=connection.getInputStream();OutputStream output=new FileOutputStream(part)){
      progress.stage("主通道已连接，正在下载…");
      UpdateTransfer.copy(input,output,expected,LIMIT,progress::bytes);
     }finally{connection.disconnect();}
    }catch(IOException primary){
     progress.stage("主通道连接失败，正在连接备用通道…");
     HttpURLConnection backup=connect("https://api.github.com/repos/"+REPOSITORY+"/contents/nufe-local.apk?ref=download");
     try(InputStream input=backup.getInputStream();ByteArrayOutputStream json=new ByteArrayOutputStream()){
      progress.stage("备用通道已连接，正在下载…");
      UpdateTransfer.copy(input,json,backup.getContentLengthLong(),2*1024*1024,progress::bytes);
      progress.stage("下载完成，正在解析安装包…");
      JSONObject payload=new JSONObject(new String(json.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
      if(!"base64".equals(payload.optString("encoding")))throw new IOException("备用下载格式错误");
      byte[] bytes=Base64.getMimeDecoder().decode(payload.getString("content"));
      if(bytes.length!=expected)throw new IOException("备用包尚未同步，请稍后重试");
      try(OutputStream output=new FileOutputStream(part)){output.write(bytes);}
     }finally{backup.disconnect();}
    }
    progress.stage("正在校验版本和签名，请稍候…");verify(part,tag);
    File apk=UpdateProvider.apk(activity);if(apk.exists()&&!apk.delete())throw new IOException("无法替换旧安装包");
    if(!part.renameTo(apk))throw new IOException("无法保存安装包");
   }catch(Exception e){failure=e;part.delete();}
   Exception error=failure;busy=false;ui(()->{progress.dialog.dismiss();if(error!=null)message("下载失败："+error.getMessage());else install();});
  },"download-app-update").start();
 }
 private void verify(File file,String tag)throws Exception{PackageManager pm=activity.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;PackageInfo local=pm.getPackageInfo(activity.getPackageName(),flags),remote=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags);if(remote==null||!activity.getPackageName().equals(remote.packageName))throw new IOException("安装包不是本软件");long lv=Build.VERSION.SDK_INT>=28?local.getLongVersionCode():local.versionCode,rv=Build.VERSION.SDK_INT>=28?remote.getLongVersionCode():remote.versionCode;if(rv<=lv)throw new IOException("安装包版本未高于当前版本");if(tag!=null&&!remote.versionName.equals(tag.replaceFirst("^[vV]","")))throw new IOException("安装包与发布版本号不一致");android.content.pm.Signature[] left=Build.VERSION.SDK_INT>=28?local.signingInfo.getApkContentsSigners():local.signatures,right=Build.VERSION.SDK_INT>=28?remote.signingInfo.getApkContentsSigners():remote.signatures;if(left==null||right==null||left.length!=right.length)throw new IOException("安装包签名不匹配");Set<String> installed=new HashSet<>();for(android.content.pm.Signature s:left)installed.add(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));for(android.content.pm.Signature s:right)if(!installed.contains(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(s.toByteArray()))))throw new IOException("安装包签名不匹配，请使用原签名构建");}
 public void resumeInstall(){if(prefs().getBoolean("pending_install",false)){prefs().edit().putBoolean("pending_install",false).apply();if(activity.getPackageManager().canRequestPackageInstalls())install();else message("未允许安装。你可以稍后重新检查更新。");}}
 private void install(){try{verify(UpdateProvider.apk(activity),null);if(!activity.getPackageManager().canRequestPackageInstalls()){new AlertDialog.Builder(activity).setTitle("允许安装新版").setMessage("请在接下来的系统页面允许本 App 安装应用，然后返回，继续安装。").setNegativeButton("取消",null).setPositiveButton("去设置",(d,w)->{try{activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));prefs().edit().putBoolean("pending_install",true).apply();}catch(Exception e){message("无法打开安装权限设置："+e.getMessage());}}).show();return;}Uri uri=UpdateProvider.uri(activity);Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("软件更新",uri));activity.startActivity(intent);}catch(Exception e){message("无法安装："+e.getMessage());}}
}





