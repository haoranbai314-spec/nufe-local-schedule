package cn.local.nufeschedule;
import android.content.Context;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.widget.*;
final class Ui {
 static final int INK=0xff26334b,MUTED=0xff7a879b,ACCENT=0xff5264c6,SURFACE=0xfff3f5fa;
 static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static void style(Button button,boolean primary){button.setAllCaps(false);button.setTextSize(14);button.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));button.setTextColor(primary?0xffffffff:0xff52617a);button.setBackgroundResource(primary?R.drawable.primary_press:R.drawable.soft_press);button.setBackgroundTintList(null);button.setStateListAnimator(null);button.setMinHeight(dp(button.getContext(),44));button.setMinimumHeight(dp(button.getContext(),44));button.setMinWidth(0);button.setMinimumWidth(0);button.setPadding(dp(button.getContext(),12),dp(button.getContext(),8),dp(button.getContext(),12),dp(button.getContext(),8));}
 static void add(LinearLayout row,Button button){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(row.getContext(),44),1);p.setMargins(dp(row.getContext(),3),dp(row.getContext(),4),dp(row.getContext(),3),dp(row.getContext(),4));row.addView(button,p);}
 static GradientDrawable card(Context c){GradientDrawable bg=new GradientDrawable();bg.setColor(0xffffffff);bg.setCornerRadius(dp(c,16));bg.setStroke(dp(c,1),0xffe7ebf3);return bg;}
 static int courseLayout(String name){int[] ids={R.layout.widget_block_0,R.layout.widget_block_1,R.layout.widget_block_2,R.layout.widget_block_3,R.layout.widget_block_4,R.layout.widget_block_5,R.layout.widget_block_6};return ids[Math.floorMod(name.hashCode(),ids.length)];}
 static CharSequence courseLabel(Data.Course c){String text=WidgetTimeline.label(c);android.text.SpannableString label=new android.text.SpannableString(text);label.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),0,c.name.length(),33);if(c.name.length()<text.length())label.setSpan(new android.text.style.ForegroundColorSpan(0xff60708a),c.name.length(),text.length(),33);return label;}
}
