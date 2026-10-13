package io.github.katakishi0.oberonextended.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Dialogs are drawn using the Extended palette, not Android's gray AlertDialog. */
public final class ExtendedDialog {
    private ExtendedDialog() {}

    public static void info(Activity activity,String title,String message){
        present(activity,title,message,"Entendi",null,null,null);
    }
    public static void confirm(Activity activity,String title,String message,
                               String action,Runnable confirmed){
        present(activity,title,message,action,confirmed,"Cancelar",null);
    }
    public static void choice(Activity activity,String title,String message,
                              String positive,Runnable accepted,
                              String negative,Runnable declined){
        present(activity,title,message,positive,accepted,negative,declined);
    }
    private static void present(Activity activity,String title,String message,
        String positive,Runnable accept,String negative,Runnable decline){
        MobileUi ui=MobileUi.from(activity);
        Dialog dialog=new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout main=ui.card(true);
        main.setPadding(dp(activity,24),dp(activity,22),dp(activity,24),dp(activity,22));
        TextView header=ui.label(title,22,true,ui.accent);
        main.addView(header,new LinearLayout.LayoutParams(-1,-2));

        ScrollView scroll=new ScrollView(activity);
        scroll.setFillViewport(false);
        TextView body=ui.label(message,15,false,ui.text);
        body.setLineSpacing(dp(activity,4),1.10f);
        body.setPadding(0,dp(activity,16),0,dp(activity,12));
        scroll.addView(body);
        main.addView(scroll,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout actions=new LinearLayout(activity);
        actions.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionLp=new LinearLayout.LayoutParams(-1,dp(activity,50));
        actionLp.topMargin=dp(activity,16);
        if(negative!=null){
            Button dismiss=ui.button(negative,false);
            dismiss.setOnClickListener(v->{dialog.dismiss();if(decline!=null)decline.run();});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
            lp.rightMargin=dp(activity,10);
            actions.addView(dismiss,lp);
        }
        Button agree=ui.button(positive,true);
        agree.setOnClickListener(v->{dialog.dismiss();if(accept!=null)accept.run();});
        actions.addView(agree,new LinearLayout.LayoutParams(0,-1,1));
        main.addView(actions,actionLp);

        dialog.setContentView(main);
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=window.getAttributes();
            params.width=(int)Math.min(activity.getResources().getDisplayMetrics().widthPixels*.92,
                dp(activity,540));
            params.height=WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity=Gravity.CENTER;
            params.dimAmount=.72f;
            window.setAttributes(params);
        }
        dialog.show();
        if(window!=null)window.setLayout(
            (int)Math.min(activity.getResources().getDisplayMetrics().widthPixels*.92,dp(activity,540)),
            WindowManager.LayoutParams.WRAP_CONTENT);
    }
    private static int dp(Activity a,float value){return MobileUi.dp(a,value);}
}
