package cn.local.nufeschedule;
import android.content.Context;import android.text.*;import java.time.LocalDate;import java.util.*;

/** 跨各天共用的时间分段；课块按完整文字测量高度，空闲时段压缩。 */
final class WidgetTimeline {
 int days=5;int[] times;float[] heights;float density,font;int needed;
 static String label(Data.Course c){return c.name+"\n"+c.start+"–"+c.end+"\n"+(c.room.isEmpty()?"教室未提供":c.room)+"\n"+(c.teacher.isEmpty()?"教师未提供":c.teacher);}
 WidgetTimeline(Context context,Data data,LocalDate monday,int widthDp,int availablePixels){
  density=context.getResources().getDisplayMetrics().density;TreeSet<Integer> knots=new TreeSet<>();List<Data.Course> all=new ArrayList<>();
  for(int n=0;n<7;n++)for(Data.Course c:data.on(monday.plusDays(n))){all.add(c);knots.add(c.startMinutes());knots.add(c.endMinutes());if(n>=5)days=n+1;}
  if(knots.isEmpty()){knots.add(8*60);knots.add(18*60);}knots.add(knots.first()/60*60);knots.add((knots.last()+59)/60*60);times=new int[knots.size()];int index=0;for(int t:knots)times[index++]=t;
  heights=new float[times.length-1];boolean[] active=new boolean[heights.length];
  for(int n=0;n<heights.length;n++){for(Data.Course c:all)if(c.startMinutes()<times[n+1]&&c.endMinutes()>times[n]){active[n]=true;break;}heights[n]=Math.max(12,active[n]?(times[n+1]-times[n])*.38f:12)*density;}
  font=days>5?8.5f:9.5f;TextPaint paint=new TextPaint(3);paint.setTextSize(font*context.getResources().getDisplayMetrics().scaledDensity);int width=Math.max(18,Math.round(((widthDp-20-32f)/days-10)*density));
  for(Data.Course c:all){StaticLayout text=StaticLayout.Builder.obtain(label(c),0,label(c).length(),paint,width).setIncludePad(false).setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE).build();float required=text.getHeight()+10*density;float present=0;for(int n=0;n<heights.length;n++)if(times[n]>=c.startMinutes()&&times[n+1]<=c.endMinutes())present+=heights[n];if(required>present){int duration=c.endMinutes()-c.startMinutes();for(int n=0;n<heights.length;n++)if(times[n]>=c.startMinutes()&&times[n+1]<=c.endMinutes())heights[n]+=(required-present)*(times[n+1]-times[n])/duration;}}
  float sum=0;int duration=0;for(int n=0;n<heights.length;n++){sum+=heights[n];if(active[n])duration+=times[n+1]-times[n];}needed=(int)Math.ceil(sum);
  if(availablePixels>sum&&duration>0)for(int n=0;n<heights.length;n++)if(active[n])heights[n]+=(availablePixels-sum)*(times[n+1]-times[n])/duration;
 }
 int position(int time){float pos=0;for(int n=0;n<heights.length;n++){if(time>=times[n+1])pos+=heights[n];else if(time>times[n]){pos+=heights[n]*(time-times[n])/(times[n+1]-times[n]);break;}else break;}return Math.round(pos);}
}
