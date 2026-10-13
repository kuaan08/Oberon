package io.github.katakishi0.oberonextended.mobile;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/** Shows only measurable Android and Oberon server data. */
public final class DiagnosticsActivity extends Activity {
    private MobileUi ui;
    private SharedPreferences prefs;
    private LinearLayout devices;
    private TextView connection,networkStats,phoneBattery;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){
        @Override public void run(){
            if(devices!=null) {
                connection.setText(L10n.t(DiagnosticsActivity.this,(MobileService.running()?"Servidor ativo":"Servidor desligado"))+
                    "  •  "+MobileService.connectedClients()+" conexão(ões) Xbox");
                double interval=MobileService.requestIntervalMs();
                String period=interval>0?
                    String.format(Locale.US,"%.1f ms entre pedidos • %.0f pedidos/s",interval,1000/interval):
                    "Aguardando os primeiros pedidos do Xbox";
                networkStats.setText("Recebidos: "+MobileService.requests()+
                    "\nEnviados: "+MobileService.responses()+
                    "\n"+period+"\n\n"+
                    "Isso é o intervalo entre solicitações, NÃO a latência real até o Xbox. "+
                    "O receptor original não devolve timestamps de confirmação.");
                BatteryManager manager=(BatteryManager)getSystemService(BATTERY_SERVICE);
                int percent=manager==null?-1:manager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY);
                phoneBattery.setText(percent>=0?"Bateria do celular: "+percent+"%":
                    L10n.t(DiagnosticsActivity.this,"Bateria do celular: indisponível"));
                renderDevices();
            }
            handler.postDelayed(this,1700);
        }
    };
    private int d(float value){return MobileUi.dp(this,value);}
    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        OrientationPrefs.apply(this,"app_orientation","auto");
        prefs=getSharedPreferences(MobileUi.PREFS,MODE_PRIVATE);
        ui=MobileUi.from(this);
        ui.applyWindow(this);
        ScrollView page=new ScrollView(this);
        page.setFillViewport(true);page.setBackgroundColor(ui.bg);
        LinearLayout layout=new LinearLayout(this);
        layout.setPadding(d(18),d(18),d(18),d(32));
        layout.setOrientation(LinearLayout.VERTICAL);
        page.addView(layout);setContentView(page);
        Button back=ui.button("‹  Voltar",false);
        back.setOnClickListener(v->finish());
        layout.addView(back,new LinearLayout.LayoutParams(d(112),d(48)));
        layout.addView(ui.label("Diagnóstico detalhado",26,true,ui.text),
            ui.margin(-1,d(62),9));
        layout.addView(ui.label("REDE LOCAL",13,true,ui.accent),
            ui.margin(-1,d(28),6));
        LinearLayout network=ui.card(true);
        connection=ui.label("",18,true,ui.text);network.addView(connection);
        network.addView(ui.label("IP local: "+LanAddress.find()+
            ":26401  •  WebSocket",14,false,ui.muted),ui.margin(-1,-2,8));
        networkStats=ui.label("",14,false,ui.text);
        networkStats.setLineSpacing(d(3),1.05f);
        network.addView(networkStats,ui.margin(-1,-2,14));
        layout.addView(network,ui.margin(-1,-2,6));

        layout.addView(ui.label("DISPOSITIVO ANDROID",13,true,ui.accent),
            ui.margin(-1,d(32),10));
        LinearLayout phone=ui.card(true);
        phoneBattery=ui.label("",15,true,ui.text);
        phone.addView(phoneBattery);
        phone.addView(ui.label("Sistema: Android "+android.os.Build.VERSION.RELEASE+
            "\nModelo: "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL,
            13,false,ui.muted),ui.margin(-1,-2,8));
        layout.addView(phone);

        layout.addView(ui.label("CONTROLES DISPONÍVEIS",13,true,ui.accent),
            ui.margin(-1,d(32),12));
        devices=new LinearLayout(this);devices.setOrientation(LinearLayout.VERTICAL);
        layout.addView(devices);
        TextView note=ui.label("A bateria de controles só aparece quando o driver do Android "+
            "expõe essa informação. Muitos controles genéricos não informam capacidade. "+
            "Selecione o dispositivo preferido em Experimental.",13,false,ui.muted);
        layout.addView(note,ui.margin(-1,-2,12));
    }
    private void renderDevices(){
        devices.removeAllViews();
        int found=0;
        for(int id:InputDevice.getDeviceIds()){
            InputDevice pad=InputDevice.getDevice(id);
            if(pad==null||pad.isVirtual())continue;
            if(!InputDevices.isController(pad))continue;
            found++;
            LinearLayout card=ui.card(true);
            card.addView(ui.label(pad.getName(),19,true,ui.text));
            String family=InputDevices.family(pad);
            card.addView(ui.label(L10n.t(this,"Tipo provável: ")+family+"\n"+
                "ID Android: "+id+"  •  Vendor ID: "+pad.getVendorId()+
                "  •  Product ID: "+pad.getProductId(),13,false,ui.muted),
                ui.margin(-1,-2,7));
            card.addView(ui.label(L10n.t(this,"Bateria: ")+InputDevices.battery(pad),14,true,ui.accent),
                ui.margin(-1,-2,8));
            StringBuilder axes=new StringBuilder();
            for(InputDevice.MotionRange range:pad.getMotionRanges()){
                if(axes.length()>180){axes.append(" ...");break;}
                if(axes.length()>0)axes.append(", ");
                axes.append(InputDevices.axisName(range.getAxis()));
            }
            card.addView(ui.label("Eixos disponíveis: "+
                (axes.length()==0?"nenhum":axes.toString()),13,false,ui.muted),
                ui.margin(-1,-2,7));
            int selected=prefs.getInt("selected_controller",-1);
            card.addView(ui.label(selected<0?"Seleção: automática":
                selected==id?"Selecionado para captura":"Não selecionado",
                12,false,ui.muted));
            devices.addView(card,ui.margin(-1,-2,10));
        }
        if(found==0){
            LinearLayout card=ui.card(true);
            card.addView(ui.label("Nenhum controle Bluetooth/USB detectado pelo Android.",
                15,false,ui.muted));
            devices.addView(card);
        }
    }
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
