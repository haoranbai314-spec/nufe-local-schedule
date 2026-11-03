package cn.local.nufeschedule;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class Data {
 public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
 public static final String FILE="schedule.json";
 public JSONObject json=new JSONObject(); public List<Course> courses=new ArrayList<>();
 public LocalDate monday; public String semester="尚未导入";
 public static class Course {
  public String name,room,teacher,start,end; public int weekday,su,eu; public Set<Integer> weeks=new TreeSet<>();
  public int startMinutes(){return minutes(start);} public int endMinutes(){return minutes(end);}
 }
 public static int minutes(String s){try{LocalTime t=LocalTime.parse(s,DateTimeFormatter.ofPattern("H:mm"));return t.getHour()*60+t.getMinute();}catch(Exception e){return -1;}}
 public static LocalDate today(){return LocalDate.now(ZONE);}
 public static Data load(Context c){try(InputStream in=c.openFileInput(FILE)){return parse(read(in));}catch(Exception e){return new Data();}}
 public static String read(InputStream in)throws IOException{
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("文件超过 2 MB");out.write(b,0,n);}return out.toString("UTF-8");
 }
 public static Data parse(String raw)throws Exception{
  JSONObject root=new JSONObject(raw); JSONArray activities=root.optJSONArray("activities");
  if(activities==null){JSONArray v=root.optJSONArray("studentTableVms");if(v!=null&&v.length()>0)activities=v.getJSONObject(0).optJSONArray("activities");}
  if(activities==null)throw new Exception("没有找到课程数据");if(activities.length()>2000)throw new Exception("课程条数过多");
  Data d=new Data();d.json=root;d.semester=root.optString("semester_name","学校课表");
  String date=root.optString("week1_monday","");if(!date.isEmpty())d.monday=LocalDate.parse(date);
  JSONArray clean=new JSONArray();int invalid=0;
  for(int i=0;i<activities.length();i++){
   JSONObject a=activities.optJSONObject(i);if(a==null){invalid++;continue;} Course x=new Course();x.name=a.optString("courseName","").trim();x.weekday=a.optInt("weekday",0);x.su=a.optInt("startUnit",0);x.eu=a.optInt("endUnit",x.su);
   x.start=a.optString("startTime","");x.end=a.optString("endTime","");x.room=a.optString("room","");if(x.room.isEmpty())x.room=a.optString("building","");
   JSONArray teachers=a.optJSONArray("teachers");List<String> names=new ArrayList<>();if(teachers!=null)for(int j=0;j<teachers.length();j++){Object t=teachers.opt(j);String name=t instanceof JSONObject?((JSONObject)t).optString("name",((JSONObject)t).optString("teacherName","")):String.valueOf(t);names.add(name.replaceAll("[（(].*$",""));}x.teacher=String.join("、",names);
   JSONArray ws=a.optJSONArray("weekIndexes");if(ws!=null)for(int j=0;j<ws.length();j++){int w=ws.optInt(j,0);if(w>=1&&w<=60)x.weeks.add(w);}
   if(x.name.isEmpty()||x.weekday<1||x.weekday>7||x.weeks.isEmpty()){invalid++;continue;}
   d.courses.add(x);clean.put(a);
  }
  // 缺少时间时，仅从同一份课表里相同节次的已知时间推断，不猜测学校作息。
  Map<Integer,String> starts=new HashMap<>(),ends=new HashMap<>();for(Course x:d.courses){if(minutes(x.start)>=0&&x.su>0)starts.putIfAbsent(x.su,x.start);if(minutes(x.end)>=0&&x.eu>0)ends.putIfAbsent(x.eu,x.end);}
  for(Course x:d.courses){if(minutes(x.start)<0)x.start=starts.getOrDefault(x.su,"");if(minutes(x.end)<0)x.end=ends.getOrDefault(x.eu,"");if(minutes(x.start)<0||minutes(x.end)<=minutes(x.start))throw new Exception("课程“"+x.name+"”缺少有效时间，请检查学校课表或改用完整 JSON 文件");}
  if(invalid>0)throw new Exception("有 "+invalid+" 条课程缺少周次或星期。为避免漏课，未保存，请检查原始数据。");
  if(d.courses.isEmpty())throw new Exception("这份课表没有可导入的定时课程");
  root.put("activities",clean);root.remove("studentTableVms");root.remove("student");return d;
 }
 public int week(LocalDate day){return monday==null?0:(int)Math.floorDiv(ChronoUnit.DAYS.between(monday,day),7)+1;}
 public List<Course> on(LocalDate day){List<Course> list=new ArrayList<>();int w=week(day);for(Course c:courses)if(c.weekday==day.getDayOfWeek().getValue()&&c.weeks.contains(w))list.add(c);list.sort(Comparator.comparingInt(Course::startMinutes));return list;}
 public void save(Context c,LocalDate week1)throws Exception{
  if(week1.getDayOfWeek()!=DayOfWeek.MONDAY)throw new Exception("第一周起始日必须是周一");monday=week1;json.put("week1_monday",week1.toString());json.put("saved_at",System.currentTimeMillis());
  AtomicFile file=new AtomicFile(new File(c.getFilesDir(),FILE));FileOutputStream out=null;try{out=file.startWrite();out.write(json.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
  ScheduleWidget.updateAll(c);NextLesson.refresh(c);
 }
 public static String lastSaved(Data d){long t=d.json.optLong("saved_at",0);return t==0?"尚未导入":Instant.ofEpochMilli(t).atZone(ZONE).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));}
}

