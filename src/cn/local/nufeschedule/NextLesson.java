package cn.local.nufeschedule;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import java.time.*;
import java.util.*;

public class NextLesson extends BroadcastReceiver {
 static final int REQUEST=71,ID=4001;
 static final String CHANNEL="next_lesson_v2",STOP="cn.local.nufeschedule.STOP_NEXT_LESSON";
 static final String DISMISS="cn.local.nufeschedule.DISMISS_NEXT_LESSON";
 private static SharedPreferences prefs(Context c){return c.getSharedPreferences("next_lesson",0);}
 static boolean enabled(Context c){return prefs(c).getBoolean("enabled",false);}
 static void setEnabled(Context c,boolean value){
  prefs(c).edit().putBoolean("enabled",value).remove("published_key").remove("dismissed_key").apply();
  refresh(c);
  if(c instanceof MainActivity)((MainActivity)c).render();
 }
 static boolean permitted(Context c){return Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED;}
 static final class Plan {
  final List<Data.Course> courses=new ArrayList<>();
  long start=Long.MAX_VALUE,nextRefresh;
  LocalDate date;
  String details="",key="";
 }
 private static Plan plan(Data data,ZonedDateTime now){
  Plan result=new Plan();result.date=now.toLocalDate();
  result.nextRefresh=result.date.plusDays(1).atStartOfDay(Data.ZONE).plusMinutes(1).toInstant().toEpochMilli();
  List<Data.Course> today=data.on(result.date);
  long[] starts=new long[today.size()];
  for(int n=0;n<today.size();n++)starts[n]=result.date.atStartOfDay(Data.ZONE).plusMinutes(today.get(n).startMinutes()).toInstant().toEpochMilli();
  result.start=NotificationState.nextStart(starts,now.toInstant().toEpochMilli());
  if(result.start==Long.MAX_VALUE)return result;
  StringBuilder details=new StringBuilder();
  for(int n=0;n<today.size();n++)if(starts[n]==result.start){
   Data.Course course=today.get(n);result.courses.add(course);
   if(details.length()>0)details.append("\n");
   details.append(course.name).append(" · ").append(course.start).append("–").append(course.end)
    .append("\n").append(course.room.isEmpty()?"教室未提供":course.room);
  }
  result.details=details.toString();result.key=result.start+"|"+result.details;result.nextRefresh=result.start;
  return result;
 }
 public static String status(Context c){
  if(!enabled(c))return "未开启 · 点击设置锁屏课程";
  NotificationManager manager=c.getSystemService(NotificationManager.class);
  if(!permitted(c)||!manager.areNotificationsEnabled())return "通知权限未开启 · 请到通知设置允许";
  NotificationChannel channel=manager.getNotificationChannel(CHANNEL);
  if(channel!=null&&channel.getImportance()==NotificationManager.IMPORTANCE_NONE)return "课程通知已被系统关闭 · 请到通知设置允许";
  Data data=Data.load(c);
  if(data.monday==null||data.courses.isEmpty())return "已开启 · 请先导入课表";
  Plan plan=plan(data,ZonedDateTime.now(Data.ZONE));
  if(plan.courses.isEmpty())return "已开启 · 今天没有尚未开始的课程，卡片已隐藏";
  if(plan.key.equals(prefs(c).getString("dismissed_key","")))return "已开启 · 本节卡片已划走，下节课会重新显示";
  if(!prefs(c).getString("last_error","").isEmpty())return "课程卡片暂未刷新 · 点击管理后重试";
  Data.Course first=plan.courses.get(0);
  return "已开启 · 下一节 "+first.start+" "+first.name;
 }
 public static void systemSettings(Activity activity){
  try{
   activity.startActivity(new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,activity.getPackageName())
    .putExtra(android.provider.Settings.EXTRA_CHANNEL_ID,CHANNEL));
  }catch(Exception error){
   try{activity.startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,activity.getPackageName()));}
   catch(Exception ignored){CalendarExport.show(activity,"请到系统设置 → 应用 → 南财课表 → 通知，允许通知和锁屏显示。");}
  }
 }
 public static void settings(Activity activity){
  String timing=Build.VERSION.SDK_INT<31||activity.getSystemService(AlarmManager.class).canScheduleExactAlarms()?
   "课程切换：已允许准时提醒。":"课程切换：可能延迟，可在设置中允许“课程切换时间”。";
  new AlertDialog.Builder(activity).setTitle("锁屏下一节课")
   .setMessage(status(activity)+"\n\n"+timing+
    "\n\n显示今天最近一节尚未开始的课程。倒计时由系统更新；本节开始后换到下一节，今天没有后续课程时隐藏。划走的本节卡片不会因打开 App 立即恢复，点击“开启／刷新”可恢复。"+
    "\n\n允许在锁屏展示课程名称、教室和时间。声音和振动由手机系统设置决定，App 不修改已有通知设置。"+
    "\n\n锁屏没有卡片时：\n① 点“通知设置”，允许课程通知，关闭“静默通知／仅在通知中心显示”。"+
    "\n② 在本 App 的系统通知页面，开启锁屏通知和显示详情。"+
    "\n③ 如需安静，选择“无铃声”并关闭振动；不要用“静默通知”代替。"+
    "\n\n锁屏通知在亮屏未解锁时显示；息屏通知是黑屏时的简短提示，二者独立。"+
    "\n\n系统省电或强行停止 App 仍可能阻止更新，打开 App 可刷新。")
   .setNegativeButton("关闭功能",(dialog,which)->setEnabled(activity,false))
   .setNeutralButton("通知设置",(dialog,which)->systemSettings(activity))
   .setPositiveButton("开启／刷新",(dialog,which)->{
    if(!permitted(activity))activity.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},REQUEST);
    else{setEnabled(activity,true);CalendarExport.show(activity,status(activity));}
   }).show();
 }
 private static PendingIntent alarm(Context c){
  return PendingIntent.getBroadcast(c,ID,new Intent(c,NextLesson.class).setAction("cn.local.nufeschedule.REFRESH_LESSON"),
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 static void schedule(Context c,AlarmManager manager,long when){
  LessonAlarm.schedule(new LessonAlarm.Driver(){
   public boolean exactAllowed(){return Build.VERSION.SDK_INT<31||manager.canScheduleExactAlarms();}
   public void exact(long time){manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,alarm(c));}
   public void inexact(long time){manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,alarm(c));}
  },when);
  prefs(c).edit().putLong("scheduled_at",when).apply();
  android.util.Log.i("NextLesson","scheduled at="+when);
 }
 static void alarmSettings(Activity activity){
  if(Build.VERSION.SDK_INT<31){CalendarExport.show(activity,"此系统无需单独授权准时提醒。");return;}
  new AlertDialog.Builder(activity).setTitle("课程切换时间")
   .setMessage("允许“闹钟和提醒”可减少课程切换延迟。未允许时仍使用普通定时刷新，可能延迟；此设置不改变通知声音或振动。")
   .setNegativeButton("取消",null).setPositiveButton("前往设置",(dialog,which)->{
    try{activity.startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+activity.getPackageName())));}
    catch(ActivityNotFoundException error){CalendarExport.show(activity,"请到系统设置 → 特殊应用权限 → 闹钟和提醒中设置。");}
   }).show();
 }
 public static synchronized void refresh(Context c){
  try{refreshChecked(c);prefs(c).edit().remove("last_error").apply();}
  catch(RuntimeException error){
   android.util.Log.e("NextLesson","Refresh failed",error);
   prefs(c).edit().putString("last_error",error.getClass().getSimpleName()).apply();
   if(enabled(c))try{schedule(c,c.getSystemService(AlarmManager.class),System.currentTimeMillis()+15*60*1000L);}
   catch(RuntimeException retryFailure){android.util.Log.e("NextLesson","Retry scheduling failed",retryFailure);}
  }
 }
 private static void refreshChecked(Context c){
  NotificationManager manager=c.getSystemService(NotificationManager.class);
  AlarmManager alarms=c.getSystemService(AlarmManager.class);
  alarms.cancel(alarm(c));
  if(!enabled(c)){manager.cancel(ID);return;}
  if(!permitted(c)){manager.cancel(ID);schedule(c,alarms,System.currentTimeMillis()+15*60*1000L);return;}
  NotificationChannel channel=manager.getNotificationChannel(CHANNEL);
  if(channel==null){
   channel=new NotificationChannel(CHANNEL,"锁屏下一节课",NotificationManager.IMPORTANCE_DEFAULT);
   channel.setDescription("今天最近一节尚未开始的课程");
   channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);channel.setSound(null,null);
   channel.enableVibration(false);channel.setShowBadge(false);manager.createNotificationChannel(channel);
  }
  if(!manager.areNotificationsEnabled()||channel.getImportance()==NotificationManager.IMPORTANCE_NONE){
   manager.cancel(ID);schedule(c,alarms,System.currentTimeMillis()+15*60*1000L);return;
  }
  ZonedDateTime now=ZonedDateTime.now(Data.ZONE);
  Plan next=plan(Data.load(c),now);
  if(next.courses.isEmpty()){
   manager.cancel(ID);prefs(c).edit().remove("published_key").remove("dismissed_key").apply();
  }else{
   boolean active=false;
   for(android.service.notification.StatusBarNotification item:manager.getActiveNotifications())if(item.getId()==ID)active=true;
   if(NotificationState.shouldPost(next.key,prefs(c).getString("published_key",""),prefs(c).getString("dismissed_key",""),active)){
    manager.notify(ID,notification(c,next,now.toInstant().toEpochMilli()));
    prefs(c).edit().putString("published_key",next.key).apply();
    android.util.Log.i("NextLesson","posted id="+ID+" start="+next.start);
   }
  }
  schedule(c,alarms,next.nextRefresh);
 }
 private static Notification notification(Context c,Plan next,long now){
  Data.Course first=next.courses.get(0);
  String title=next.courses.size()==1?"下一节 · "+first.name:"下一时段 · "+next.courses.size()+" 门课";
  String summary=first.start+"–"+first.end+" · "+(first.room.isEmpty()?"教室未提供":first.room);
  Intent open=new Intent(c,MainActivity.class).setData(Uri.parse("nufelocal://next-lesson/"+next.start))
   .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  if(next.courses.size()==1)open.putExtra("day",next.date.toString()).putExtra("course",first.name)
   .putExtra("start",first.start).putExtra("room",first.room);
  PendingIntent content=PendingIntent.getActivity(c,ID,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getBroadcast(c,ID+1,new Intent(c,NextLesson.class).setAction(STOP),
   PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Intent dismissed=new Intent(c,NextLesson.class).setAction(DISMISS).putExtra("lesson_key",next.key)
   .setData(new Uri.Builder().scheme("nufelocal").authority("dismiss-lesson").appendPath(next.key).build());
  PendingIntent delete=PendingIntent.getBroadcast(c,ID+2,dismissed,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  android.widget.RemoteViews card=new android.widget.RemoteViews(c.getPackageName(),R.layout.notification_next);
  card.setTextViewText(R.id.notify_title,title);
  card.setTextViewText(R.id.notify_details,next.courses.size()==1?summary:next.details);
  card.setBoolean(R.id.notify_countdown,"setCountDown",true);
  card.setChronometer(R.id.notify_countdown,android.os.SystemClock.elapsedRealtime()+Math.max(0,next.start-now),null,true);
  return new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.notification_course)
   .setContentTitle(title).setContentText(summary).setStyle(new Notification.DecoratedCustomViewStyle())
   .setCustomContentView(card).setCustomBigContentView(card).setWhen(now).setShowWhen(false).setUsesChronometer(false)
   .setVisibility(Notification.VISIBILITY_PUBLIC).setCategory(Notification.CATEGORY_EVENT).setOnlyAlertOnce(false)
   .setOngoing(false).setTimeoutAfter(Math.max(1000,next.start-now)).setContentIntent(content).setDeleteIntent(delete)
   .addAction(new Notification.Action.Builder(null,"关闭此功能",stop).build()).build();
 }
 @Override public void onReceive(Context c,Intent intent){
  String action=intent.getAction();android.util.Log.i("NextLesson","receive action="+action);
  if(STOP.equals(action))setEnabled(c,false);
  else if(DISMISS.equals(action)){
   String key=intent.getStringExtra("lesson_key");
   if(key!=null)prefs(c).edit().putString("dismissed_key",key).apply();
  }else{
   if(Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action))prefs(c).edit().remove("published_key").apply();
   refresh(c);
  }
 }
}
