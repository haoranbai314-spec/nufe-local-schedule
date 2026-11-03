package cn.local.nufeschedule;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import java.time.*;
import java.util.*;

/** Opt-in local lock-screen notification, independent of calendar export. */
public class NextLesson extends BroadcastReceiver {
 static final int REQUEST=71,ID=4001;
 static final String CHANNEL="next_lesson_v2",STOP="cn.local.nufeschedule.STOP_NEXT_LESSON";
 static boolean enabled(Context c){return c.getSharedPreferences("next_lesson",0).getBoolean("enabled",false);}
 static void setEnabled(Context c,boolean value){c.getSharedPreferences("next_lesson",0).edit().putBoolean("enabled",value).remove("published_key").apply();refresh(c);if(c instanceof MainActivity)((MainActivity)c).render();}
 static boolean permitted(Context c){return Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED;}
 public static String status(Context c){
  if(!enabled(c))return "未开启 · 点击设置锁屏课程";
  NotificationManager nm=c.getSystemService(NotificationManager.class);
  if(!permitted(c)||!nm.areNotificationsEnabled())return "通知权限未开启 · 请到通知设置允许";
  NotificationChannel channel=nm.getNotificationChannel(CHANNEL);
  if(channel!=null&&channel.getImportance()==NotificationManager.IMPORTANCE_NONE)return "课程通知已被系统关闭 · 请到通知设置允许";
  Data d=Data.load(c);if(d.monday==null||d.courses.isEmpty())return "已开启 · 请先导入课表";
  long now=System.currentTimeMillis();for(Data.Course course:d.on(Data.today()))if(Data.today().atStartOfDay(Data.ZONE).plusMinutes(course.startMinutes()).toInstant().toEpochMilli()>now)return "已开启 · 下一节 "+course.start+" "+course.name;
  return "已开启 · 今天没有尚未开始的课程，卡片已隐藏";
 }
 public static void systemSettings(Activity a){try{Intent intent=new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,a.getPackageName()).putExtra(android.provider.Settings.EXTRA_CHANNEL_ID,CHANNEL);a.startActivity(intent);}catch(Exception e){try{a.startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,a.getPackageName()));}catch(Exception ignored){CalendarExport.show(a,"请到系统设置 → 应用 → 南财课表·本地 → 通知，允许通知和锁屏显示。");}}}
 public static void settings(Activity a){new AlertDialog.Builder(a).setTitle("锁屏下一节课").setMessage(status(a)+"\n\n显示今天最近一节尚未开始的课程，包括课程名称、教室、起止时间和倒计时。今天没有后续课程时隐藏。\n\n开启即允许在锁屏展示课程详情，最终可见内容由手机锁屏隐私设置决定。无声音、无震动，不上传课表，也不依赖日历导入。\n\n锁屏没有卡片时：\n① 点击下方“通知设置”，允许课程通知，关闭“静默通知／仅在通知中心显示”。\n② 在本 App 的系统通知页面，开启“锁屏通知”和显示通知详情。\n③ 希望不响铃时，将声音设为“无铃声”、关闭振动；不要用“静默通知”代替。\n\n锁屏通知：亮屏但未解锁时的课程卡片。\n息屏通知：黑屏时的简短提示，依赖息屏显示；不必为了锁屏卡片开启息屏显示。\n\n开关名称因品牌而异。系统省电可能延迟切换课程，可打开 App 刷新。") .setNegativeButton("关闭功能",(d,w)->setEnabled(a,false)).setNeutralButton("通知设置",(d,w)->systemSettings(a)).setPositiveButton("开启",(d,w)->{if(!permitted(a))a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},REQUEST);else{setEnabled(a,true);CalendarExport.show(a,status(a)+"\n\n如果锁屏没有显示，请在系统通知设置中允许锁屏通知，并关闭隐藏内容。");}}).show();}
 private static PendingIntent alarm(Context c){return PendingIntent.getBroadcast(c,ID,new Intent(c,NextLesson.class).setAction("cn.local.nufeschedule.REFRESH_LESSON"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static void schedule(Context c,AlarmManager am,long when){
  LessonAlarm.schedule(new LessonAlarm.Driver(){
   public boolean exactAllowed(){return Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms();}
   public void exact(long time){am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,alarm(c));}
   public void inexact(long time){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,alarm(c));}
  },when);
  android.util.Log.i("NextLesson","scheduled at="+when);
 }
 static void alarmSettings(Activity a){
  if(Build.VERSION.SDK_INT<31){CalendarExport.show(a,"此系统无需单独授权精确闹钟。");return;}
  new AlertDialog.Builder(a).setTitle("课程切换时间").setMessage("允许闹钟和提醒可减少课程切换延迟。未允许时仍使用普通定时刷新；系统休眠和厂商限制仍可能延迟。此设置不会开启声音或振动。")
   .setNegativeButton("取消",null).setPositiveButton("前往设置",(d,w)->{
    try{a.startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+a.getPackageName())));}
    catch(ActivityNotFoundException e){CalendarExport.show(a,"请到系统设置 → 特殊应用权限 → 闹钟和提醒中设置。");}
   }).show();
 }
 public static synchronized void refresh(Context c){NotificationManager nm=c.getSystemService(NotificationManager.class);AlarmManager am=c.getSystemService(AlarmManager.class);am.cancel(alarm(c));if(!enabled(c)){nm.cancel(ID);return;}if(!permitted(c)){nm.cancel(ID);schedule(c,am,System.currentTimeMillis()+15*60*1000L);return;}
  if(nm.getNotificationChannel(CHANNEL)==null){NotificationChannel channel=new NotificationChannel(CHANNEL,"锁屏下一节课",NotificationManager.IMPORTANCE_DEFAULT);channel.setDescription("今天最近一节尚未开始的课；无声音和震动");channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);channel.setSound(null,null);channel.enableVibration(false);channel.setShowBadge(false);nm.createNotificationChannel(channel);}if(!nm.areNotificationsEnabled()||nm.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE){nm.cancel(ID);schedule(c,am,System.currentTimeMillis()+15*60*1000L);return;}
  ZonedDateTime now=ZonedDateTime.now(Data.ZONE);Data data=Data.load(c);List<Data.Course> next=new ArrayList<>();long start=Long.MAX_VALUE;
  for(Data.Course course:data.on(now.toLocalDate())){long time=now.toLocalDate().atStartOfDay(Data.ZONE).plusMinutes(course.startMinutes()).toInstant().toEpochMilli();if(time>now.toInstant().toEpochMilli()){if(time<start){next.clear();start=time;}if(time==start)next.add(course);}}
  long update=now.toLocalDate().plusDays(1).atStartOfDay(Data.ZONE).plusMinutes(1).toInstant().toEpochMilli();
  if(next.isEmpty()){nm.cancel(ID);c.getSharedPreferences("next_lesson",0).edit().remove("published_key").apply();}else{Data.Course first=next.get(0);Intent open=new Intent(c,MainActivity.class).setData(Uri.parse("nufelocal://next-lesson/"+start)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);if(next.size()==1)open.putExtra("day",now.toLocalDate().toString()).putExtra("course",first.name).putExtra("start",first.start).putExtra("room",first.room);PendingIntent content=PendingIntent.getActivity(c,ID,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent stop=PendingIntent.getBroadcast(c,ID+1,new Intent(c,NextLesson.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);StringBuilder details=new StringBuilder();for(Data.Course course:next){if(details.length()>0)details.append("\n");details.append(course.name).append(" · ").append(course.start).append("–").append(course.end).append("\n").append(course.room.isEmpty()?"教室未提供":course.room);}
   android.widget.RemoteViews card=new android.widget.RemoteViews(c.getPackageName(),R.layout.notification_next);card.setTextViewText(R.id.notify_title,next.size()==1?"下一节 · "+first.name:"下一时段 · "+next.size()+" 门课");card.setTextViewText(R.id.notify_details,next.size()==1?first.start+"–"+first.end+" · "+(first.room.isEmpty()?"教室未提供":first.room):details.toString());card.setBoolean(R.id.notify_countdown,"setCountDown",true);card.setChronometer(R.id.notify_countdown,android.os.SystemClock.elapsedRealtime()+start-System.currentTimeMillis(),null,true);Notification notification=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.notification_course).setContentTitle(next.size()==1?"下一节 · "+first.name:"下一时段 · "+next.size()+" 门课").setContentText(first.start+"–"+first.end+" · "+(first.room.isEmpty()?"教室未提供":first.room)).setStyle(new Notification.DecoratedCustomViewStyle()).setCustomContentView(card).setCustomBigContentView(card).setWhen(System.currentTimeMillis()).setShowWhen(false).setUsesChronometer(false).setVisibility(Notification.VISIBILITY_PUBLIC).setCategory(Notification.CATEGORY_EVENT).setOnlyAlertOnce(false).setOngoing(false).setTimeoutAfter(start-now.toInstant().toEpochMilli()).setContentIntent(content).addAction(new Notification.Action.Builder(null,"关闭此功能",stop).build()).build();String key=start+"|"+details.toString();boolean active=false;for(android.service.notification.StatusBarNotification item:nm.getActiveNotifications())if(item.getId()==ID)active=true;if(!active||!key.equals(c.getSharedPreferences("next_lesson",0).getString("published_key",""))){nm.notify(ID,notification);android.util.Log.i("NextLesson","posted id="+ID+" start="+start);c.getSharedPreferences("next_lesson",0).edit().putString("published_key",key).apply();}update=start;
  }
  schedule(c,am,update);
 }
 @Override public void onReceive(Context c,Intent i){android.util.Log.i("NextLesson","receive action="+i.getAction());if(STOP.equals(i.getAction()))setEnabled(c,false);else{if(Intent.ACTION_TIME_CHANGED.equals(i.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction()))c.getSharedPreferences("next_lesson",0).edit().remove("published_key").apply();refresh(c);}}
}






