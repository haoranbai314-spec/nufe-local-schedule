package cn.local.nufeschedule;
public final class NotificationStateTest {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  check(NotificationState.shouldPost("lesson-1","","",false));
  check(!NotificationState.shouldPost("lesson-1","lesson-1","",true));
  check(!NotificationState.shouldPost("lesson-1","lesson-1","lesson-1",false));
  check(!NotificationState.shouldPost("lesson-1","lesson-1","lesson-1",true));
  check(NotificationState.shouldPost("lesson-2","lesson-1","lesson-1",false));
  check(NotificationState.shouldPost("lesson-1","lesson-1","",false));
  check(NotificationState.shouldPost("room-changed","lesson-1","",true));
  check(NotificationState.nextStart(new long[]{300,200,200,100},150)==200);
  check(NotificationState.nextStart(new long[]{300,200,100},200)==300);
  check(NotificationState.nextStart(new long[]{300,200,100},300)==Long.MAX_VALUE);
  check(NotificationState.nextStart(new long[0],0)==Long.MAX_VALUE);
  System.out.println("PASS: dismissed card, next class and exact start boundaries");
 }
}
