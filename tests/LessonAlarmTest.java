package cn.local.nufeschedule;

public final class LessonAlarmTest {
 static final long BOUNDARY=1793692800000L;
 static final class Fake implements LessonAlarm.Driver {
  boolean allowed, revoke, failCheck, failFallback;
  int exactCalls,inexactCalls; long scheduled=-1;
  public boolean exactAllowed(){if(failCheck)throw new SecurityException();return allowed;}
  public void exact(long time){exactCalls++;if(revoke)throw new SecurityException();scheduled=time;}
  public void inexact(long time){inexactCalls++;if(failFallback)throw new IllegalStateException("unavailable");scheduled=time;}
 }
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  Fake f=new Fake();f.allowed=true;LessonAlarm.schedule(f,BOUNDARY);
  check(f.exactCalls==1&&f.inexactCalls==0&&f.scheduled==BOUNDARY);
  f=new Fake();LessonAlarm.schedule(f,BOUNDARY);
  check(f.exactCalls==0&&f.inexactCalls==1&&f.scheduled==BOUNDARY);
  f=new Fake();f.allowed=true;f.revoke=true;LessonAlarm.schedule(f,BOUNDARY);
  check(f.exactCalls==1&&f.inexactCalls==1&&f.scheduled==BOUNDARY);
  f=new Fake();f.failCheck=true;LessonAlarm.schedule(f,BOUNDARY);
  check(f.exactCalls==0&&f.inexactCalls==1&&f.scheduled==BOUNDARY);
  f=new Fake();LessonAlarm.schedule(f,BOUNDARY);LessonAlarm.schedule(f,BOUNDARY+3600000);
  check(f.inexactCalls==2&&f.scheduled==BOUNDARY+3600000);
  f=new Fake();f.failFallback=true;boolean failed=false;
  try{LessonAlarm.schedule(f,BOUNDARY);}catch(IllegalStateException expected){failed=true;}
  check(failed); // Do not silently claim success when no alarm was scheduled.
  System.out.println("PASS: 6 scheduling regression cases");
 }
}
