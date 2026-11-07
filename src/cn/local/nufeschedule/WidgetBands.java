package cn.local.nufeschedule;
final class WidgetBands {
 static int[] viewportCuts(int total,int anchor,int viewport,int step,int labelHeight){
  // Reserve a label-height before the anchor and at both viewport edges.
  // This is a font-relative safety area, not a device-specific pixel correction.
  int guard=Math.max(1,labelHeight);
  int height=Math.max(2*guard,Math.min(total,viewport-2*guard));
  height=Math.min(total,height);
  int start=Math.max(0,Math.min(total-height,anchor-guard)),end=start+height;
  java.util.TreeSet<Integer> edges=new java.util.TreeSet<>();
  edges.add(0);edges.add(total);edges.add(start);edges.add(end);
  for(int y=Math.max(1,step);y<total;y+=Math.max(1,step))if(y<start||y>end)edges.add(y);
  int[] result=new int[edges.size()];int n=0;for(int y:edges)result[n++]=y;
  return result;
 }
 static int pixelTarget(int[] edges,int pixel){
  for(int n=0;n<edges.length-1;n++)if(pixel<edges[n+1])return n;
  return Math.max(0,edges.length-2);
 }
 static int[] cuts(int first,int last){
  if(last<=first)return new int[]{first};
  int count=(last-first+29)/30;int[] result=new int[count+1];
  for(int n=0;n<count;n++)result[n]=first+n*30;
  result[count]=last;return result;
 }
 static int target(int[] cuts,int minute,float scale,int inset){
  if(cuts.length<2||minute<=cuts[0])return 0;
  if(minute>=cuts[cuts.length-1])return cuts.length-1;
  int pixel=Math.round((minute-cuts[0])*scale)+inset;
  for(int n=0;n<cuts.length-1;n++)if(pixel<Math.round((cuts[n+1]-cuts[0])*scale))return n;
  return cuts.length-1;
 }
 static final class Slice {
  final int top,height,offset;
  Slice(int top,int height,int offset){this.top=top;this.height=height;this.offset=offset;}
 }
 static Slice clip(int rowStart,int rowEnd,int courseStart,int courseEnd){
  int first=Math.max(rowStart,courseStart),last=Math.min(rowEnd,courseEnd);
  return last<=first?null:new Slice(first-rowStart,last-first,courseStart-first);
 }
}
