package cn.local.nufeschedule;

/** Scheduling policy kept independent of Android for host regression tests. */
final class LessonAlarm {
 interface Driver {
  boolean exactAllowed();
  void exact(long when);
  void inexact(long when);
 }
 static void schedule(Driver driver,long when){
  try{
   if(driver.exactAllowed()){driver.exact(when);return;}
  }catch(SecurityException revoked){
   // The special permission can be revoked between checking and scheduling.
  }
  driver.inexact(when);
 }
}
