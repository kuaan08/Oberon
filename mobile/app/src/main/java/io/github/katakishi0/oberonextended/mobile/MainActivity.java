package io.github.katakishi0.oberonextended.mobile;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private final InputState input=MobileService.INPUT;
    private SharedPreferences prefs;
    private boolean light;
    private LinearLayout root,layout;
    private TextView status,ip,controllerName;
    private Button connect,touchMode,hardwareMode;
    private VirtualGamepadView gamepadView;
    private TextView physicalInfo;
    private int bg,surface,fg,muted,accent;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){
        @Override public void run(){
            if(status!=null) {
                status.setText(MobileService.currentStatus());
                status.setTextColor(MobileService.connectedClients()>0?accent:muted);
                connect.setText(MobileService.running()?"STOP SERVER":"START SERVER");
                ip.setText("Xbox address: "+LanAddress.find()+":"+OberonProtocol.PORT);
                controllerName.setText(input.usePhysical()?
                    "Gamepad: "+input.lastController():"Virtual touch controller ready");
            }
            handler.postDelayed(this,700);
        }
    };

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        prefs=getSharedPreferences("mobile_settings",MODE_PRIVATE);
        light=prefs.getBoolean("light",false);
        input.setDeadzone(prefs.getInt("deadzone",10)/100f);
        input.setUsePhysical(prefs.getBoolean("physical",false));
        setColors();
        buildUi();
    }
    private void setColors(){
        bg=Color.rgb(light?245:10,light?249:21,light?246:14);
        surface=Color.rgb(light?230:22,light?239:39,light?233:28);
        fg=light?Color.rgb(22,46,30):Color.rgb(236,248,239);
        muted=light?Color.rgb(94,116,101):Color.rgb(151,176,160);
        accent=light?Color.rgb(21,131,66):Color.rgb(113,238,152);
    }
    private int dp(float dp){return Math.round(getResources().getDisplayMetrics().density*dp);}
    private GradientDrawable background(int color,int radius){
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(color);drawable.setCornerRadius(dp(radius));return drawable;
    }
    private TextView text(String t,int sp,int color,boolean bold){
        TextView tv=new TextView(this);tv.setText(t);tv.setTextSize(sp);tv.setTextColor(color);
        if(bold)tv.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        tv.setGravity(Gravity.CENTER_VERTICAL);
        return tv;
    }
    private Button button(String title,boolean primary){
        Button btn=new Button(this);btn.setText(title);btn.setTextSize(11);
        btn.setAllCaps(false);btn.setPadding(dp(10),0,dp(10),0);
        btn.setTextColor(primary?bg:fg);
        btn.setBackground(background(primary?accent:surface,8));
        btn.setMinHeight(dp(40));btn.setMinimumHeight(dp(40));
        return btn;
    }
    private void buildUi(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);root.setPadding(dp(16),dp(9),dp(16),dp(6));
        setContentView(root);
        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("OBERON",18,fg,true);
        TextView extended=text("  EXTENDED MOBILE",18,accent,true);
        titleRow.addView(brand);titleRow.addView(extended);
        View spacer=new View(this);
        titleRow.addView(spacer,new LinearLayout.LayoutParams(0,dp(2),1));
        Button theme=button(light?"Dark theme":"Light theme",false);
        theme.setOnClickListener(v->{prefs.edit().putBoolean("light",!light).apply();recreate();});
        Button help=button("Help",false);
        help.setOnClickListener(v->showHelp());
        titleRow.addView(theme,new LinearLayout.LayoutParams(dp(100),dp(35)));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(68),dp(35));hp.leftMargin=dp(7);
        titleRow.addView(help,hp);
        root.addView(titleRow,new LinearLayout.LayoutParams(-1,dp(38)));

        LinearLayout info=new LinearLayout(this);info.setGravity(Gravity.CENTER_VERTICAL);
        status=text("Waiting for Xbox",13,muted,true);
        info.addView(status,new LinearLayout.LayoutParams(dp(160),dp(35)));
        ip=text("Xbox address: "+LanAddress.find()+":26401",13,accent,true);
        info.addView(ip,new LinearLayout.LayoutParams(0,dp(35),1));
        Button copy=button("Copy IP",false);copy.setOnClickListener(v->{
            ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
                ClipData.newPlainText("Oberon IP",LanAddress.find()));
            Toast.makeText(this,"IP copied",Toast.LENGTH_SHORT).show();
        });
        info.addView(copy,new LinearLayout.LayoutParams(dp(80),dp(37)));
        connect=button("START SERVER",true);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(135),dp(37));cp.leftMargin=dp(8);
        info.addView(connect,cp);
        connect.setOnClickListener(v->toggleServer());
        root.addView(info,new LinearLayout.LayoutParams(-1,dp(44)));

        LinearLayout controlRow=new LinearLayout(this);controlRow.setGravity(Gravity.CENTER_VERTICAL);
        touchMode=button("Touch controls",!input.usePhysical());
        hardwareMode=button("Bluetooth / USB",input.usePhysical());
        touchMode.setOnClickListener(v->setMode(false));
        hardwareMode.setOnClickListener(v->setMode(true));
        controlRow.addView(touchMode,new LinearLayout.LayoutParams(dp(130),dp(35)));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(145),dp(35));bp.leftMargin=dp(7);
        controlRow.addView(hardwareMode,bp);
        controllerName=text("",12,muted,false);
        LinearLayout.LayoutParams label=new LinearLayout.LayoutParams(0,dp(35),1);label.leftMargin=dp(16);
        controlRow.addView(controllerName,label);
        Button settings=button("Settings",false);settings.setOnClickListener(v->settings());
        controlRow.addView(settings,new LinearLayout.LayoutParams(dp(90),dp(35)));
        root.addView(controlRow,new LinearLayout.LayoutParams(-1,dp(43)));

        gamepadView=new VirtualGamepadView(this,input);gamepadView.setLight(light);
        physicalInfo=text("BLUETOOTH / USB GAMEPAD\\n\\nConnect a gamepad to Android, then press a button.\\n\\nAnalog sticks, face buttons, triggers and the D-pad are supported when the Android driver exposes them.",19,fg,true);
        physicalInfo.setGravity(Gravity.CENTER);
        physicalInfo.setBackground(background(surface,14));
        root.addView(gamepadView,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(physicalInfo,new LinearLayout.LayoutParams(-1,0,1));
        TextView footer=text("Local Wi-Fi only  •  Xbox needs the original Oberon receiver  •  Port 26401",10,muted,false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer,new LinearLayout.LayoutParams(-1,dp(23)));
        updateMode();
    }
    private void setMode(boolean physical){
        input.reset();input.setUsePhysical(physical);
        prefs.edit().putBoolean("physical",physical).apply();
        updateMode();
    }
    private void updateMode(){
        if(gamepadView!=null){
            gamepadView.setVisibility(input.usePhysical()?View.GONE:View.VISIBLE);
            if(input.usePhysical())gamepadView.release();
            physicalInfo.setVisibility(input.usePhysical()?View.VISIBLE:View.GONE);
        }
        if(touchMode!=null){
            touchMode.setTextColor(input.usePhysical()?fg:bg);
            touchMode.setBackground(background(input.usePhysical()?surface:accent,8));
            hardwareMode.setTextColor(input.usePhysical()?bg:fg);
            hardwareMode.setBackground(background(input.usePhysical()?accent:surface,8));
        }
    }
    private void toggleServer(){
        if(MobileService.running()) {
            startService(new Intent(this,MobileService.class).setAction(MobileService.ACTION_STOP));
        }else{
            String ipText=LanAddress.find();
            if(ipText.startsWith("No local")){
                Toast.makeText(this,"Connect to the same Wi-Fi as Xbox first",Toast.LENGTH_LONG).show();
                return;
            }
            Intent start=new Intent(this,MobileService.class).setAction(MobileService.ACTION_START);
            if(Build.VERSION.SDK_INT>=26)startForegroundService(start);
            else startService(start);
        }
        // The lifecycle ticker already refreshes status while visible.
    }
    private void showHelp(){
        new AlertDialog.Builder(this)
            .setTitle("Connect Android to your Xbox")
            .setMessage("1. Connect your phone and Xbox to the same Wi-Fi.\n\n"+
                "2. Tap START SERVER here.\n\n"+
                "3. Open the original Oberon Remote Input app on your Xbox.\n\n"+
                "4. Enter the IP shown at the top of this screen, with port 26401.\n\n"+
                "5. Tap Connect on Xbox. Use the on-screen gamepad or select Bluetooth / USB.\n\n"+
                "The Xbox receiver is still required. This app does not inject input into the Xbox by itself.")
            .setPositiveButton("Got it",null).show();
    }
    private void settings(){
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(25),dp(8),dp(25),dp(8));
        TextView label=text("Analog stick deadzone",15,fg,true);content.addView(label);
        SeekBar seek=new SeekBar(this);seek.setMax(35);seek.setProgress(prefs.getInt("deadzone",10));
        content.addView(seek);
        TextView level=text("Deadzone: "+seek.getProgress()+"%",13,muted,false);
        content.addView(level);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                input.setDeadzone(progress/100f);
                level.setText("Deadzone: "+progress+"%");
                prefs.edit().putInt("deadzone",progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar){}
            @Override public void onStopTrackingTouch(SeekBar bar){}
        });
        TextView notes=text("Physical controllers are read while this app is in the foreground. "+
            "The foreground service keeps the local Xbox WebSocket connection open.",12,muted,false);
        notes.setPadding(0,dp(14),0,dp(6));content.addView(notes);
        new AlertDialog.Builder(this).setTitle("Oberon Extended Mobile")
            .setView(content)
            .setNeutralButton("Restore defaults",(d,w)->{
                prefs.edit().clear().apply();input.setDeadzone(.10f);input.reset();recreate();
            }).setPositiveButton("Close",null).show();
    }
    @Override protected void onResume(){super.onResume();handler.post(ticker);}
    @Override protected void onPause(){
        handler.removeCallbacks(ticker);
        if(gamepadView!=null)gamepadView.release();
        input.reset();super.onPause();
    }
    private boolean isGamepad(KeyEvent e){
        int source=e.getSource();
        return ((source&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD)||
            ((source&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(isGamepad(e)){
            int bit=mapButton(e.getKeyCode());
            if(bit!=0){
                if(e.getDevice()!=null)input.setControllerName(e.getDevice().getName());
                input.setPhysicalButton(bit,e.getAction()==KeyEvent.ACTION_DOWN);
                return true;
            }
            if(e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_L2 || e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_R2){
                if(e.getAction()==KeyEvent.ACTION_DOWN||e.getAction()==KeyEvent.ACTION_UP){
                    float p=e.getAction()==KeyEvent.ACTION_DOWN?1:0;
                    input.setPhysicalDigitalTrigger(e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_L2,p);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(e);
    }
    private static int mapButton(int key){
        switch(key){
            case KeyEvent.KEYCODE_BUTTON_A:return InputState.A;
            case KeyEvent.KEYCODE_BUTTON_B:return InputState.B;
            case KeyEvent.KEYCODE_BUTTON_X:return InputState.X;
            case KeyEvent.KEYCODE_BUTTON_Y:return InputState.Y;
            case KeyEvent.KEYCODE_BUTTON_SELECT:return InputState.VIEW;
            case KeyEvent.KEYCODE_BUTTON_START:return InputState.START;
            case KeyEvent.KEYCODE_BUTTON_MODE:return InputState.GUIDE;
            case KeyEvent.KEYCODE_BUTTON_L1:return InputState.LB;
            case KeyEvent.KEYCODE_BUTTON_R1:return InputState.RB;
            case KeyEvent.KEYCODE_BUTTON_THUMBL:return InputState.L3;
            case KeyEvent.KEYCODE_BUTTON_THUMBR:return InputState.R3;
            case KeyEvent.KEYCODE_DPAD_UP:return InputState.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:return InputState.DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT:return InputState.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:return InputState.RIGHT;
            default:return 0;
        }
    }
    @Override public boolean onGenericMotionEvent(MotionEvent e){
        if((e.getSource()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK &&
            e.getAction()==MotionEvent.ACTION_MOVE){
            if(e.getDevice()!=null)input.setControllerName(e.getDevice().getName());
            InputDevice dev=e.getDevice();
            float rx=axis(e,dev,MotionEvent.AXIS_Z,MotionEvent.AXIS_RX);
            float ry=axis(e,dev,MotionEvent.AXIS_RZ,MotionEvent.AXIS_RY);
            float lt=Math.max(e.getAxisValue(MotionEvent.AXIS_LTRIGGER),e.getAxisValue(MotionEvent.AXIS_BRAKE));
            float rt=Math.max(e.getAxisValue(MotionEvent.AXIS_RTRIGGER),e.getAxisValue(MotionEvent.AXIS_GAS));
            input.setPhysicalAxes(e.getAxisValue(MotionEvent.AXIS_X),
                e.getAxisValue(MotionEvent.AXIS_Y),rx,ry,lt,rt);
            input.setPhysicalHat(e.getAxisValue(MotionEvent.AXIS_HAT_X),
                e.getAxisValue(MotionEvent.AXIS_HAT_Y));
            return true;
        }
        return super.onGenericMotionEvent(e);
    }
    private static float axis(MotionEvent e,InputDevice d,int primary,int fallback){
        return d!=null&&d.getMotionRange(primary,InputDevice.SOURCE_JOYSTICK)!=null
            ?e.getAxisValue(primary):e.getAxisValue(fallback);
    }
}
