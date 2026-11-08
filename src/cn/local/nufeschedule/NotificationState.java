package cn.local.nufeschedule;

/** Notification decisions do not depend on Android or a particular phone brand. */
final class NotificationState {
 static boolean shouldPost(String key,String published,String dismissed,boolean active){
  return !key.equals(dismissed)&&(!active||!key.equals(published));
 }
 static long nextStart(long[] starts,long now){
  long result=Long.MAX_VALUE;
  for(long start:starts)if(start>now&&start<result)result=start;
  return result;
 }
}
