package io.github.katakishi0.oberonextended.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared Android-native color system and UI widgets. */
public final class MobileUi {
    public static final String PREFS = "mobile_settings";
    public static final String[] THEMES = {
        "Xbox Green", "Xbox Light", "Midnight", "Graphite", "Violet", "Ocean"
    };
    public final int bg, panel, surface, text, muted, accent, accentText, border;
    public final String theme;
    private final Context context;

    private MobileUi(Context ctx, String name) {
        context = ctx;
        theme = name;
        switch(name) {
            case "Xbox Light":
                bg=rgb(242,248,243); panel=rgb(255,255,255); surface=rgb(227,238,229);
                text=rgb(27,49,34); muted=rgb(91,112,97); accent=rgb(24,138,70);
                accentText=Color.WHITE; border=rgb(193,216,199); break;
            case "Midnight":
                bg=rgb(11,15,29); panel=rgb(25,31,52); surface=rgb(38,46,73);
                text=rgb(242,245,255); muted=rgb(160,173,198); accent=rgb(112,167,255);
                accentText=rgb(10,17,33);border=rgb(64,81,112);break;
            case "Graphite":
                bg=rgb(16,18,20); panel=rgb(30,34,38); surface=rgb(44,50,55);
                text=rgb(248,250,252); muted=rgb(163,173,180); accent=rgb(186,225,202);
                accentText=rgb(14,28,19);border=rgb(65,73,77);break;
            case "Violet":
                bg=rgb(19,15,31); panel=rgb(35,25,54); surface=rgb(52,39,76);
                text=rgb(251,247,255); muted=rgb(186,170,209); accent=rgb(193,142,255);
                accentText=rgb(30,14,45);border=rgb(84,64,116);break;
            case "Ocean":
                bg=rgb(8,23,29); panel=rgb(16,39,49); surface=rgb(24,56,67);
                text=rgb(236,250,253); muted=rgb(151,191,201); accent=rgb(89,221,224);
                accentText=rgb(4,32,37);border=rgb(51,88,99);break;
            default:
                bg=rgb(9,20,14);panel=rgb(20,39,28);surface=rgb(27,49,36);
                text=rgb(236,249,239);muted=rgb(157,182,164);accent=rgb(111,238,151);
                accentText=rgb(7,28,14);border=rgb(54,88,65);break;
        }
    }
    public static MobileUi from(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new MobileUi(context, prefs.getString("theme", "Xbox Green"));
    }
    public void applyWindow(Activity activity) {
        activity.getWindow().setStatusBarColor(bg);
        activity.getWindow().setNavigationBarColor(bg);
        int flags="Xbox Light".equals(theme)
            ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            : 0;
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
    }
    private static int rgb(int r,int g,int b){return Color.rgb(r,g,b);}
    public static int dp(Context c,float value) {
        return Math.round(c.getResources().getDisplayMetrics().density*value);
    }
    public GradientDrawable shape(int color,int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);shape.setCornerRadius(dp(context,radius));
        return shape;
    }
    public TextView label(String value,int sp,boolean strong,int color) {
        TextView textView=new TextView(context);
        textView.setText(value);textView.setTextSize(sp);textView.setTextColor(color);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        if(strong)textView.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        return textView;
    }
    public Button button(String label,boolean primary) {
        Button button=new Button(context);
        button.setText(label);button.setTextSize(13);button.setAllCaps(false);
        button.setTextColor(primary?accentText:text);
        button.setBackground(shape(primary?accent:surface,12));
        button.setPadding(dp(context,12),0,dp(context,12),0);
        button.setMinHeight(dp(context,46));
        button.setMinimumHeight(dp(context,46));
        animatePress(button);
        return button;
    }
    public LinearLayout card(boolean vertical) {
        LinearLayout card=new LinearLayout(context);
        card.setOrientation(vertical?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        card.setPadding(dp(context,18),dp(context,16),dp(context,18),dp(context,16));
        card.setBackground(shape(panel,18));
        return card;
    }
    public LinearLayout.LayoutParams margin(int width,int height,int top) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(width,height);
        p.topMargin=dp(context,top);return p;
    }
    public static void animatePress(View view) {
        view.setOnTouchListener((v,event)->{
            if(!v.isEnabled())return false;
            if(!v.getContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getBoolean("animations",true))return false;
            if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(.965f).scaleY(.965f).setDuration(95).start();
            } else if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP ||
                      event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1).scaleY(1).setDuration(155).start();
            }
            return false;
        });
    }
    public static void enter(Activity activity) {
        if(activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean("animations",true))
            activity.overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }
    public static void exit(Activity activity) {
        if(activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean("animations",true))
            activity.overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }
    public static void immersive(Activity activity) {
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        activity.getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
}
