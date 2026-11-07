package cn.local.nufeschedule;
import android.graphics.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class WeekPainter {
 public static class Hit { public LocalDate day;public Data.Course course;Hit(LocalDate d,Data.Course x){day=d;course=x;} }
 public static Hit hit(Data data,LocalDate monday,int width,int height,float px,float py){
  if(width<=0||height<=0)return null;float scale=width/380f,w=380,h=height/scale,x=px/scale,y=py/scale;
  int days=5,min=8*60,max=18*60;for(int i=0;i<7;i++)for(Data.Course c:data.on(monday.plusDays(i))){if(i>=5)days=i+1;min=Math.min(min,(c.startMinutes()/60)*60);max=Math.max(max,((c.endMinutes()+59)/60)*60);}
  float left=33,top=76,bottom=h-25,cw=(w-left-9)/days,range=max-min;
  for(int i=days-1;i>=0;i--){java.util.List<Data.Course> list=data.on(monday.plusDays(i));for(int j=list.size()-1;j>=0;j--){Data.Course c=list.get(j);float x0=left+i*cw+2,y0=top+(c.startMinutes()-min)/range*(bottom-top)+1,y1=top+(c.endMinutes()-min)/range*(bottom-top)-1;if(x>=x0&&x<=x0+cw-4&&y>=y0&&y<=y1)return new Hit(monday.plusDays(i),c);}}
  return null;
 }
 static final int[] COLORS={0xff5264ad,0xffaa577b,0xff2d7c70,0xff9e7236,0xff7963a5,0xff3d76a3,0xff637f3d};
 static Paint p=new Paint(3);
 static void text(Canvas c,String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));c.drawText(s,x,y,p);}
 static String cut(String s,float width,float size){p.setTextSize(size);if(p.measureText(s)<=width)return s;int n=p.breakText(s,true,Math.max(0,width-p.measureText("…")),null);return s.substring(0,n)+"…";}
 public static Bitmap draw(Data data,LocalDate monday,int width,int height){
  width=Math.max(240,width);height=Math.max(200,height);Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);float scale=width/380f;c.scale(scale,scale);float w=width/scale,h=height/scale;
  p.setColor(0xffffffff);c.drawRoundRect(0,0,w,h,16,16,p);LocalDate today=Data.today();
  text(c,"周课表",14,24,15,0xff26334b);String week=data.monday==null?"未导入":"第 "+data.week(monday)+" 周";text(c,week,w-76,24,11,0xff5264c6);
  text(c,monday.format(DateTimeFormatter.ofPattern("MM/dd"))+" — "+monday.plusDays(6).format(DateTimeFormatter.ofPattern("MM/dd")),14,43,10,0xff74809a);
  if(data.courses.isEmpty()){text(c,"点击打开，登录学校教务导入课表",14,90,14,0xff59627c);return bitmap;}
  int days=5,min=8*60,max=18*60;for(int i=0;i<7;i++)for(Data.Course x:data.on(monday.plusDays(i))){if(i>=5)days=i+1;min=Math.min(min,(x.startMinutes()/60)*60);max=Math.max(max,((x.endMinutes()+59)/60)*60);}
  float left=33,top=76,bottom=h-25,cw=(w-left-9)/days,range=max-min;String[] labels={"一","二","三","四","五","六","日"};
  for(int i=0;i<days;i++){LocalDate day=monday.plusDays(i);boolean current=day.equals(today);if(current){p.setColor(0xfff0f3fc);c.drawRoundRect(left+i*cw,top-26,left+(i+1)*cw,bottom,6,6,p);}text(c,"周"+labels[i],left+i*cw+5,top-13,10,current?0xff5264c6:0xff69758c);text(c,String.valueOf(day.getDayOfMonth()),left+i*cw+5,top-1,9,current?0xff5264c6:0xff8994a9);}
  for(int time=min;time<=max;time+=60){float y=top+(time-min)/range*(bottom-top);p.setColor(0xffe2e7f2);p.setStrokeWidth(.5f);c.drawLine(left,y,w-9,y,p);text(c,String.format(Locale.ROOT,"%02d:00",time/60),3,y+3,8,0xff8591a7);}
  for(int i=0;i<days;i++)for(Data.Course x:data.on(monday.plusDays(i))){
   float x0=left+i*cw+2,y0=top+(x.startMinutes()-min)/range*(bottom-top)+1,y1=top+(x.endMinutes()-min)/range*(bottom-top)-1;int color=COLORS[Math.floorMod(x.name.hashCode(),COLORS.length)];
   p.setColor((color&0x00ffffff)|0x1c000000);c.drawRoundRect(x0,y0,x0+cw-4,y1,6,6,p);p.setColor(color);c.drawRoundRect(x0,y0,x0+2,y1,1,1,p);
   float font=days>5?9:10,available=cw-11,y=y0+13;int count=0;String rest=x.name;
   while(!rest.isEmpty()&&count<3&&y<y1-7){p.setTextSize(font);int n=Math.max(1,p.breakText(rest,true,available,null));String line=rest.substring(0,n);if(count==2&&n<rest.length())line=cut(rest,available,font);text(c,line,x0+5,y,font,color);rest=rest.substring(n);y+=font+2;count++;}
   if(y+9<y1){text(c,cut(x.start+"–"+x.end,available,8),x0+5,y,8,color);y+=11;}
   if(y+9<y1)text(c,cut(x.room,available,9),x0+5,y,9,color);
  }
  text(c,"上次更新 "+Data.lastSaved(data)+"  ·  点击课程查看详情",12,h-9,9,0xff818ca2);return bitmap;
 }
}
