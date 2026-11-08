package cn.local.nufeschedule;

final class WidgetPosition {
 static int anchor(int first,int last,int minute){
  return Math.max(first,Math.min(Math.max(first,last-1),minute));
 }
 static int target(int[] cuts,int minute){
  if(cuts.length==0)return 0;
  if(minute>=cuts[cuts.length-1])return cuts.length-1;
  int result=0;
  for(int n=0;n<cuts.length-1;n++)if(cuts[n]<=minute)result=n;
  return result;
 }
 static boolean locate(boolean requested,String shownDay,String today,long revision,long savedRevision){
  return requested||!today.equals(shownDay)||revision!=savedRevision;
 }
}
