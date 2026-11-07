package cn.local.nufeschedule;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.*;import android.view.*;import android.webkit.*;import android.widget.*;import org.json.*;import java.io.*;

public class SchoolActivity extends Activity {
 static final String PAGE="https://jwxt.nufe.edu.cn/student/for-std/course-table";
 WebView web;TextView status;Button read;Handler handler=new Handler();int attempt=0,serial=0;boolean reading=false;
 boolean school(String url){try{Uri u=Uri.parse(url);String host=u.getHost();return "https".equals(u.getScheme())&&host!=null&&(host.equals("nufe.edu.cn")||host.endsWith(".nufe.edu.cn"));}catch(Exception e){return false;}}
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xfff4f6fc);setContentView(root);if(Build.VERSION.SDK_INT>=30)MainActivity.insets(root);
  LinearLayout bar=new LinearLayout(this);root.addView(bar);Button close=new Button(this);close.setText("返回");close.setOnClickListener(v->finish());bar.addView(close);Button course=new Button(this);course.setText("进入课表");course.setOnClickListener(v->{serial++;reading=false;read.setEnabled(true);web.loadUrl(PAGE);});bar.addView(course,new LinearLayout.LayoutParams(0,-2,1));read=new Button(this);read.setText("读取课表");read.setOnClickListener(v->capture());bar.addView(read);
  status=new TextView(this);status.setPadding(14,8,14,8);status.setText("请本人登录学校教务，进入课表后点击“读取课表”。\n账号和验证码只提交给学校，课程保存在本机。");root.addView(status);
  Ui.style(close,false);Ui.style(course,false);Ui.style(read,true);status.setTextSize(12);status.setTextColor(Ui.MUTED);status.setPadding(Ui.dp(this,14),Ui.dp(this,10),Ui.dp(this,14),Ui.dp(this,10));
  web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSupportZoom(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);settings.setSaveFormData(false);settings.setSavePassword(false);CookieManager.getInstance().setAcceptCookie(true);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(!school(r.getUrl().toString())){status.setText("已阻止非学校地址。请在学校官方页面登录。");return true;}return false;}
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){String url=r.getUrl().toString();if(!school(url)&&!url.startsWith("data:"))return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));return null;}
   @Override public void onPageFinished(WebView v,String url){CookieManager.getInstance().flush();if(!reading)status.setText("当前："+Uri.parse(url).getHost()+"\n登录完成后点“进入课表”，再点“读取课表”。");}
   @Override public void onReceivedSslError(WebView v,android.webkit.SslErrorHandler h,android.net.http.SslError e){h.cancel();status.setText("学校连接的证书验证失败，未继续。请检查手机日期、网络或稍后重试。");}
   @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())status.setText("页面无法连接："+e.getDescription()+"。请检查网络或校园访问条件。");}
  });web.loadUrl(PAGE);
 }
 void capture(){if(reading)return;Uri current=Uri.parse(web.getUrl()==null?"":web.getUrl());if(!"jwxt.nufe.edu.cn".equals(current.getHost())||!current.getPath().startsWith("/student/for-std/course-table")){status.setText("请先完成登录并点击“进入课表”。");return;}
  try{String js;try(InputStream in=getAssets().open("school-import.js")){js=Data.read(in);}reading=true;read.setEnabled(false);status.setText("正在从学校读取课表，请勿离开本页…");attempt=0;int id=++serial;web.evaluateJavascript(js,v->handler.postDelayed(()->poll(id),400));}catch(Exception e){fail("无法读取导入脚本");}
 }
 void fail(String s){reading=false;read.setEnabled(true);status.setText("未保存："+s);}
 void poll(int id){if(id!=serial||isFinishing())return;if(++attempt>60){fail("读取超时，请重试。原有本地课表不受影响。");return;}
  web.evaluateJavascript("JSON.stringify(window.__nufeLocalImport || {state:'waiting'})",value->{if(id!=serial||isFinishing())return;try{Object decoded=new JSONTokener(value).nextValue();JSONObject result=new JSONObject(String.valueOf(decoded));String state=result.optString("state");if(state.equals("done")){JSONObject schedule=result.getJSONObject("schedule");Data d=Data.parse(schedule.toString());Intent out=new Intent();out.putExtra("schedule",d.json.toString());setResult(RESULT_OK,out);finish();}else if(state.equals("error"))fail(result.optString("error","读取失败"));else handler.postDelayed(()->poll(id),400);}catch(Exception e){fail(e.getMessage());}});
 }
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override public void onDestroy(){serial++;handler.removeCallbacksAndMessages(null);if(web!=null){web.stopLoading();web.destroy();}super.onDestroy();}
}
