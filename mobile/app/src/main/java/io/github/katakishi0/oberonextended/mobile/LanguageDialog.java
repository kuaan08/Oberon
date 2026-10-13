package io.github.katakishi0.oberonextended.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Theme-aware language picker, independent of Android OS language settings. */
public final class LanguageDialog {
    private LanguageDialog(){}
    public static void show(Activity activity){
        MobileUi ui=MobileUi.from(activity);
        Dialog dialog=new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout body=ui.card(true);
        body.setPadding(dp(activity,20),dp(activity,18),dp(activity,20),dp(activity,16));
        body.addView(ui.label("Language / Idioma",23,true,ui.accent));
        body.addView(ui.label("Select a language. Unsupported text falls back to English.",
            13,false,ui.muted),ui.margin(-1,dp(activity,48),7));
        ScrollView scroll=new ScrollView(activity);
        LinearLayout list=new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        final String chosen=L10n.selected(activity);
        for(int i=0;i<L10n.CODES.length;i++){
            final String code=L10n.CODES[i];
            String name=i==0?L10n.titleForSystem(activity):L10n.NAMES[i];
            Button button=ui.button((chosen.equals(code)?"✓  ":"")+name,chosen.equals(code));
            button.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            button.setOnClickListener(v->{
                L10n.set(activity,code);
                dialog.dismiss();
                activity.recreate();
            });
            list.addView(button,ui.margin(-1,dp(activity,48),6));
        }
        scroll.addView(list);
        LinearLayout.LayoutParams space=new LinearLayout.LayoutParams(-1,0,1);
        space.topMargin=dp(activity,12);
        body.addView(scroll,space);
        Button close=ui.button(L10n.t(activity,"Cancelar"),false);
        close.setOnClickListener(v->dialog.dismiss());
        body.addView(close,ui.margin(-1,dp(activity,48),13));
        dialog.setContentView(body);
        Window w=dialog.getWindow();
        if(w!=null){
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=w.getAttributes();
            params.width=Math.min((int)(activity.getResources().getDisplayMetrics().widthPixels*.92),
                dp(activity,500));
            params.height=Math.min((int)(activity.getResources().getDisplayMetrics().heightPixels*.82),
                dp(activity,620));
            params.gravity=Gravity.CENTER;params.dimAmount=.74f;
            w.setAttributes(params);
        }
        dialog.show();
        if(w!=null)w.setLayout(
            Math.min((int)(activity.getResources().getDisplayMetrics().widthPixels*.92),dp(activity,500)),
            Math.min((int)(activity.getResources().getDisplayMetrics().heightPixels*.82),dp(activity,620)));
    }
    private static int dp(Activity a,float v){return MobileUi.dp(a,v);}
}
