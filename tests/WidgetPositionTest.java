package cn.local.nufeschedule;
public final class WidgetPositionTest {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  int[] cuts={480,540,600,735,810,1045,1200,1260};
  check(WidgetPosition.anchor(480,1260,0)==480);
  check(WidgetPosition.anchor(480,1260,879)==879);
  check(WidgetPosition.anchor(480,1260,1260)==1259);
  check(WidgetPosition.anchor(480,1260,1439)==1259);
  check(WidgetPosition.target(cuts,420)==0);
  check(WidgetPosition.target(cuts,880)==4);
  check(WidgetPosition.target(cuts,1260)==7);
  check(WidgetPosition.target(cuts,1439)==7);
  check(WidgetPosition.target(new int[0],900)==0);
  check(!WidgetPosition.locate(false,"2026-11-08","2026-11-08",5,5));
  check(WidgetPosition.locate(true,"2026-11-08","2026-11-08",5,5));
  check(WidgetPosition.locate(false,"2026-11-07","2026-11-08",5,5));
  check(WidgetPosition.locate(false,"","2026-11-08",5,5));
  check(WidgetPosition.locate(false,"2026-11-08","2026-11-08",6,5));
  System.out.println("PASS: widget time boundaries and refresh positioning");
 }
}
