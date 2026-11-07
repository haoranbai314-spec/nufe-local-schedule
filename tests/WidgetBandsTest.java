package cn.local.nufeschedule;
public final class WidgetBandsTest {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  for(int total:new int[]{1200,2300,4200})for(int viewport:new int[]{200,689,1000})for(int label:new int[]{18,36,50,72})for(int anchor=0;anchor<=total;anchor+=17){
 int[] edges=WidgetBands.viewportCuts(total,anchor,viewport,63,label);
 check(edges[0]==0&&edges[edges.length-1]==total);
 for(int n=1;n<edges.length;n++)check(edges[n]>edges[n-1]);
 int row=WidgetBands.pixelTarget(edges,Math.min(total-1,anchor));
 check(edges[row]<=Math.min(total-1,anchor)&&edges[row+1]>Math.min(total-1,anchor));
 if(anchor>=label&&anchor<=total-viewport)check(anchor-edges[row]>=label);
 int covered=0;for(int n=0;n<edges.length-1;n++){
  WidgetBands.Slice part=WidgetBands.clip(edges[n],edges[n+1],35,total-17);
  if(part!=null){check(-part.offset==covered);covered+=part.height;}
 }check(covered==total-52);
}
int first=480,last=1260;int[] cuts=WidgetBands.cuts(first,last);
  check(cuts.length==27&&cuts[0]==first&&cuts[26]==last);
  for(int n=1;n<cuts.length;n++)check(cuts[n]-cuts[n-1]==30);
  for(float density:new float[]{1f,2f,2.75f,3f})for(float unit:new float[]{.72f,1f,2.4f}){
   float scale=unit*density;int inset=Math.round(9*density);
   for(int minute=first;minute<last;minute++){
    int target=WidgetBands.target(cuts,minute,scale,inset);
    int y=Math.round((minute-first)*scale)+inset;
    int begin=Math.round((cuts[target]-first)*scale);
    int end=target==cuts.length-1?begin+Math.round(22*density):Math.round((cuts[target+1]-first)*scale);
    check(y>=begin&&y<end);
   }
   check(WidgetBands.target(cuts,100,scale,inset)==0);
   check(WidgetBands.target(cuts,1439,scale,inset)==cuts.length-1);
   for(int[] course:new int[][]{{480,525},{600,735},{810,895},{910,1045},{1200,1260}}){
    int courseStart=Math.round((course[0]-first)*scale)+inset;
    int courseEnd=Math.round((course[1]-first)*scale)+inset;
    int covered=0;
    for(int n=0;n<cuts.length;n++){
     int begin=Math.round((cuts[n]-first)*scale);
     int end=n==cuts.length-1?begin+Math.round(22*density):Math.round((cuts[n+1]-first)*scale);
     WidgetBands.Slice slice=WidgetBands.clip(begin,end,courseStart,courseEnd);
     if(slice!=null){
      check(slice.top>=0&&slice.height>0&&slice.offset<=0);
      check(begin+slice.top+slice.offset==courseStart);
      check(-slice.offset==covered);
      covered+=slice.height;
     }
    }
    check(covered==courseEnd-courseStart);
   }
  }
  System.out.println("PASS: every minute is inside its scroll target; native card slices have no gaps or rescaling");
 }
}
