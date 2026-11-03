package cn.local.nufeschedule;
import android.app.*;import android.appwidget.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.widget.RemoteViews;
import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;
public class ScheduleWidget extends AppWidgetProvider {
 public static void updateAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,ScheduleWidget.class));for(int id:ids)update(c,m,id);}
 static final String NAV="cn.local.nufeschedule.NAVIGATE";
 static final String PINNED="cn.local.nufeschedule.PINNED";
 static int offset(Context c,int id){return c.getSharedPreferences("widget",0).getInt("offset_"+id,0);}
 static PendingIntent navigate(Context c,int id,int offset){Intent i=new Intent(c,ScheduleWidget.class).setAction(NAV).setData(Uri.parse("nufelocal://widget/"+id+"/"+offset));i.putExtra("widget_id",id);i.putExtra("offset",offset);return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
 static PendingIntent open(Context c,int id,LocalDate day,Data.Course course){Intent i=new Intent(c,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);String key=day==null?"home":day.toString();if(day!=null)i.putExtra("day",day.toString());if(course!=null){i.putExtra("course",course.name);i.putExtra("start",course.start);i.putExtra("room",course.room);key+="/"+course.name+"/"+course.start+"/"+course.room;}i.setData(new Uri.Builder().scheme("nufelocal").authority("open").appendPath(String.valueOf(id)).appendPath(key).build());return PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
 static RemoteViews views(Context c,int id,Data data,LocalDate today,int offset){
  LocalDate monday=today.minusDays(today.getDayOfWeek().getValue()-1).plusWeeks(offset);
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);v.setOnClickPendingIntent(R.id.widget_root,open(c,id,null,null));v.setTextViewText(R.id.widget_title,"第 "+data.week(monday)+" 周"+(offset==0?" · 本周":" · 回本周"));v.setTextViewText(R.id.widget_dates,monday.format(DateTimeFormatter.ofPattern("MM/dd"))+" — "+monday.plusDays(6).format(DateTimeFormatter.ofPattern("MM/dd")));
  v.setOnClickPendingIntent(R.id.widget_prev,navigate(c,id,Math.max(-60,offset-1)));v.setOnClickPendingIntent(R.id.widget_next,navigate(c,id,Math.min(60,offset+1)));v.setOnClickPendingIntent(R.id.widget_title,navigate(c,id,0));v.setOnClickPendingIntent(R.id.widget_footer,open(c,id,null,null));v.setTextViewText(R.id.widget_footer,data.courses.isEmpty()?"点击打开并导入课表":"本地 · "+Data.lastSaved(data)+" · 点击打开");
  int days=5;for(int n=5;n<7;n++)if(!data.on(monday.plusDays(n)).isEmpty())days=n+1;v.removeAllViews(R.id.widget_headers);
  for(int n=0;n<days;n++){LocalDate day=monday.plusDays(n);RemoteViews header=new RemoteViews(c.getPackageName(),R.layout.widget_date_header);header.setTextViewText(R.id.day_title,new String[]{"周一","周二","周三","周四","周五","周六","周日"}[n]+"\n"+day.format(DateTimeFormatter.ofPattern("M/d")));if(day.equals(today))header.setTextColor(R.id.day_title,0xff4859e4);header.setOnClickPendingIntent(R.id.day_title,open(c,id,day,null));v.addView(R.id.widget_headers,header);}
  Intent service=new Intent(c,CourseListService.class).putExtra("widget_id",id).setData(Uri.parse("nufelocal://calendar-grid-v45/"+id+"/"+today));v.setRemoteAdapter(R.id.widget_list,service);
  Intent detail=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).setData(Uri.parse("nufelocal://calendar-details/"+id));int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(android.os.Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;v.setPendingIntentTemplate(R.id.widget_list,PendingIntent.getActivity(c,id,detail,flags));v.setTextViewText(R.id.widget_footer,data.courses.isEmpty()?"点击打开并导入课表":"上下滑动查看课程 · 左侧为实际时间");
  int[] range=CourseListService.bounds(data,monday);int minute=LocalTime.now(Data.ZONE).getHour()*60+LocalTime.now(Data.ZONE).getMinute();int anchor=Math.max(range[0],Math.min(range[1]-60,minute));if(offset!=0){anchor=range[1];for(int n=0;n<7;n++)for(Data.Course course:data.on(monday.plusDays(n)))anchor=Math.min(anchor,course.startMinutes());anchor=Math.max(range[0],Math.min(range[1]-1,anchor));}c.getSharedPreferences("widget",0).edit().putInt("anchor_"+id,anchor).apply();
  v.setOnClickPendingIntent(R.id.widget_footer,navigate(c,id,0));v.setTextViewText(R.id.widget_footer,minute<range[0]?"08:00 前显示早课 · 点击定位现在":minute>=range[1]?"今日时间轴已结束 · 点击查看晚间":"上下滑动 · 点击此处定位现在");

  return v;
 }
 static void update(Context c,AppWidgetManager manager,int id){update(c,manager,id,true);}
 static void update(Context c,AppWidgetManager manager,int id,boolean locate){long generation=c.getSharedPreferences("widget",0).getLong("generation_"+id,0)+1;c.getSharedPreferences("widget",0).edit().putLong("generation_"+id,generation).apply();Data data=Data.load(c);LocalDate today=Data.today();int offset=offset(c,id);RemoteViews content=views(c,id,data,today,offset);LocalDate shownMonday=today.minusDays(today.getDayOfWeek().getValue()-1).plusWeeks(offset);int[] dayRange=CourseListService.bounds(data,shownMonday);LocalTime localTime=LocalTime.now(Data.ZONE);boolean afterEnd=offset==0&&localTime.getHour()*60+localTime.getMinute()>=dayRange[1];if(afterEnd&&android.os.Build.VERSION.SDK_INT>=31){CourseListService.Factory factory=new CourseListService.Factory(c,id);factory.onDataSetChanged();RemoteViews.RemoteCollectionItems.Builder items=new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(factory.getViewTypeCount());for(int row=0;row<factory.getCount();row++)items.addItem(factory.getItemId(row),factory.getViewAt(row));content.setRemoteAdapter(R.id.widget_list,items.build());if(locate)content.setScrollPosition(R.id.widget_list,factory.getCount()-1);manager.updateAppWidget(id,content);return;}manager.updateAppWidget(id,content);manager.notifyAppWidgetViewDataChanged(id,R.id.widget_list);if(locate&&android.os.Build.VERSION.SDK_INT>=31){LocalDate monday=today.minusDays(today.getDayOfWeek().getValue()-1).plusWeeks(offset);int[] range=CourseListService.bounds(data,monday);int anchor=c.getSharedPreferences("widget",0).getInt("anchor_"+id,range[0]);int[] cuts=CourseListService.cuts(data,monday,range[0],range[1],anchor);int index=0;for(int n=0;n<cuts.length;n++)if(cuts[n]<=anchor&&n<cuts.length-1)index=n;final int target=index;new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{if(c.getSharedPreferences("widget",0).getLong("generation_"+id,0)!=generation||offset(c,id)!=offset||c.getSharedPreferences("widget",0).getInt("anchor_"+id,-1)!=anchor)return;RemoteViews scroll=new RemoteViews(c.getPackageName(),R.layout.widget);scroll.setScrollPosition(R.id.widget_list,Math.min(cuts.length-1,target+1));manager.partiallyUpdateAppWidget(id,scroll);new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{if(c.getSharedPreferences("widget",0).getLong("generation_"+id,0)!=generation||offset(c,id)!=offset)return;RemoteViews align=new RemoteViews(c.getPackageName(),R.layout.widget);align.setScrollPosition(R.id.widget_list,target);manager.partiallyUpdateAppWidget(id,align);},500);},350);}}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){update(c,m,id,false);}
 @Override public void onDeleted(Context c,int[] ids){android.content.SharedPreferences.Editor e=c.getSharedPreferences("widget",0).edit();for(int id:ids)e.remove("offset_"+id);e.apply();}
 @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);String a=i.getAction();if(PINNED.equals(a)){int id=i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);AppWidgetManager manager=AppWidgetManager.getInstance(c);AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info!=null&&info.provider.equals(new ComponentName(c,ScheduleWidget.class))){c.getSharedPreferences("widget",0).edit().putLong("last_pinned",System.currentTimeMillis()).apply();update(c,manager,id);android.widget.Toast.makeText(c,"课表组件已添加到桌面",android.widget.Toast.LENGTH_LONG).show();}}else if(NAV.equals(a)){int id=i.getIntExtra("widget_id",-1);AppWidgetManager manager=AppWidgetManager.getInstance(c);AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null||!info.provider.equals(new ComponentName(c,ScheduleWidget.class)))return;int offset=Math.max(-60,Math.min(60,i.getIntExtra("offset",0)));c.getSharedPreferences("widget",0).edit().putInt("offset_"+id,offset).apply();update(c,manager,id);}else if(Intent.ACTION_DATE_CHANGED.equals(a)||Intent.ACTION_TIME_CHANGED.equals(a)||Intent.ACTION_TIMEZONE_CHANGED.equals(a))updateAll(c);}
}



















