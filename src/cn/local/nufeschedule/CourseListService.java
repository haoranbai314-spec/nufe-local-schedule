package cn.local.nufeschedule;
import android.content.*;import android.appwidget.*;import android.widget.*;import android.text.*;import java.time.*;import java.util.*;
public class CourseListService extends RemoteViewsService {
 static int[] bounds(Data d,LocalDate monday,int days){int first=480,last=1260;for(int n=0;n<days;n++)for(Data.Course c:d.on(monday.plusDays(n))){first=Math.min(first,c.startMinutes()/60*60);last=Math.max(last,(c.endMinutes()+59)/60*60);}return new int[]{first,last};}
 static int[] cuts(Data data,LocalDate monday,int first,int last,int anchor){return WidgetBands.cuts(first,last);}
 @Override public RemoteViewsFactory onGetViewFactory(Intent intent){return new Factory(getApplicationContext(),intent.getIntExtra("widget_id",-1));}
 static class Factory implements RemoteViewsFactory {
  Context context;int id,days,first,last,anchor,viewedOffset,currentMinute;LocalDate snapshotToday;int[] breaks,pixelEdges;Data data;LocalDate monday;float perMinute,font,density,timeWidth;Factory(Context c,int widgetId){context=c;id=widgetId;}
  public void onCreate(){onDataSetChanged();}public void onDestroy(){}
  public void onDataSetChanged(){
   ZonedDateTime now=ZonedDateTime.now(Data.ZONE);LocalDate day=now.toLocalDate();int offset=ScheduleWidget.offset(context,id);prepare(Data.load(context),day.minusDays(day.getDayOfWeek().getValue()-1).plusWeeks(offset),offset,context.getSharedPreferences("widget",0).getInt("anchor_"+id,480),now);
  }
  void prepare(Data snapshot,LocalDate weekStart,int offset,int requestedAnchor,ZonedDateTime now){
   data=snapshot;monday=weekStart;viewedOffset=offset;snapshotToday=now.toLocalDate();currentMinute=now.getHour()*60+now.getMinute();days=ScheduleWidget.columns(context,id);first=480;last=1260;density=context.getResources().getDisplayMetrics().density;
   for(int n=0;n<days;n++)for(Data.Course c:data.on(monday.plusDays(n))){first=Math.min(first,c.startMinutes()/60*60);last=Math.max(last,(c.endMinutes()+59)/60*60);}
   font=days==5?10f:9f;android.os.Bundle options=AppWidgetManager.getInstance(context).getAppWidgetOptions(id);boolean landscape=context.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;int width=Math.max(160,options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,330));int textWidth=Math.max(18,Math.round(((width-20-42f)/days-10)*density));timeWidth=Math.max(12,textWidth-4*density);TextPaint paint=new TextPaint(3);paint.setTextSize(font*context.getResources().getDisplayMetrics().scaledDensity);perMinute=(days==5?.72f:.54f)*density;
   float scaled=context.getResources().getDisplayMetrics().scaledDensity;
   for(int n=0;n<days;n++)for(Data.Course course:data.on(monday.plusDays(n))){
    paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);paint.setTextSize(font*scaled);
    int nameHeight=StaticLayout.Builder.obtain(course.name,0,course.name.length(),paint,textWidth).setIncludePad(true).setLineSpacing(density,1f).build().getHeight();
    String meta=(course.room.isEmpty()?"教室未提供":course.room)+"\n"+(course.teacher.isEmpty()?"教师未提供":course.teacher);
    paint.setTypeface(android.graphics.Typeface.DEFAULT);paint.setTextSize(Math.max(8,font-.5f)*scaled);
    int metaHeight=StaticLayout.Builder.obtain(meta,0,meta.length(),paint,textWidth).setIncludePad(true).setLineSpacing(density,1f).build().getHeight();
    paint.setTextSize(12*scaled);android.graphics.Paint.FontMetrics fm=paint.getFontMetrics();
    float required=nameHeight+metaHeight+2*(fm.descent-fm.ascent)+11*density;
    perMinute=Math.max(perMinute,required/Math.max(1,course.endMinutes()-course.startMinutes()));
   }
   anchor=WidgetPosition.anchor(first,last,requestedAnchor);breaks=cuts(data,monday,first,last,anchor);
   if(android.os.Build.VERSION.SDK_INT>=31){
    int heightDp=options.getInt(landscape?AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,420);
    RemoteViews shell=ScheduleWidget.views(context,id,data,snapshotToday,monday,offset,new int[]{first,last},currentMinute);
    android.view.View root=shell.apply(context,null);
    root.measure(android.view.View.MeasureSpec.makeMeasureSpec(Math.round(width*density),android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(Math.round(heightDp*density),android.view.View.MeasureSpec.EXACTLY));
    root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight());
    int viewport=Math.max(Math.round(80*density),root.findViewById(R.id.widget_list).getMeasuredHeight());
    pixelEdges=WidgetBands.viewportCuts(position(last)+Math.round(22*density),position(anchor),viewport,Math.max(1,Math.round(30*perMinute)),Math.round(18*density));
   }
  }
  public int getCount(){return android.os.Build.VERSION.SDK_INT>=31&&pixelEdges!=null?pixelEdges.length-1:1;}public int getViewTypeCount(){return 2;}public long getItemId(int p){return pixelEdges!=null&&p<pixelEdges.length?pixelEdges[p]:0;}public boolean hasStableIds(){return true;}public RemoteViews getLoadingView(){return new RemoteViews(context.getPackageName(),R.layout.widget_loading);}
  int position(int time){return Math.round((time-first)*perMinute);}
  void bind(RemoteViews block,Data.Course course,int height){if(android.os.Build.VERSION.SDK_INT>=31)block.setViewLayoutHeight(R.id.course_content,height,android.util.TypedValue.COMPLEX_UNIT_PX);else block.setInt(R.id.course_content,"setMinimumHeight",height);block.setTextViewText(R.id.card_name,course.name);block.setTextViewTextSize(R.id.card_name,android.util.TypedValue.COMPLEX_UNIT_SP,font);block.setTextColor(R.id.card_name,WeekPainter.COLORS[Math.floorMod(course.name.hashCode(),WeekPainter.COLORS.length)]);String clock=course.start;block.setTextViewTextSize(R.id.card_time,android.util.TypedValue.COMPLEX_UNIT_SP,12f);block.setFloat(R.id.card_time,"setTextScaleX",1f);block.setTextViewText(R.id.card_time,clock);block.setTextViewText(R.id.card_end,course.end);block.setTextViewTextSize(R.id.card_end,android.util.TypedValue.COMPLEX_UNIT_SP,12f);block.setTextViewText(R.id.card_meta,(course.room.isEmpty()?"教室未提供":course.room)+"\n"+(course.teacher.isEmpty()?"教师未提供":course.teacher));block.setTextViewTextSize(R.id.card_meta,android.util.TypedValue.COMPLEX_UNIT_SP,Math.max(8,font-.5f));}
  void gap(RemoteViews column,int pixels){if(pixels<=0)return;RemoteViews gap=new RemoteViews(context.getPackageName(),R.layout.widget_gap);gap.setInt(R.id.gap,"setHeight",pixels);column.addView(R.id.day_courses,gap);}
  public RemoteViews getViewAt(int p){if(data==null)return null;if(android.os.Build.VERSION.SDK_INT>=31)return band(p);if(p!=0)return null;
   RemoteViews grid=new RemoteViews(context.getPackageName(),R.layout.widget_calendar);grid.removeAllViews(R.id.calendar_grid);grid.setOnClickFillInIntent(R.id.calendar_grid,new Intent());RemoteViews axis=new RemoteViews(context.getPackageName(),R.layout.widget_axis_body);axis.removeAllViews(R.id.axis_ticks);
   for(int t=first;t<last;t+=60){RemoteViews tick=new RemoteViews(context.getPackageName(),R.layout.widget_tick);tick.setTextViewText(R.id.tick,String.format(Locale.ROOT,"%02d:00",t/60));tick.setInt(R.id.tick,"setHeight",position(t+60)-position(t));axis.addView(R.id.axis_ticks,tick);}grid.addView(R.id.calendar_grid,axis);
   for(int n=0;n<days;n++){LocalDate day=monday.plusDays(n);RemoteViews column=new RemoteViews(context.getPackageName(),R.layout.widget_body_day);column.removeAllViews(R.id.day_courses);column.setInt(R.id.day_courses,"setBackgroundColor",day.equals(snapshotToday)?0xfff0f4ff:0x00000000);int cursor=0;
    for(Data.Course c:data.on(day)){int start=position(c.startMinutes());gap(column,Math.max(0,start-cursor));int height=position(c.endMinutes())-start;RemoteViews block=new RemoteViews(context.getPackageName(),Ui.courseLayout(c.name));bind(block,c,height);block.setContentDescription(R.id.course_content,day+"，"+WidgetTimeline.label(c)+"，点击查看详情");Intent fill=new Intent();fill.putExtra("day",day.toString());fill.putExtra("course",c.name);fill.putExtra("start",c.start);fill.putExtra("room",c.room);block.setOnClickFillInIntent(R.id.course_content,fill);column.addView(R.id.day_courses,block);cursor=Math.max(cursor,start+height);}
    gap(column,position(last)-cursor);grid.addView(R.id.calendar_grid,column);
   }return grid;
  }
  RemoteViews line(int y,boolean now){RemoteViews line=new RemoteViews(context.getPackageName(),R.layout.widget_hourline);line.setViewLayoutMargin(R.id.hourline,RemoteViews.MARGIN_TOP,y+Math.round(9*density),android.util.TypedValue.COMPLEX_UNIT_PX);if(now){line.setInt(R.id.hourline,"setBackgroundColor",0xffd86a78);line.setInt(R.id.hourline,"setHeight",Math.max(2,Math.round(density)));}return line;}
  int targetForMinute(int minute){return WidgetBands.pixelTarget(pixelEdges,position(WidgetPosition.anchor(first,last,minute)));}
  RemoteViews band(int p){
   if(p<0||p>=pixelEdges.length-1)return null;
   int rowStart=pixelEdges[p];
   int rowEnd=pixelEdges[p+1];
   int height=Math.max(1,rowEnd-rowStart),inset=Math.round(9*density),minute=currentMinute;
   boolean current=viewedOffset==0&&minute>=first&&minute<last;
   TreeSet<Integer> labels=new TreeSet<>();
   for(int time=first;time<=last;time+=60){
    int y=position(time)+inset;
    if(y>=rowStart&&y<rowEnd&&!(current&&Math.abs(time-minute)<12))labels.add(time);
   }
   int nowY=position(minute)+inset;
   if(current&&nowY>=rowStart&&nowY<rowEnd)labels.add(minute);
   RemoteViews row=new RemoteViews(context.getPackageName(),R.layout.widget_band);
   row.removeAllViews(R.id.calendar_grid);row.setOnClickFillInIntent(R.id.calendar_grid,new Intent());
   RemoteViews axis=new RemoteViews(context.getPackageName(),R.layout.widget_axis_frame);
   axis.setViewLayoutHeight(R.id.axis_frame,height,android.util.TypedValue.COMPLEX_UNIT_PX);
   axis.removeAllViews(R.id.axis_frame);
   for(int time:labels){
    RemoteViews tick=new RemoteViews(context.getPackageName(),R.layout.widget_band_time);
    tick.setInt(R.id.band_time,"setHeight",Math.round(18*density));
    tick.setViewLayoutMargin(R.id.band_time,RemoteViews.MARGIN_TOP,position(time)-rowStart,android.util.TypedValue.COMPLEX_UNIT_PX);
    tick.setTextViewText(R.id.band_time,String.format(Locale.ROOT,"%02d:%02d",time/60,time%60));
    if(current&&time==minute)tick.setTextColor(R.id.band_time,0xffc85f70);
    axis.addView(R.id.axis_frame,tick);
   }
   row.addView(R.id.calendar_grid,axis);
   for(int dayIndex=0;dayIndex<days;dayIndex++){
    LocalDate day=monday.plusDays(dayIndex);
    RemoteViews frame=new RemoteViews(context.getPackageName(),R.layout.widget_band_day);
    frame.setInt(R.id.band_day,"setBackgroundColor",day.equals(snapshotToday)?0xfff0f4ff:0x00000000);
    frame.setViewLayoutHeight(R.id.band_day,height,android.util.TypedValue.COMPLEX_UNIT_PX);
    frame.removeAllViews(R.id.band_day);
    for(int time=first;time<=last;time+=60){
     int y=position(time)+inset;
     if(y>=rowStart&&y<rowEnd)frame.addView(R.id.band_day,line(position(time)-rowStart,false));
    }
    if(current&&nowY>=rowStart&&nowY<rowEnd)frame.addView(R.id.band_day,line(position(minute)-rowStart,true));
    for(Data.Course course:data.on(day)){
     int courseStart=position(course.startMinutes())+inset;
     int courseEnd=position(course.endMinutes())+inset;
     WidgetBands.Slice slice=WidgetBands.clip(rowStart,rowEnd,courseStart,courseEnd);
     if(slice==null)continue;
     RemoteViews block=new RemoteViews(context.getPackageName(),Ui.courseLayout(course.name));
     bind(block,course,courseEnd-courseStart);
     block.setViewLayoutHeight(R.id.course_container,slice.height,android.util.TypedValue.COMPLEX_UNIT_PX);
     block.setViewLayoutMargin(R.id.course_container,RemoteViews.MARGIN_TOP,slice.top,android.util.TypedValue.COMPLEX_UNIT_PX);
     block.setViewLayoutMargin(R.id.course_content,RemoteViews.MARGIN_TOP,slice.offset,android.util.TypedValue.COMPLEX_UNIT_PX);
     Intent fill=new Intent().putExtra("day",day.toString()).putExtra("course",course.name)
      .putExtra("start",course.start).putExtra("room",course.room);
     block.setOnClickFillInIntent(R.id.course_content,fill);
     block.setContentDescription(R.id.course_content,day+"，"+WidgetTimeline.label(course));
     frame.addView(R.id.band_day,block);
    }
    row.addView(R.id.calendar_grid,frame);
   }
   return row;
  }
 }
}












