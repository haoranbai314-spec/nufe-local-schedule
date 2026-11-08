package cn.local.nufeschedule;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import java.time.*;
import java.util.*;

/** Exports dated lessons into an app-owned local calendar in one transaction. */
public final class CalendarExport {
 static final int REQUEST=61;
 private static final String ACCOUNT="cn.local.nufeschedule", NAME="nufe_local_lessons";
 private static volatile boolean busy;
 static void show(Activity activity,String text){if(!activity.isFinishing()&&!activity.isDestroyed())new AlertDialog.Builder(activity).setMessage(text).setPositiveButton("知道了",null).show();}
 public static void begin(Activity activity){
  Data data=Data.load(activity);if(data.monday==null||data.courses.isEmpty()){show(activity,"请先登录教务导入课表，并核对第一周周一日期。");return;}
  if(busy){show(activity,"正在导入，请稍候。");return;}
  List<ContentValues> lessons=new ArrayList<>();Set<String> seen=new HashSet<>();LocalDate first=null,last=null;
  for(Data.Course c:data.courses)for(int week:c.weeks){LocalDate date=data.monday.plusWeeks(week-1).plusDays(c.weekday-1);long start=date.atStartOfDay(Data.ZONE).plusMinutes(c.startMinutes()).toInstant().toEpochMilli();long end=date.atStartOfDay(Data.ZONE).plusMinutes(c.endMinutes()).toInstant().toEpochMilli();String key=date+"|"+c.name+"|"+start+"|"+end+"|"+c.room;if(!seen.add(key))continue;
   ContentValues value=new ContentValues();value.put(Events.TITLE,c.name);value.put(Events.DTSTART,start);value.put(Events.DTEND,end);value.put(Events.EVENT_TIMEZONE,Data.ZONE.getId());value.put(Events.EVENT_END_TIMEZONE,Data.ZONE.getId());value.put(Events.EVENT_LOCATION,c.room);value.put(Events.DESCRIPTION,data.semester+" · 第 "+week+" 周\n教师："+c.teacher+"\n由南财课表·本地导入；教务调课后请重新导入日历。");value.put(Events.HAS_ALARM,0);lessons.add(value);if(first==null||date.isBefore(first))first=date;if(last==null||date.isAfter(last))last=date;
  }
  if(lessons.size()>1000){show(activity,"展开后的课程超过1000节，暂不支持一次导入，请检查课表周次。");return;}
  if(activity.checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED||activity.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)!=PackageManager.PERMISSION_GRANTED){new AlertDialog.Builder(activity).setTitle("允许访问手机日历").setMessage("需要日历权限来建立“南财课表·本地”日历，并避免重复导入。仅操作本 App 建立的日历，不上传日历内容。").setNegativeButton("取消",null).setPositiveButton("继续",(d,w)->activity.requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR},REQUEST)).show();return;}
  new AlertDialog.Builder(activity).setTitle("导入手机日历").setMessage(data.semester+"\n"+first+" 至 "+last+"\n共 "+lessons.size()+" 节课，按实际周次和北京时间写入。\n\n会替换“南财课表·本地”专用日历内的旧课程（包括在该日历内手动修改或新增的内容），其他日历不受影响。默认不添加提醒。\n\n软件更新课表后，需要再次点击此按钮更新手机日历。").setNegativeButton("取消",null).setPositiveButton("导入",(d,w)->write(activity,lessons)).show();
 }
 private static void write(Activity activity,List<ContentValues> lessons){if(busy){show(activity,"正在导入，请稍候。");return;}busy=true;ProgressDialog progress=ProgressDialog.show(activity,"导入手机日历","正在写入课程…",true,false);new Thread(()->{String result;
  try{
   ContentResolver resolver=activity.getContentResolver();long id=-1;try(Cursor cursor=resolver.query(Calendars.CONTENT_URI,new String[]{Calendars._ID},Calendars.ACCOUNT_NAME+"=? AND "+Calendars.ACCOUNT_TYPE+"=? AND "+Calendars.NAME+"=?",new String[]{ACCOUNT,CalendarContract.ACCOUNT_TYPE_LOCAL,NAME},null)){if(cursor!=null&&cursor.moveToFirst())id=cursor.getLong(0);}
   ArrayList<ContentProviderOperation> operations=new ArrayList<>();
   if(id<0){Uri uri=Calendars.CONTENT_URI.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER,"true").appendQueryParameter(Calendars.ACCOUNT_NAME,ACCOUNT).appendQueryParameter(Calendars.ACCOUNT_TYPE,CalendarContract.ACCOUNT_TYPE_LOCAL).build();ContentValues cal=new ContentValues();cal.put(Calendars.ACCOUNT_NAME,ACCOUNT);cal.put(Calendars.ACCOUNT_TYPE,CalendarContract.ACCOUNT_TYPE_LOCAL);cal.put(Calendars.NAME,NAME);cal.put(Calendars.CALENDAR_DISPLAY_NAME,"南财课表·本地");cal.put(Calendars.CALENDAR_COLOR,0xff5264ad);cal.put(Calendars.CALENDAR_ACCESS_LEVEL,Calendars.CAL_ACCESS_OWNER);cal.put(Calendars.OWNER_ACCOUNT,ACCOUNT);cal.put(Calendars.CALENDAR_TIME_ZONE,Data.ZONE.getId());cal.put(Calendars.VISIBLE,1);cal.put(Calendars.SYNC_EVENTS,1);operations.add(ContentProviderOperation.newInsert(uri).withValues(cal).build());}
   else operations.add(ContentProviderOperation.newDelete(Events.CONTENT_URI).withSelection(Events.CALENDAR_ID+"=?",new String[]{Long.toString(id)}).build());
   for(ContentValues values:lessons){ContentProviderOperation.Builder insert=ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(values);if(id<0)insert.withValueBackReference(Events.CALENDAR_ID,0);else insert.withValue(Events.CALENDAR_ID,id);operations.add(insert.build());}
   resolver.applyBatch(CalendarContract.AUTHORITY,operations);
   result="已导入 "+lessons.size()+" 节课。\n打开手机日历，在日历列表中勾选“南财课表·本地”即可查看。部分品牌需要手动启用这个日历。";
  }catch(Exception e){result="导入失败："+e.getMessage()+"\n请确认已允许日历权限，且手机系统日历服务可用。";}
  busy=false;String text=result;activity.runOnUiThread(()->{if(!activity.isFinishing()&&!activity.isDestroyed()){progress.dismiss();show(activity,text);}});
 },"export-calendar").start();}
}

