package cn.local.nufeschedule;
import android.content.*;import android.appwidget.*;import android.widget.*;import android.text.*;import java.time.*;import java.util.*;
public class CourseListService extends RemoteViewsService {
 static int[] bounds(Data d,LocalDate monday){int first=480,last=1260;for(int n=0;n<7;n++)for(Data.Course c:d.on(monday.plusDays(n))){first=Math.min(first,c.startMinutes()/60*60);last=Math.max(last,(c.endMinutes()+59)/60*60);}return new int[]{first,last};}
 static int[] cuts(Data data,LocalDate monday,int first,int last,int anchor){TreeSet<Integer> values=new TreeSet<>();for(int t=first;t<=last;t+=60)values.add(t);values.add(first);values.add(last);values.add(Math.max(first,Math.min(last-1,anchor)));List<Data.Course> all=new ArrayList<>();for(int day=0;day<7;day++)for(Data.Course course:data.on(monday.plusDays(day))){all.add(course);values.add(course.startMinutes());values.add(course.endMinutes());}values.removeIf(t->{for(Data.Course course:all)if(course.startMinutes()<t&&course.endMinutes()>t)return true;return false;});int[] result=new int[values.size()];int n=0;for(int t:values)result[n++]=t;return result;}
 @Override public RemoteViewsFactory onGetViewFactory(Intent intent){return new Factory(getApplicationContext(),intent.getIntExtra("widget_id",-1));}
 static class Factory implements RemoteViewsFactory {
  Context context;int id,days,first,last,anchor;int[] breaks;Data data;LocalDate monday;float perMinute,font,density,timeWidth;Factory(Context c,int widgetId){context=c;id=widgetId;}
  public void onCreate(){onDataSetChanged();}public void onDestroy(){}
  public void onDataSetChanged(){
   data=Data.load(context);LocalDate today=Data.today();monday=today.minusDays(today.getDayOfWeek().getValue()-1).plusWeeks(ScheduleWidget.offset(context,id));days=5;first=480;last=1260;density=context.getResources().getDisplayMetrics().density;
   for(int n=0;n<7;n++)for(Data.Course c:data.on(monday.plusDays(n))){if(n>=5)days=n+1;first=Math.min(first,c.startMinutes()/60*60);last=Math.max(last,(c.endMinutes()+59)/60*60);}
   font=days>5?9f:10f;int width=AppWidgetManager.getInstance(context).getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,330);int textWidth=Math.max(18,Math.round(((width-20-42f)/days-10)*density));timeWidth=Math.max(12,textWidth-4*density);TextPaint paint=new TextPaint(3);paint.setTextSize(font*context.getResources().getDisplayMetrics().scaledDensity);perMinute=.72f*density;
   float scaled=context.getResources().getDisplayMetrics().scaledDensity;
   for(int n=0;n<7;n++)for(Data.Course course:data.on(monday.plusDays(n))){
    paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);paint.setTextSize(font*scaled);
    int nameHeight=StaticLayout.Builder.obtain(course.name,0,course.name.length(),paint,textWidth).setIncludePad(false).build().getHeight();
    String meta=(course.room.isEmpty()?"教室未提供":course.room)+"\n"+(course.teacher.isEmpty()?"教师未提供":course.teacher);
    paint.setTypeface(android.graphics.Typeface.DEFAULT);paint.setTextSize(Math.max(8,font-.5f)*scaled);
    int metaHeight=StaticLayout.Builder.obtain(meta,0,meta.length(),paint,textWidth).setIncludePad(false).build().getHeight();
    paint.setTextSize(12*scaled);android.graphics.Paint.FontMetrics fm=paint.getFontMetrics();
    float required=nameHeight+metaHeight+2*(fm.descent-fm.ascent)+11*density;
    perMinute=Math.max(perMinute,required/Math.max(1,course.endMinutes()-course.startMinutes()));
   }
   anchor=context.getSharedPreferences("widget",0).getInt("anchor_"+id,first);breaks=cuts(data,monday,first,last,anchor);
  }
  public int getCount(){return android.os.Build.VERSION.SDK_INT>=31&&breaks!=null?breaks.length:1;}public int getViewTypeCount(){return 2;}public long getItemId(int p){return breaks!=null&&p<breaks.length?breaks[p]:0;}public boolean hasStableIds(){return true;}public RemoteViews getLoadingView(){return new RemoteViews(context.getPackageName(),R.layout.widget_loading);}
  int position(int time){return Math.round((time-first)*perMinute);}
  void bind(RemoteViews block,Data.Course course,int height){if(android.os.Build.VERSION.SDK_INT>=31)block.setViewLayoutHeight(R.id.course_content,height,android.util.TypedValue.COMPLEX_UNIT_PX);else block.setInt(R.id.course_content,"setMinimumHeight",height);block.setTextViewText(R.id.card_name,course.name);block.setTextViewTextSize(R.id.card_name,android.util.TypedValue.COMPLEX_UNIT_SP,font);block.setTextColor(R.id.card_name,WeekPainter.COLORS[Math.floorMod(course.name.hashCode(),WeekPainter.COLORS.length)]);String clock=course.start;block.setTextViewTextSize(R.id.card_time,android.util.TypedValue.COMPLEX_UNIT_SP,12f);block.setFloat(R.id.card_time,"setTextScaleX",1f);block.setTextViewText(R.id.card_time,clock);block.setTextViewText(R.id.card_end,course.end);block.setTextViewTextSize(R.id.card_end,android.util.TypedValue.COMPLEX_UNIT_SP,12f);block.setTextViewText(R.id.card_meta,(course.room.isEmpty()?"教室未提供":course.room)+"\n"+(course.teacher.isEmpty()?"教师未提供":course.teacher));block.setTextViewTextSize(R.id.card_meta,android.util.TypedValue.COMPLEX_UNIT_SP,Math.max(8,font-.5f));}
  void gap(RemoteViews column,int pixels){if(pixels<=0)return;RemoteViews gap=new RemoteViews(context.getPackageName(),R.layout.widget_gap);gap.setInt(R.id.gap,"setHeight",pixels);column.addView(R.id.day_courses,gap);}
  public RemoteViews getViewAt(int p){if(data==null)return null;if(android.os.Build.VERSION.SDK_INT>=31)return band(p);if(p!=0)return null;
   RemoteViews grid=new RemoteViews(context.getPackageName(),R.layout.widget_calendar);grid.removeAllViews(R.id.calendar_grid);grid.setOnClickFillInIntent(R.id.calendar_grid,new Intent());RemoteViews axis=new RemoteViews(context.getPackageName(),R.layout.widget_axis_body);axis.removeAllViews(R.id.axis_ticks);
   for(int t=first;t<last;t+=60){RemoteViews tick=new RemoteViews(context.getPackageName(),R.layout.widget_tick);tick.setTextViewText(R.id.tick,String.format(Locale.ROOT,"%02d:00",t/60));tick.setInt(R.id.tick,"setHeight",position(t+60)-position(t));axis.addView(R.id.axis_ticks,tick);}grid.addView(R.id.calendar_grid,axis);
   for(int n=0;n<days;n++){LocalDate day=monday.plusDays(n);RemoteViews column=new RemoteViews(context.getPackageName(),R.layout.widget_body_day);column.removeAllViews(R.id.day_courses);column.setInt(R.id.day_courses,"setBackgroundColor",day.equals(Data.today())?0xfff0f4ff:0x00000000);int cursor=0;
    for(Data.Course c:data.on(day)){int start=position(c.startMinutes());gap(column,Math.max(0,start-cursor));int height=position(c.endMinutes())-start;RemoteViews block=new RemoteViews(context.getPackageName(),Ui.courseLayout(c.name));bind(block,c,height);block.setContentDescription(R.id.course_content,day+"，"+WidgetTimeline.label(c)+"，点击查看详情");Intent fill=new Intent();fill.putExtra("day",day.toString());fill.putExtra("course",c.name);fill.putExtra("start",c.start);fill.putExtra("room",c.room);block.setOnClickFillInIntent(R.id.course_content,fill);column.addView(R.id.day_courses,block);cursor=Math.max(cursor,start+height);}
    gap(column,position(last)-cursor);grid.addView(R.id.calendar_grid,column);
   }return grid;
  }
  RemoteViews line(int y,boolean now){RemoteViews line=new RemoteViews(context.getPackageName(),R.layout.widget_hourline);line.setViewLayoutMargin(R.id.hourline,RemoteViews.MARGIN_TOP,y+Math.round(9*density),android.util.TypedValue.COMPLEX_UNIT_PX);if(now){line.setInt(R.id.hourline,"setBackgroundColor",0xffd86a78);line.setInt(R.id.hourline,"setHeight",Math.max(2,Math.round(density)));}return line;}
  RemoteViews endRow(){RemoteViews row=new RemoteViews(context.getPackageName(),R.layout.widget_band);row.removeAllViews(R.id.calendar_grid);row.setOnClickFillInIntent(R.id.calendar_grid,new Intent());RemoteViews label=new RemoteViews(context.getPackageName(),R.layout.widget_band_time);label.setTextViewText(R.id.band_time,String.format(Locale.ROOT,"%02d:%02d",last/60,last%60));label.setInt(R.id.band_time,"setHeight",Math.round(18*density));row.addView(R.id.calendar_grid,label);for(int day=0;day<days;day++){RemoteViews frame=new RemoteViews(context.getPackageName(),R.layout.widget_band_day);frame.setInt(R.id.band_day,"setBackgroundColor",monday.plusDays(day).equals(Data.today())?0xfff0f4ff:0x00000000);frame.setViewLayoutHeight(R.id.band_day,22*density,android.util.TypedValue.COMPLEX_UNIT_PX);frame.addView(R.id.band_day,line(0,false));row.addView(R.id.calendar_grid,frame);}return row;}
  RemoteViews band(int p){if(p<0||p>=breaks.length)return null;if(p==breaks.length-1)return endRow();
   int start=breaks[p],end=breaks[p+1],height=position(end)-position(start)+Math.round(9*density);LocalTime clock=LocalTime.now(Data.ZONE);int minute=clock.getHour()*60+clock.getMinute();boolean current=ScheduleWidget.offset(context,id)==0&&minute>=start&&minute<end;
   RemoteViews row=new RemoteViews(context.getPackageName(),R.layout.widget_band);row.removeAllViews(R.id.calendar_grid);row.setOnClickFillInIntent(R.id.calendar_grid,new Intent());RemoteViews axis=new RemoteViews(context.getPackageName(),R.layout.widget_axis_frame);axis.setViewLayoutHeight(R.id.axis_frame,height,android.util.TypedValue.COMPLEX_UNIT_PX);axis.removeAllViews(R.id.axis_frame);
   TreeSet<Integer> labels=new TreeSet<>();for(int t=(start+59)/60*60;t<end;t+=60)labels.add(t);if(current)labels.add(minute);
   for(int time:labels){if(current&&time!=minute&&Math.abs(time-minute)<12)continue;RemoteViews tick=new RemoteViews(context.getPackageName(),R.layout.widget_band_time);tick.setInt(R.id.band_time,"setHeight",Math.round(18*density));tick.setViewLayoutMargin(R.id.band_time,RemoteViews.MARGIN_TOP,position(time)-position(start),android.util.TypedValue.COMPLEX_UNIT_PX);tick.setTextViewText(R.id.band_time,String.format(Locale.ROOT,"%02d:%02d",time/60,time%60));if(current&&time==minute)tick.setTextColor(R.id.band_time,0xffc85f70);axis.addView(R.id.axis_frame,tick);}row.addView(R.id.calendar_grid,axis);
   for(int dayIndex=0;dayIndex<days;dayIndex++){LocalDate day=monday.plusDays(dayIndex);RemoteViews frame=new RemoteViews(context.getPackageName(),R.layout.widget_band_day);frame.setInt(R.id.band_day,"setBackgroundColor",day.equals(Data.today())?0xfff0f4ff:0x00000000);frame.setViewLayoutHeight(R.id.band_day,height,android.util.TypedValue.COMPLEX_UNIT_PX);frame.removeAllViews(R.id.band_day);
    for(int time=(start+59)/60*60;time<end;time+=60)frame.addView(R.id.band_day,line(position(time)-position(start),false));if(current)frame.addView(R.id.band_day,line(position(minute)-position(start),true));
    for(Data.Course course:data.on(day)){if(course.startMinutes()<start||course.endMinutes()>end)continue;RemoteViews block=new RemoteViews(context.getPackageName(),Ui.courseLayout(course.name));bind(block,course,position(course.endMinutes())-position(course.startMinutes()));block.setViewLayoutMargin(R.id.course_content,RemoteViews.MARGIN_TOP,position(course.startMinutes())-position(start)+Math.round(9*density),android.util.TypedValue.COMPLEX_UNIT_PX);Intent fill=new Intent();fill.putExtra("day",day.toString());fill.putExtra("course",course.name);fill.putExtra("start",course.start);fill.putExtra("room",course.room);block.setOnClickFillInIntent(R.id.course_content,fill);block.setContentDescription(R.id.course_content,day+"，"+WidgetTimeline.label(course));frame.addView(R.id.band_day,block);}row.addView(R.id.calendar_grid,frame);
   }return row;
  }
 }
}









