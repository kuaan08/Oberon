package io.github.katakishi0.oberonextended.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Responsive Android home screen. Gamepad touch input lives in TouchActivity. */
public final class MainActivity extends Activity {
    private final InputState input=MobileService.INPUT;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private MobileUi ui;
    private TextView status,ip,device,connectionCount,details;
    private Button connection;
    private boolean autoTouchLaunched;
    private int d(float v){return MobileUi.dp(this,v);}

    private final Runnable updater=new Runnable(){
        @Override public void run(){
            if(status!=null) {
                String raw=MobileService.currentStatus();
                status.setText(statusLabel(raw));
                status.setTextColor(MobileService.connectedClients()>0?ui.accent:ui.muted);
                connectionCount.setText(MobileService.connectedClients()>0?
                    "Xbox conectado":"Aguardando conexão");
                connection.setText(MobileService.running()?"Parar servidor":"Iniciar servidor");
                ip.setText(LanAddress.find()+":26401");
                String detected=detectController();
                device.setText(detected==null?"Nenhum controle detectado":
                    "Conectado: "+detected);
                if(detected!=null)input.setControllerName(detected);
                if(details!=null){
                    InputDevice pad=selectedController();
                    double interval=MobileService.requestIntervalMs();
                    details.setText("Captura: "+(pad==null?"sem controle":InputDevices.family(pad))+
                        "\nBateria: "+(pad==null?"indisponível":InputDevices.battery(pad))+
                        "\nCaptura em segundo plano: "+MobileService.backgroundCaptureStatus()+
                        "\nXbox conectado(s): "+MobileService.connectedClients()+
                        "\nPacotes recebidos/enviados: "+MobileService.requests()+"/"+MobileService.responses()+
                        "\nIntervalo entre pedidos: "+(interval<=0?"aguardando":
                            String.format(java.util.Locale.US,"%.1f ms",interval)));
                }
            }
            handler.postDelayed(this,800);
        }
    };
    private String statusLabel(String value){
        if(!MobileService.running())return "Servidor desligado";
        if(MobileService.connectedClients()>0)return "Xbox conectado";
        if(value.startsWith("Could not"))return "Erro na conexão: "+value;
        return "Servidor pronto";
    }
    private InputDevice selectedController(){
        int chosen=prefs.getInt("selected_controller",-1);
        for(int id:InputDevice.getDeviceIds()){
            InputDevice pad=InputDevice.getDevice(id);
            if(InputDevices.isController(pad)&&(chosen<0||chosen==id))return pad;
        }
        return null;
    }
    private String detectController(){
        InputDevice pad=selectedController();
        return pad==null?null:pad.getName();
    }
    private boolean acceptDevice(int id){
        if(prefs.getBoolean("multi_controller_enabled",true))return true;
        int chosen=prefs.getInt("selected_controller",-1);
        return chosen<0||chosen==id;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences(MobileUi.PREFS,MODE_PRIVATE);
        OrientationPrefs.apply(this,"app_orientation","auto");
        ui=MobileUi.from(this);
        input.setUsePhysical(true);
        input.setDeadzone(prefs.getInt("deadzone",10)/100f);
        ui.applyWindow(this);
        render();
        if(state==null&&!prefs.getBoolean("default_physical",true)){
            autoTouchLaunched=true;
            handler.postDelayed(this::openTouch,270);
        }
    }
    private TextView text(String value,int size,boolean bold,int color) {
        return ui.label(value,size,bold,color);
    }
    private void render(){
        boolean wide=getResources().getConfiguration().orientation==
            Configuration.ORIENTATION_LANDSCAPE;
        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setBackgroundColor(ui.bg);
        scroller.setClipToPadding(false);
        LinearLayout whole=new LinearLayout(this);
        whole.setOrientation(LinearLayout.VERTICAL);
        whole.setPadding(d(18),d(13),d(18),d(25));
        scroller.addView(whole);
        setContentView(scroller);

        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout branding=new LinearLayout(this);
        branding.setOrientation(LinearLayout.VERTICAL);
        TextView oberon=text("OBERON",wide?20:25,true,ui.text);
        TextView extended=text("EXTENDED MOBILE",wide?19:21,true,ui.accent);
        branding.addView(oberon);branding.addView(extended);
        header.addView(branding,new LinearLayout.LayoutParams(0,-2,1f));
        Button settings=ui.button("⚙  Configurações",false);
        settings.setOnClickListener(v->openSettings());
        header.addView(settings,new LinearLayout.LayoutParams(d(wide?160:144),d(49)));
        whole.addView(header,ui.margin(-1,-2,0));

        TextView subtitle=text("Seu celular como controle remoto do Xbox.",13,false,ui.muted);
        whole.addView(subtitle,ui.margin(-1,d(35),2));

        LinearLayout sections=new LinearLayout(this);
        sections.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        whole.addView(sections,ui.margin(-1,-2,8));
        LinearLayout left=new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout right=new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        if(wide){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
            lp.rightMargin=d(7);sections.addView(left,lp);
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);
            rp.leftMargin=d(7);sections.addView(right,rp);
        }else {
            sections.addView(left,new LinearLayout.LayoutParams(-1,-2));
            sections.addView(right,new LinearLayout.LayoutParams(-1,-2));
        }

        LinearLayout serverCard=ui.card(true);
        serverCard.addView(text("CONEXÃO LOCAL",13,true,ui.accent));
        status=text("Servidor desligado",21,true,ui.text);
        serverCard.addView(status,ui.margin(-1,d(42),6));
        connectionCount=text("Aguardando conexão",13,false,ui.muted);
        serverCard.addView(connectionCount);
        serverCard.addView(text("Endereço para inserir no Xbox",13,false,ui.muted),
            ui.margin(-1,d(29),12));
        ip=text(LanAddress.find()+":26401",21,true,ui.text);
        ip.setTextIsSelectable(true);
        serverCard.addView(ip,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout actions=new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button copy=ui.button("Copiar IP",false);
        copy.setOnClickListener(v->{
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE))
                .setPrimaryClip(ClipData.newPlainText("Oberon IP",LanAddress.find()));
            android.widget.Toast.makeText(this,"IP copiado",android.widget.Toast.LENGTH_SHORT).show();
        });
        actions.addView(copy,new LinearLayout.LayoutParams(0,d(48),1));
        connection=ui.button("Iniciar servidor",true);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,d(48),1.5f);
        cp.leftMargin=d(9);actions.addView(connection,cp);
        connection.setOnClickListener(v->toggleServer());
        serverCard.addView(actions,ui.margin(-1,d(50),14));
        left.addView(serverCard,ui.margin(-1,-2,6));

        LinearLayout gamepadCard=ui.card(true);
        gamepadCard.addView(text("BLUETOOTH / USB",13,true,ui.accent));
        gamepadCard.addView(text("Controle físico",20,true,ui.text),
            ui.margin(-1,d(38),5));
        device=text("Procurando controles...",14,false,ui.muted);
        device.setMaxLines(3);
        gamepadCard.addView(device,ui.margin(-1,-2,2));
        gamepadCard.addView(text("Captura normal: enquanto o aplicativo está aberto. Em aparelhos com root, é possível ativar captura em segundo plano nas configurações experimentais.",13,false,ui.muted),
            ui.margin(-1,-2,11));
        right.addView(gamepadCard,ui.margin(-1,-2,6));
        if(prefs.getBoolean("detailed_mode",false)){
            LinearLayout detailCard=ui.card(true);
            detailCard.addView(text("MODO DETALHADO",13,true,ui.accent));
            details=text("Aguardando dados...",13,false,ui.text);
            details.setLineSpacing(d(3),1.05f);
            detailCard.addView(details,ui.margin(-1,-2,8));
            Button diagnostic=ui.button("Abrir diagnóstico completo",false);
            diagnostic.setOnClickListener(v->startActivity(
                new Intent(this,DiagnosticsActivity.class)));
            detailCard.addView(diagnostic,ui.margin(-1,d(48),12));
            right.addView(detailCard,ui.margin(-1,-2,6));
        }

        LinearLayout touchCard=ui.card(true);
        touchCard.addView(text("CONTROLE NA TELA",13,true,ui.accent));
        touchCard.addView(text("Jogue usando o toque",20,true,ui.text),
            ui.margin(-1,d(36),6));
        touchCard.addView(text("Abra os controles em tela cheia, na vertical ou horizontal. Vários dedos podem ser usados ao mesmo tempo.",13,false,ui.muted));
        Button open=ui.button("Abrir controle virtual  ↗",true);
        open.setOnClickListener(v->openTouch());
        touchCard.addView(open,ui.margin(-1,d(54),14));
        right.addView(touchCard,ui.margin(-1,-2,6));

        LinearLayout helper=ui.card(true);
        helper.addView(text("COMO CONECTAR",13,true,ui.accent));
        helper.addView(text("1. Coloque o celular e o Xbox na mesma rede Wi-Fi.\n2. Inicie o servidor aqui.\n3. Abra o Oberon original no Xbox.\n4. Digite o IP do celular e conecte.",14,false,ui.text),
            ui.margin(-1,-2,8));
        Button help=ui.button("Ajuda e explicações",false);
        help.setOnClickListener(v->help());
        helper.addView(help,ui.margin(-1,d(45),13));
        left.addView(helper,ui.margin(-1,-2,7));
        LinearLayout utilities=ui.card(true);
        utilities.addView(text("FERRAMENTAS",13,true,ui.accent));
        LinearLayout shortcuts=new LinearLayout(this);
        Button diagnostics=ui.button("Diagnóstico",false);
        diagnostics.setOnClickListener(v->startActivity(
            new Intent(this,DiagnosticsActivity.class)));
        shortcuts.addView(diagnostics,new LinearLayout.LayoutParams(0,d(48),1));
        Button updates=ui.button("Atualizações",false);
        updates.setOnClickListener(v->startActivity(
            new Intent(this,UpdatesActivity.class)));
        LinearLayout.LayoutParams udp=new LinearLayout.LayoutParams(0,d(48),1);
        udp.leftMargin=d(8);shortcuts.addView(updates,udp);
        utilities.addView(shortcuts,ui.margin(-1,d(50),10));
        left.addView(utilities,ui.margin(-1,-2,7));

        TextView foot=text("Oberon Extended Mobile  ·  Alpha 4  ·  Rede local",12,false,ui.muted);
        foot.setGravity(Gravity.CENTER);
        whole.addView(foot,ui.margin(-1,d(43),2));

        if(prefs.getBoolean("animations",true)) {
            sections.setAlpha(0);sections.setTranslationY(d(18));
            sections.animate().alpha(1).translationY(0).setDuration(290).start();
        }
    }
    private void toggleServer(){
        if(MobileService.running()){
            startService(new Intent(this,MobileService.class).setAction(MobileService.ACTION_STOP));
        }else{
            if(LanAddress.find().startsWith("No local")){
                android.widget.Toast.makeText(this,"Conecte ao Wi-Fi local primeiro",android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent=new Intent(this,MobileService.class).setAction(MobileService.ACTION_START);
            if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);
            else startService(intent);
        }
        handler.postDelayed(updater,200);
    }
    private void openTouch() {
        input.reset();
        startActivity(new Intent(this,TouchActivity.class));
        MobileUi.enter(this);
    }
    private void openSettings() {
        startActivity(new Intent(this,SettingsActivity.class));
        MobileUi.enter(this);
    }
    private void help(){
        ExtendedDialog.info(this,"Como conectar ao Xbox",
            "1. Conecte o celular e o Xbox à mesma rede Wi-Fi.\n\n"+
            "2. Toque em Iniciar servidor.\n\n"+
            "3. Abra o Oberon Remote Input original no Xbox.\n\n"+
            "4. Digite o IP mostrado aqui na porta 26401.\n\n"+
            "5. Use um controle Bluetooth/USB ou abra o controle virtual.\n\n"+
            "O aplicativo receptor no Xbox continua sendo necessário.");
    }
    @Override protected void onResume(){
        super.onResume();
        OrientationPrefs.apply(this,"app_orientation","auto");
        input.setUsePhysical(true);
        MobileUi next=MobileUi.from(this);
        if(!next.theme.equals(ui.theme)){recreate();return;}
        handler.removeCallbacks(updater);
        handler.post(updater);
    }
    @Override protected void onPause(){
        handler.removeCallbacks(updater);
        // Normal Android capture stops when this Activity loses focus.
        // With the explicitly enabled ROOT reader, do not erase its live
        // physical input state when opening another app.
        if(MobileService.backgroundCaptureRequested()){
            // A root reader still starting must not leave a held UI button stuck.
            if(!MobileService.backgroundCapturing())input.clearPhysical();
        }else input.reset();
        super.onPause();
    }
    private boolean gamepadSource(int source){
        return (source&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD ||
            (source&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK ||
            (source&InputDevice.SOURCE_DPAD)==InputDevice.SOURCE_DPAD;
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(MobileService.backgroundCapturing())return super.dispatchKeyEvent(e);
        if(gamepadSource(e.getSource()) && acceptDevice(e.getDeviceId())){
            int bit=mapButton(e.getKeyCode());
            if(prefs.getBoolean("experimental_enabled",false) &&
                prefs.getBoolean("experimental_swap_ab",false)){
                if(bit==InputState.A)bit=InputState.B;
                else if(bit==InputState.B)bit=InputState.A;
            }
            if(bit!=0){
                if(e.getDevice()!=null)input.setControllerName(e.getDevice().getName());
                InputState pad=MobileService.CONTROLLERS==null?input:
                    MobileService.CONTROLLERS.forDevice(e.getDeviceId());
                if(pad==null)return super.dispatchKeyEvent(e);
                pad.setPhysicalButton(bit,e.getAction()==KeyEvent.ACTION_DOWN);
                return true;
            }
            if(e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_L2 ||
                e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_R2) {
                InputState pad=MobileService.CONTROLLERS==null?input:
                    MobileService.CONTROLLERS.forDevice(e.getDeviceId());
                if(pad==null)return super.dispatchKeyEvent(e);
                pad.setPhysicalDigitalTrigger(e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_L2,
                    e.getAction()==KeyEvent.ACTION_DOWN?1:0);
                return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }
    private static int mapButton(int key) {
        switch(key) {
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
        if(MobileService.backgroundCapturing())return super.onGenericMotionEvent(e);
        if((e.getSource()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK &&
            e.getAction()==MotionEvent.ACTION_MOVE && acceptDevice(e.getDeviceId())) {
            if(e.getDevice()!=null)input.setControllerName(e.getDevice().getName());
            InputDevice source=e.getDevice();
            boolean experimental=prefs.getBoolean("experimental_enabled",false);
            boolean alternate=experimental&&prefs.getBoolean("experimental_axes_rxry",false);
            float rx=alternate?axis(e,source,MotionEvent.AXIS_RX,MotionEvent.AXIS_Z):
                axis(e,source,MotionEvent.AXIS_Z,MotionEvent.AXIS_RX);
            float ry=alternate?axis(e,source,MotionEvent.AXIS_RY,MotionEvent.AXIS_RZ):
                axis(e,source,MotionEvent.AXIS_RZ,MotionEvent.AXIS_RY);
            float lt=Math.max(e.getAxisValue(MotionEvent.AXIS_LTRIGGER),
                e.getAxisValue(MotionEvent.AXIS_BRAKE));
            float rt=Math.max(e.getAxisValue(MotionEvent.AXIS_RTRIGGER),
                e.getAxisValue(MotionEvent.AXIS_GAS));
            boolean invert=experimental&&prefs.getBoolean("experimental_invert_y",false);
            InputState pad=MobileService.CONTROLLERS==null?input:
                MobileService.CONTROLLERS.forDevice(e.getDeviceId());
            if(pad==null)return super.onGenericMotionEvent(e);
            pad.setPhysicalAxes(e.getAxisValue(MotionEvent.AXIS_X),
                e.getAxisValue(MotionEvent.AXIS_Y)*(invert?-1:1),
                rx,ry*(invert?-1:1),lt,rt);
            pad.setPhysicalHat(e.getAxisValue(MotionEvent.AXIS_HAT_X),
                e.getAxisValue(MotionEvent.AXIS_HAT_Y));
            return true;
        }
        return super.onGenericMotionEvent(e);
    }
    private static float axis(MotionEvent e,InputDevice device,int primary,int fallback){
        return device!=null&&device.getMotionRange(primary,InputDevice.SOURCE_JOYSTICK)!=null
            ?e.getAxisValue(primary):e.getAxisValue(fallback);
    }
}
