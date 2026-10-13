package io.github.katakishi0.oberonextended.mobile;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Full-screen controller with one-tap return, independent of the dashboard. */
public final class TouchActivity extends Activity {
    private VirtualGamepadView pad;
    private InputState input=MobileService.INPUT;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs=getSharedPreferences(MobileUi.PREFS,MODE_PRIVATE);
        String orientation=prefs.getString("touch_orientation","landscape");
        OrientationPrefs.apply(this,"touch_orientation","landscape");
        MobileUi.immersive(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        MobileUi ui=MobileUi.from(this);
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(ui.bg);
        pad=new VirtualGamepadView(this,input);
        pad.setPalette(ui);
        pad.setHaptics(prefs.getBoolean("haptics",true));
        pad.setAnimations(prefs.getBoolean("animations",true));
        pad.setScale(prefs.getInt("touch_scale",100)/100f);
        pad.setIndividualSizes(prefs.getInt("touch_stick_scale",100)/100f,
            prefs.getInt("touch_face_scale",100)/100f);
        pad.setOpacity(prefs.getInt("touch_opacity",100));
        root.addView(pad,new FrameLayout.LayoutParams(-1,-1));

        // The back control stays above the pad in both orientations and is
        // deliberately outside the gamepad's active control regions.
        Button back=ui.button("‹  Voltar",false);
        FrameLayout.LayoutParams backPos=new FrameLayout.LayoutParams(
            MobileUi.dp(this,111),MobileUi.dp(this,43),Gravity.TOP|Gravity.START);
        backPos.leftMargin=MobileUi.dp(this,14);
        backPos.topMargin=MobileUi.dp(this,11);
        root.addView(back,backPos);
        back.setOnClickListener(v->finish());

        LinearLayout status=new LinearLayout(this);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setGravity(Gravity.END);
        TextView brand=ui.label("OBERON  EXTENDED MOBILE",12,true,ui.accent);
        brand.setGravity(Gravity.RIGHT);
        status.addView(brand);
        TextView help=ui.label("CONTROLE VIRTUAL · "+("portrait".equals(orientation)?"VERTICAL":"HORIZONTAL"),11,false,ui.muted);
        help.setGravity(Gravity.RIGHT);
        status.addView(help);
        FrameLayout.LayoutParams statusPos=new FrameLayout.LayoutParams(
            -2,MobileUi.dp(this,45),Gravity.TOP|Gravity.END);
        statusPos.rightMargin=MobileUi.dp(this,16);
        statusPos.topMargin=MobileUi.dp(this,11);
        root.addView(status,statusPos);

        setContentView(root);
        MobileUi.enter(this);
    }
    @Override public void onResume(){
        super.onResume();
        input.reset();
        input.setUsePhysical(false);
    }
    @Override public void onPause(){
        if(pad!=null)pad.release();
        input.reset();
        super.onPause();
    }
    @Override public void finish(){
        if(pad!=null)pad.release();
        input.setUsePhysical(true);
        super.finish();
        MobileUi.exit(this);
    }
    @Override public void onBackPressed(){finish();}
}
