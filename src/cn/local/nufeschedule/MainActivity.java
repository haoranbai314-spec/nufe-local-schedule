package cn.local.nufeschedule;

import android.app.*;import android.appwidget.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.net.Uri;import android.os.*;import android.view.*;import android.widget.*;
import org.json.*;import java.io.*;import java.time.*;import java.time.format.DateTimeFormatter;

public class MainActivity extends Activity {
 LinearLayout root,body;Data data;LocalDate viewed=Data.today();Handler handler=new Handler();long lastAutoCheck=-60000;boolean renderedEnabled;String renderedDismissed="";AppUpdate updater;ScrollView mainScroll;int restoreScrollY;Bitmap cachedWeek;String cachedWeekKey="";
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 public static void insets(View root){root.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets x=in.getInsets(WindowInsets.Type.systemBars());v.setPadding(x.left,x.top,x.right,x.bottom);return in;});}
 @Override public void onCreate(Bundle b){super.onCreate(b);updater=new AppUpdate(this);getWindow().setStatusBarColor(0xfff4f6fc);getWindow().setNavigationBarColor(0xfff4f6fc);if(b!=null){try{viewed=LocalDate.parse(b.getString("viewed",Data.today().toString()));}catch(Exception ignored){}restoreScrollY=b.getInt("scroll_y",0);}render();if(b==null)handleIntent(getIntent());}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
 void handleIntent(Intent i){if(i==null||!i.hasExtra("day"))return;try{viewed=LocalDate.parse(i.getStringExtra("day"));render();if(i.hasExtra("course"))for(Data.Course c:data.on(viewed)){if(c.name.equals(i.getStringExtra("course"))&&c.start.equals(i.getStringExtra("start"))&&c.room.equals(i.getStringExtra("room"))){details(viewed,c);break;}}}catch(Exception ignored){}}
 void details(LocalDate date,Data.Course c){LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(24),dp(20),dp(24),dp(12));content.addView(text(date.format(DateTimeFormatter.ofPattern("M 月 d 日"))+" · 第 "+data.week(date)+" 周",13,Ui.ACCENT));TextView name=text(c.name,22,Ui.INK);name.setTypeface(android.graphics.Typeface.DEFAULT,1);content.addView(name);content.addView(text(c.start+" — "+c.end,19,Ui.INK));String[] labels={"教室","教师","上课周次"};String[] values={c.room.isEmpty()?"未提供":c.room,c.teacher.isEmpty()?"未提供":c.teacher,c.weeks.toString()};for(int n=0;n<labels.length;n++){LinearLayout row=new LinearLayout(this);row.setPadding(0,dp(10),0,0);TextView label=text(labels[n],13,Ui.MUTED);row.addView(label,new LinearLayout.LayoutParams(dp(76),-2));TextView value=text(values[n],14,Ui.INK);row.addView(value,new LinearLayout.LayoutParams(0,-2,1));content.addView(row);}ScrollView detailScroll=new ScrollView(this);detailScroll.addView(content);AlertDialog dialog=new AlertDialog.Builder(this).setView(detailScroll).setPositiveButton("完成",null).create();dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(Ui.card(this));}
 @Override public void onResume(){super.onResume();NextLesson.refresh(this);render();boolean installing=getSharedPreferences("app_updates",0).getBoolean("pending_install",false);updater.resumeInstall();long now=SystemClock.elapsedRealtime();if(!installing&&now-lastAutoCheck>=60000){lastAutoCheck=now;handler.postDelayed(()->{if(!isFinishing()&&!isDestroyed()&&hasWindowFocus())updater.checkAutomatically();},800);}}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(4),dp(5),dp(4),dp(5));return t;}
 Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);Ui.style(b,false);b.setOnClickListener(v->action.run());return b;}
 @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("viewed",viewed.toString());out.putInt("scroll_y",mainScroll==null?0:mainScroll.getScrollY());}
 @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(updater!=null)updater.detach();super.onDestroy();}
 @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused&&body!=null&&(renderedEnabled!=NextLesson.enabled(this)||!renderedDismissed.equals(getSharedPreferences("next_lesson",0).getString("dismissed_key",""))))render();}
 void render(){
  int scrollY=mainScroll==null?restoreScrollY:mainScroll.getScrollY();restoreScrollY=0;
  renderedEnabled=NextLesson.enabled(this);renderedDismissed=getSharedPreferences("next_lesson",0).getString("dismissed_key","");
  data=Data.load(this);root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Ui.SURFACE);setContentView(root);
  if(Build.VERSION.SDK_INT>=30)insets(root);
  ScrollView scroll=new ScrollView(this);mainScroll=scroll;scroll.setFillViewport(true);scroll.post(()->scroll.scrollTo(0,scrollY));root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));body=new LinearLayout(this);body.setPadding(dp(16),dp(14),dp(16),dp(30));body.setOrientation(1);scroll.addView(body);
  LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);TextView title=text("南财课表",23,Ui.INK);title.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button settings=button("设置",()->settings());heading.addView(settings,new LinearLayout.LayoutParams(dp(60),dp(44)));Button share=button("分享",()->shareApp());LinearLayout.LayoutParams shareLp=new LinearLayout.LayoutParams(dp(60),dp(44));shareLp.leftMargin=dp(6);heading.addView(share,shareLp);body.addView(heading);
  body.addView(text(data.courses.isEmpty()?"在手机上，收好这一学期。":data.semester+"  ·  "+data.courses.size()+" 条排课  ·  本地保存",12,Ui.MUTED));
  TextView tools=text("常用功能",13,Ui.MUTED);tools.setPadding(dp(4),dp(18),dp(4),dp(8));body.addView(tools);
  LinearLayout firstActions=new LinearLayout(this);LinearLayout secondActions=new LinearLayout(this);
  firstActions.setMinimumHeight(dp(96));secondActions.setMinimumHeight(dp(96));LinearLayout.LayoutParams gridLp=new LinearLayout.LayoutParams(-1,-2);gridLp.bottomMargin=dp(8);body.addView(firstActions,gridLp);
  body.addView(secondActions,new LinearLayout.LayoutParams(-1,-2));
  feature(firstActions,data.courses.isEmpty()?"登录教务导入":"更新课表","读取学校最新排课",()->startActivityForResult(new Intent(this,SchoolActivity.class),10),true);
  feature(firstActions,"添加桌面组件","在桌面查看本周课程",()->pinWidget(),false);
  feature(secondActions,"导入手机日历","按实际周次写入课程",()->CalendarExport.begin(this),false);
  feature(secondActions,"锁屏下一节课",NextLesson.enabled(this)?"已开启 · 点击管理":"未开启 · 点击设置",()->NextLesson.settings(this),false);
  TextView notice=text(NextLesson.status(this),12,Ui.MUTED);notice.setPadding(dp(12),dp(10),dp(12),dp(10));notice.setBackground(Ui.card(this));notice.setOnClickListener(v->NextLesson.settings(this));LinearLayout.LayoutParams noticeLp=new LinearLayout.LayoutParams(-1,-2);noticeLp.topMargin=dp(10);body.addView(notice,noticeLp);
  if(data.courses.isEmpty()){body.addView(text("首次使用\n① 点击“登录教务导入”\n② 在学校页面本人输入账号和验证码\n③ 打开课表，点击顶部“读取课表”\n④ 确认第一周周一日期并保存\n⑤ 添加本 App 的桌面组件",16,0xff374665));body.addView(text("不连接作者网站，不上传课表。日历与通知权限仅在使用相应功能时申请。\n学校登录态只在本机 WebView 内保留。课表导入后离线可用；教务调课需要手动更新。",13,0xff77849a));return;}
  TextView weekHeading=text("课表浏览",16,Ui.INK);weekHeading.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);weekHeading.setPadding(dp(4),dp(24),dp(4),dp(10));body.addView(weekHeading);LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(4),dp(4),dp(4),dp(4));nav.setBackground(Ui.card(this));body.addView(nav,new LinearLayout.LayoutParams(-1,-2));Ui.add(nav,button("上一周",()->{viewed=viewed.minusWeeks(1);render();}));Ui.add(nav,button("回到本周",()->{viewed=Data.today();render();}));Ui.add(nav,button("下一周",()->{viewed=viewed.plusWeeks(1);render();}));
  LocalDate monday=viewed.minusDays(viewed.getDayOfWeek().getValue()-1);TextView range=text("第 "+data.week(viewed)+" 周   "+monday.format(DateTimeFormatter.ofPattern("M/d"))+" — "+monday.plusDays(6).format(DateTimeFormatter.ofPattern("M/d")),14,Ui.INK);range.setPadding(dp(4),dp(12),dp(4),dp(10));body.addView(range);
  ImageView week=new ImageView(this);int width=Math.max(600,getResources().getDisplayMetrics().widthPixels-dp(38));String bitmapKey=monday+"/"+data.json.optLong("saved_at",0)+"/"+width+"/"+Data.today();if(cachedWeek==null||!bitmapKey.equals(cachedWeekKey)){cachedWeek=WeekPainter.draw(data,monday,width,(int)(width*1.35));cachedWeekKey=bitmapKey;}week.setImageBitmap(cachedWeek);week.setAdjustViewBounds(true);body.addView(week,new LinearLayout.LayoutParams(-1,-2));week.setContentDescription("本周课程网格，点击课程查看详情；下方提供逐日课程明细");
  float[] down=new float[2];week.setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_DOWN){down[0]=event.getX();down[1]=event.getY();return true;}if(event.getAction()==MotionEvent.ACTION_UP){if(Math.abs(event.getX()-down[0])+Math.abs(event.getY()-down[1])<dp(12)){WeekPainter.Hit hit=WeekPainter.hit(data,monday,v.getWidth(),v.getHeight(),event.getX(),event.getY());if(hit!=null)details(hit.day,hit.course);v.performClick();}return true;}return true;});
  for(int i=0;i<7;i++){LocalDate day=monday.plusDays(i);String label=new String[]{"周一","周二","周三","周四","周五","周六","周日"}[i];body.addView(text(label+" "+day.format(DateTimeFormatter.ofPattern("MM-dd"))+(day.equals(Data.today())?" · 今天":""),16,0xff344b78));java.util.List<Data.Course> list=data.on(day);if(list.isEmpty())body.addView(text("没有课程",13,0xff8a94a5));for(Data.Course c:list){TextView card=text(c.start+"—"+c.end+"   "+c.name+"\n"+c.room+(c.teacher.isEmpty()?"":" · "+c.teacher),15,0xff354464);GradientDrawable bg=new GradientDrawable();bg.setColor(0xffffffff);bg.setCornerRadius(dp(10));card.setBackground(bg);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setOnClickListener(v->details(day,c));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(6);body.addView(card,lp);}}
 }
 void feature(LinearLayout row,String title,String subtitle,Runnable action,boolean primary){
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setMinimumHeight(dp(96));card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackgroundResource(primary?R.drawable.primary_press:R.drawable.soft_press);card.setClickable(true);card.setFocusable(true);card.setOnClickListener(v->action.run());card.setContentDescription(title+"，"+subtitle);
  TextView name=text(title,15,primary?Color.WHITE:Ui.INK);name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);name.setPadding(0,0,0,dp(6));card.addView(name,new LinearLayout.LayoutParams(-1,-2));
  TextView detail=text(subtitle,12,primary?0xffe6eaff:Ui.MUTED);detail.setPadding(0,0,0,0);card.addView(detail,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(row.getChildCount()==0)lp.rightMargin=dp(4);else lp.leftMargin=dp(4);row.addView(card,lp);
 }
 void shareApp(){Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_SUBJECT,"南财课表");share.putExtra(Intent.EXTRA_TEXT,"南财课表：离线课表、独立桌面组件，支持导入手机日历。安卓手机可在这里下载最新版，打开页面后选择 nufe-local.apk："+System.lineSeparator()+"https://github.com/haoranbai314-spec/nufe-local-schedule/releases/latest");try{startActivity(Intent.createChooser(share,"分享南财课表"));}catch(Exception e){message("无法打开系统分享菜单："+e.getMessage());}}
 void manualWidget(){new AlertDialog.Builder(this).setTitle("手动添加独立课表组件").setMessage("系统可能没有弹出快捷添加窗口。\n\n① 回到桌面，长按空白处\n② 打开“组件／原子组件／小部件”\n③ 找到“南财课表”并拖到桌面\n\n若列表中暂时找不到，先打开本 App 一次，再重新进入组件列表。").setNegativeButton("留在 App",null).setPositiveButton("去桌面",(d,w)->{Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);startActivity(home);Toast.makeText(this,"长按桌面空白处 → 组件 → 南财课表",Toast.LENGTH_LONG).show();}).show();}
 void pinWidget(){AppWidgetManager m=AppWidgetManager.getInstance(this);long requested=System.currentTimeMillis();try{if(!m.isRequestPinAppWidgetSupported()){manualWidget();return;}Intent callback=new Intent(this,ScheduleWidget.class).setAction(ScheduleWidget.PINNED);PendingIntent success=PendingIntent.getBroadcast(this,777,callback,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0));boolean accepted=m.requestPinAppWidget(new ComponentName(this,ScheduleWidget.class),null,success);if(!accepted){manualWidget();return;}Toast.makeText(this,"已请求系统添加；若没有弹窗，可手动从桌面组件列表添加",Toast.LENGTH_LONG).show();handler.postDelayed(()->{if(!isFinishing()&&hasWindowFocus()&&getSharedPreferences("widget",0).getLong("last_pinned",0)<requested)manualWidget();},1800);}catch(Exception e){manualWidget();}}
 void message(String s){new AlertDialog.Builder(this).setMessage(s).setPositiveButton("知道了",null).show();}
 void settings(){String[] items={"校准第一周周一","清除学校登录态（保留课表）","删除本地课表","检查软件更新","隐私与版本","锁屏下一节课","课程切换时间"};new AlertDialog.Builder(this).setTitle("本地设置").setItems(items,(d,which)->{
  if(which==0){if(data.courses.isEmpty()){message("请先导入课表。");return;}preview(data,false);}
  if(which==1)new AlertDialog.Builder(this).setMessage("只清除本 App 的学校登录状态，保留离线课表。继续？").setNegativeButton("取消",null).setPositiveButton("清除",(a,b)->{android.webkit.CookieManager.getInstance().removeAllCookies(value->{android.webkit.CookieManager.getInstance().flush();message("学校登录状态已清除，离线课表仍保留。");});android.webkit.WebStorage.getInstance().deleteAllData();}).show();
  if(which==2)new AlertDialog.Builder(this).setMessage("删除本 App 保存的课表？删除后需要通过学校教务重新导入。").setNegativeButton("取消",null).setPositiveButton("删除",(a,b)->{new android.util.AtomicFile(new File(getFilesDir(),Data.FILE)).delete();ScheduleWidget.updateAll(this);NextLesson.refresh(this);render();}).show();
  if(which==3)updater.check();
  if(which==4)message("南财课表 "+AppUpdate.version(this)+" · 非官方\n\n课表与学校登录\n课表保存在手机本地，离线可查看。学校登录通过本机网页，仅访问 nufe.edu.cn 的 HTTPS 页面。清除学校登录态不会删除课表。\n\n手机日历\n仅在你使用“导入手机日历”时申请日历权限。写入专用本地日历，重复导入会替换该日历内容。更新课表后需再次导入。\n\n锁屏下一节课\n默认关闭，开启时申请通知权限。允许在锁屏显示课程名称、教室和时间；可见内容也受系统隐私设置影响。首次创建时默认无声、无振动，后续遵循系统通知设置。允许“课程切换时间”可减少延迟；系统省电仍可能影响切换。划走本节卡片后，下节或手动刷新再显示。\n\n软件更新与分享\n启动检查更新、下载新版时连接 GitHub，不上传课表。分享仅发送软件介绍和下载链接。\n\n其他说明\n无广告、无统计。教务调课需手动更新，锁屏卡片不能替代正式上课提醒。\n\n开源许可\nGPL-3.0-or-later。\n教务字段解析参考 LokmenoWer/nufe-kebiao。");
  if(which==5)NextLesson.settings(this);
  if(which==6)NextLesson.alarmSettings(this);
 }).show();}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==NextLesson.REQUEST){if(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED){NextLesson.setEnabled(this,true);render();message(NextLesson.status(this));}else message("未授予通知权限，锁屏卡片未开启。可在系统应用设置中允许通知后重试。");}if(request==CalendarExport.REQUEST){boolean granted=results.length==2;for(int result:results)granted &= result==android.content.pm.PackageManager.PERMISSION_GRANTED;if(granted)CalendarExport.begin(this);else message("未授予日历权限，课表仍可正常使用。可在系统应用权限中允许后重试。");}}
 void preview(Data imported,boolean isNew){
  LinearLayout content=new LinearLayout(this);content.setPadding(dp(20),dp(4),dp(20),dp(8));content.setOrientation(1);TextView summary=text(imported.semester+"\n"+imported.courses.size()+" 条排课，保留各自上课周次。\n第一周的周一（YYYY-MM-DD）：",15,0xff354464);content.addView(summary);
  EditText date=new EditText(this);date.setSingleLine();date.setHint("例如 2026-09-07，请核对学校校历");if(imported.monday!=null)date.setText(imported.monday.toString());content.addView(date);TextView error=text("请核对日期，错误日期会让整学期课程错周。",12,0xff8b6471);content.addView(error);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(isNew?"确认导入本地课表":"校准开学日期").setView(content).setNegativeButton("取消",null).setPositiveButton("保存到手机",null).create();dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{LocalDate monday=LocalDate.parse(date.getText().toString().trim());imported.save(this,monday);viewed=Data.today();dialog.dismiss();render();Toast.makeText(this,"课表已保存，断网也可以查看",Toast.LENGTH_LONG).show();}catch(Exception e){error.setText("未保存："+e.getMessage());}}));dialog.show();
 }
 @Override protected void onActivityResult(int req,int res,Intent i){super.onActivityResult(req,res,i);if(res!=RESULT_OK||i==null)return;
  if(req==10){try{preview(Data.parse(i.getStringExtra("schedule")),true);}catch(Exception e){message("导入失败："+e.getMessage());}}
 }
}























