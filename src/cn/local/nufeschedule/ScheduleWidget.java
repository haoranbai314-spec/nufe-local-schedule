package cn.local.nufeschedule;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.RemoteViews;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class ScheduleWidget extends AppWidgetProvider {
 static final String NAV="cn.local.nufeschedule.NAVIGATE";
 static final String PINNED="cn.local.nufeschedule.PINNED";
 static final Handler MAIN=new Handler(Looper.getMainLooper());

 public static void updateAll(Context c){
  AppWidgetManager manager=AppWidgetManager.getInstance(c);
  for(int id:manager.getAppWidgetIds(new ComponentName(c,ScheduleWidget.class)))update(c,manager,id,true);
 }
 static int offset(Context c,int id){return c.getSharedPreferences("widget",0).getInt("offset_"+id,0);}
 static PendingIntent navigate(Context c,int id,int offset){
  Intent intent=new Intent(c,ScheduleWidget.class).setAction(NAV).setData(Uri.parse("nufelocal://widget/"+id+"/"+offset));
  intent.putExtra("widget_id",id).putExtra("offset",offset);
  return PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
 }
 static PendingIntent open(Context c,int id,LocalDate day,Data.Course course){
  Intent intent=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  String key=day==null?"home":day.toString();
  if(day!=null)intent.putExtra("day",day.toString());
  if(course!=null){
   intent.putExtra("course",course.name).putExtra("start",course.start).putExtra("room",course.room);
   key+="/"+course.name+"/"+course.start+"/"+course.room;
  }
  intent.setData(new Uri.Builder().scheme("nufelocal").authority("open").appendPath(String.valueOf(id)).appendPath(key).build());
  return PendingIntent.getActivity(c,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
 }
 static RemoteViews views(Context c,int id,Data data,LocalDate today,LocalDate monday,int offset,int[] range,int minute){
  RemoteViews view=new RemoteViews(c.getPackageName(),R.layout.widget);
  view.setOnClickPendingIntent(R.id.widget_root,open(c,id,null,null));
  view.setTextViewText(R.id.widget_title,data.courses.isEmpty()?"南财课表":"第 "+data.week(monday)+" 周"+(offset==0?" · 本周":" · 回本周"));
  view.setTextViewText(R.id.widget_dates,monday.format(DateTimeFormatter.ofPattern("MM/dd"))+" — "+monday.plusDays(6).format(DateTimeFormatter.ofPattern("MM/dd")));
  view.setOnClickPendingIntent(R.id.widget_prev,navigate(c,id,Math.max(-60,offset-1)));
  view.setOnClickPendingIntent(R.id.widget_next,navigate(c,id,Math.min(60,offset+1)));
  view.setOnClickPendingIntent(R.id.widget_title,navigate(c,id,0));
  int days=5;
  for(int n=5;n<7;n++)if(!data.on(monday.plusDays(n)).isEmpty())days=n+1;
  view.removeAllViews(R.id.widget_headers);
  String[] weekdays={"周一","周二","周三","周四","周五","周六","周日"};
  for(int n=0;n<days;n++){
   LocalDate day=monday.plusDays(n);
   RemoteViews header=new RemoteViews(c.getPackageName(),R.layout.widget_date_header);
   header.setTextViewText(R.id.day_title,weekdays[n]+"\n"+day.format(DateTimeFormatter.ofPattern("M/d")));
   if(day.equals(today))header.setTextColor(R.id.day_title,0xff4859e4);
   header.setOnClickPendingIntent(R.id.day_title,open(c,id,day,null));
   view.addView(R.id.widget_headers,header);
  }
  if(Build.VERSION.SDK_INT<31){
   Intent service=new Intent(c,CourseListService.class).putExtra("widget_id",id)
    .setData(Uri.parse("nufelocal://calendar-grid-v47/"+id+"/"+today));
   view.setRemoteAdapter(R.id.widget_list,service);
  }
  Intent detail=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP)
   .setData(Uri.parse("nufelocal://calendar-details/"+id));
  int flags=PendingIntent.FLAG_UPDATE_CURRENT;
  if(Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;
  view.setPendingIntentTemplate(R.id.widget_list,PendingIntent.getActivity(c,id,detail,flags));
  if(data.courses.isEmpty()){
   view.setOnClickPendingIntent(R.id.widget_footer,open(c,id,null,null));
   view.setOnClickPendingIntent(R.id.widget_footer_hit,open(c,id,null,null));
   view.setTextViewText(R.id.widget_footer,"点击打开并导入课表");
  }else{
   view.setOnClickPendingIntent(R.id.widget_footer,navigate(c,id,0));
   view.setOnClickPendingIntent(R.id.widget_footer_hit,navigate(c,id,0));
   view.setTextViewText(R.id.widget_footer,offset!=0?"上下滑动 · 点击回到本周":
    minute<range[0]?String.format(java.util.Locale.ROOT,"%02d:%02d 前显示早课 · 点击定位",range[0]/60,range[0]%60):
    minute>=range[1]?"今日时间轴已结束 · 点击查看晚间":"上下滑动 · 点击此处定位现在");
  }
  return view;
 }
 static void update(Context c,AppWidgetManager manager,int id){update(c,manager,id,true);}
 static void update(Context c,AppWidgetManager manager,int id,boolean requested){
  try{updateChecked(c,manager,id,requested);}
  catch(RuntimeException error){android.util.Log.e("ScheduleWidget","Widget update failed: "+id,error);}
 }
 private static void updateChecked(Context c,AppWidgetManager manager,int id,boolean requested){
  Context app=c.getApplicationContext();
  SharedPreferences prefs=app.getSharedPreferences("widget",0);
  Data data=Data.load(app);
  ZonedDateTime now=ZonedDateTime.now(Data.ZONE);
  LocalDate today=now.toLocalDate();
  int offset=offset(app,id),minute=now.getHour()*60+now.getMinute();
  LocalDate monday=today.minusDays(today.getDayOfWeek().getValue()-1).plusWeeks(offset);
  int[] range=CourseListService.bounds(data,monday);
  long revision=data.json.optLong("saved_at",0);
  boolean locate=WidgetPosition.locate(requested,prefs.getString("shown_day_"+id,""),today.toString(),
    revision,prefs.getLong("shown_revision_"+id,-1));
  int anchor;
  if(!locate)anchor=WidgetPosition.anchor(range[0],range[1],prefs.getInt("anchor_"+id,minute));
  else if(offset==0)anchor=WidgetPosition.anchor(range[0],range[1],minute);
  else{
   int firstCourse=range[1];
   for(int day=0;day<7;day++)for(Data.Course course:data.on(monday.plusDays(day)))firstCourse=Math.min(firstCourse,course.startMinutes());
   anchor=WidgetPosition.anchor(range[0],range[1],firstCourse==range[1]?range[0]:firstCourse);
  }
  long generation=prefs.getLong("generation_"+id,0)+1;
  prefs.edit().putLong("generation_"+id,generation).putInt("anchor_"+id,anchor).apply();
  RemoteViews view=views(app,id,data,today,monday,offset,range,minute);
  if(Build.VERSION.SDK_INT>=31){
   CourseListService.Factory factory=new CourseListService.Factory(app,id);
   factory.prepare(data,monday,offset,anchor,now);
   RemoteViews.RemoteCollectionItems.Builder items=new RemoteViews.RemoteCollectionItems.Builder()
    .setHasStableIds(true).setViewTypeCount(factory.getViewTypeCount());
   for(int row=0;row<factory.getCount();row++)items.addItem(factory.getItemId(row),factory.getViewAt(row));
   view.setRemoteAdapter(R.id.widget_list,items.build());
   int positionMinute=offset==0?minute:anchor;
   final int target=factory.targetForMinute(positionMinute);
   if(locate&&!data.courses.isEmpty())view.setScrollPosition(R.id.widget_list,target);
   manager.updateAppWidget(id,view);
  }else{
   manager.updateAppWidget(id,view);
   manager.notifyAppWidgetViewDataChanged(id,R.id.widget_list);
  }
  prefs.edit().putString("shown_day_"+id,today.toString()).putLong("shown_revision_"+id,revision).apply();
 }
 @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)update(c,manager,id,false);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,Bundle options){update(c,manager,id,false);}
 @Override public void onDeleted(Context c,int[] ids){
  SharedPreferences.Editor editor=c.getSharedPreferences("widget",0).edit();
  for(int id:ids)for(String key:new String[]{"offset_","anchor_","generation_","shown_day_","shown_revision_"})editor.remove(key+id);
  editor.apply();
 }
 @Override public void onReceive(Context c,Intent intent){
  // Keep the provider alive until its final positioning action has been sent.
  final BroadcastReceiver.PendingResult pending=goAsync();
  try{
   super.onReceive(c,intent);
   String action=intent.getAction();
   if(NAV.equals(action)){
    int id=intent.getIntExtra("widget_id",-1);
    AppWidgetManager manager=AppWidgetManager.getInstance(c);
    AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);
    if(info==null||!info.provider.equals(new ComponentName(c,ScheduleWidget.class)))return;
    int offset=Math.max(-60,Math.min(60,intent.getIntExtra("offset",0)));
    c.getSharedPreferences("widget",0).edit().putInt("offset_"+id,offset).apply();
    update(c,manager,id,true);
   }else if(PINNED.equals(action)){
    c.getSharedPreferences("widget",0).edit().putLong("last_pinned",System.currentTimeMillis()).apply();
    updateAll(c);
   }else if(Intent.ACTION_DATE_CHANGED.equals(action)||Intent.ACTION_TIME_CHANGED.equals(action)||
     Intent.ACTION_TIMEZONE_CHANGED.equals(action)||Intent.ACTION_BOOT_COMPLETED.equals(action)||
     Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))updateAll(c);
  }finally{if(pending!=null)MAIN.postDelayed(pending::finish,1200);}
 }
}

