package io.github.katakishi0.oberonextended.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** A separate, scrollable settings page that works in portrait and landscape. */
public final class SettingsActivity extends Activity {
    private MobileUi ui;
    private SharedPreferences prefs;
    private LinearLayout content;
    private int d(float v){return MobileUi.dp(this,v);}
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences(MobileUi.PREFS,MODE_PRIVATE);
        OrientationPrefs.apply(this,"app_orientation","auto");
        ui=MobileUi.from(this);
        ui.applyWindow(this);
        draw();
        MobileUi.enter(this);
    }
    private void draw() {
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ui.bg);
        scroll.setClipToPadding(false);
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(d(18),d(18),d(18),d(30));
        scroll.addView(content);
        setContentView(scroll);

        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back=ui.button("‹  Voltar",false);
        back.setOnClickListener(v->finish());
        header.addView(back,new LinearLayout.LayoutParams(d(112),d(45)));
        TextView title=ui.label("  Configurações",23,true,ui.text);
        header.addView(title,new LinearLayout.LayoutParams(0,d(55),1));
        content.addView(header);
        TextView sub=ui.label("Personalize a aparência e os controles do seu jeito.",14,false,ui.muted);
        content.addView(sub,ui.margin(-1,d(40),3));

        section("APARÊNCIA", "Escolha o tema utilizado nas páginas e nos controles virtuais.");
        LinearLayout palette=ui.card(true);
        palette.setPadding(d(12),d(12),d(12),d(12));
        RadioGroup themes=new RadioGroup(this);
        themes.setOrientation(RadioGroup.VERTICAL);
        for(String name:MobileUi.THEMES) {
            RadioButton option=new RadioButton(this);
            option.setText(name);option.setTextSize(15);option.setTextColor(ui.text);
            option.setPadding(d(8),d(5),d(8),d(5));
            option.setButtonTintList(android.content.res.ColorStateList.valueOf(ui.accent));
            option.setChecked(name.equals(prefs.getString("theme","Xbox Green")));
            option.setOnClickListener(v->{
                prefs.edit().putString("theme",name).apply();
                recreate();
            });
            themes.addView(option,new RadioGroup.LayoutParams(-1,d(46)));
        }
        palette.addView(themes);
        content.addView(palette,ui.margin(-1,-2,9));

        section("ORIENTAÇÃO DO APLICATIVO",
            "Escolha como o painel e as configurações aparecem no celular.");
        LinearLayout appCard=ui.card(true);
        addOrientationPicker(appCard,"app_orientation",
            new String[]{"auto","portrait","landscape"},
            new String[]{"Automático","Vertical","Horizontal"},true);
        addNote(appCard,"Essa preferência vale para todo o aplicativo, exceto o controle virtual.");
        content.addView(appCard,ui.margin(-1,-2,9));

        section("TELA DO CONTROLE",
            "O controle virtual tem sua própria orientação, independente do aplicativo.");
        LinearLayout orientation=ui.card(true);
        addOrientationPicker(orientation,"touch_orientation",
            new String[]{"portrait","landscape"},
            new String[]{"Vertical","Horizontal"},false);
        addNote(orientation,
            "O layout escolhido será aplicado quando você abrir o controle em tela cheia.");
        TextView scaleValue=ui.label("Tamanho dos botões: "+prefs.getInt("touch_scale",100)+"%",14,true,ui.text);
        orientation.addView(scaleValue,ui.margin(-1,d(34),7));
        SeekBar scaleBar=new SeekBar(this);
        scaleBar.setMax(40);
        scaleBar.setProgress(prefs.getInt("touch_scale",100)-80);
        scaleBar.setProgressTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        scaleBar.setThumbTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        scaleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser) {
                int percent=progress+80;
                scaleValue.setText("Tamanho dos botões: "+percent+"%");
                if(fromUser)prefs.edit().putInt("touch_scale",percent).apply();
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        orientation.addView(scaleBar,ui.margin(-1,d(42),0));

        addScale(orientation,"Tamanho dos analógicos", "touch_stick_scale",100,70,135);
        addScale(orientation,"Tamanho ABXY e direcional", "touch_face_scale",100,70,135);
        addScale(orientation,"Opacidade dos controles", "touch_opacity",100,45,100);
        content.addView(orientation,ui.margin(-1,-2,7));

        section("EXPERIÊNCIA", "Animações e resposta visual podem ser ajustadas.");
        LinearLayout options=ui.card(true);
        addSwitch(options,"Animações suaves","Transições e feedback nos botões", "animations",true);
        addSwitch(options,"Vibração ao tocar","Pequenas vibrações nos botões virtuais (se disponíveis)", "haptics",true);
        addSwitch(options,"Começar em Bluetooth / USB","Mostrar o modo físico ao abrir o app", "default_physical",true);
        addSwitch(options,"Modo detalhado","Exibir dados de controle e comunicação no painel",
            "detailed_mode",false);
        content.addView(options,ui.margin(-1,-2,9));

        section("SENSIBILIDADE", "A zona morta elimina movimentos mínimos involuntários.");
        LinearLayout sensitivity=ui.card(true);
        TextView value=ui.label("Zona morta: "+prefs.getInt("deadzone",10)+"%",16,true,ui.text);
        sensitivity.addView(value,ui.margin(-1,d(35),0));
        SeekBar deadzone=new SeekBar(this);deadzone.setMax(35);
        deadzone.setProgress(prefs.getInt("deadzone",10));
        deadzone.setProgressTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        deadzone.setThumbTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        deadzone.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                value.setText("Zona morta: "+progress+"%");
                MobileService.INPUT.setDeadzone(progress/100f);
                prefs.edit().putInt("deadzone",progress).apply();
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        sensitivity.addView(deadzone,ui.margin(-1,d(45),3));
        content.addView(sensitivity,ui.margin(-1,-2,8));

        section("REDE E INFORMAÇÕES","O Xbox deve estar na mesma rede Wi-Fi do celular.");
        LinearLayout network=ui.card(true);
        addNote(network,"Servidor: WebSocket local na porta 26401.");
        addNote(network,"O app original do Oberon precisa estar aberto no Xbox. O Android não injeta controles diretamente no console.");
        addNote(network,"Suporte a um controle transmitido por vez nesta versão.");
        content.addView(network,ui.margin(-1,-2,8));
        section("DIAGNÓSTICOS E ATUALIZAÇÕES",
            "Informações técnicas e novas versões do Oberon Extended Mobile.");
        LinearLayout tools=ui.card(true);
        Button diagnostics=ui.button("Diagnóstico detalhado",false);
        diagnostics.setOnClickListener(v->
            startActivity(new android.content.Intent(this,DiagnosticsActivity.class)));
        tools.addView(diagnostics,ui.margin(-1,d(51),3));
        Button updates=ui.button("Verificar atualizações",false);
        updates.setOnClickListener(v->
            startActivity(new android.content.Intent(this,UpdatesActivity.class)));
        tools.addView(updates,ui.margin(-1,d(51),10));
        content.addView(tools,ui.margin(-1,-2,8));

        section("EXPERIMENTAL",
            "Compatibilidade com controles genéricos e comportamento da conexão.");
        LinearLayout experiments=ui.card(true);
        addSwitch(experiments,"Ativar compatibilidade experimental",
            "Habilita os ajustes avançados abaixo","experimental_enabled",false);
        addSwitch(experiments,"Eixos alternativos do analógico direito",
            "Tenta RX/RY em controles que não respondem usando Z/RZ",
            "experimental_axes_rxry",false);
        addSwitch(experiments,"Inverter eixo Y",
            "Inverte o movimento vertical dos analógicos físicos",
            "experimental_invert_y",false);
        addSwitch(experiments,"Trocar A e B",
            "Para controles genéricos com botões A/B invertidos",
            "experimental_swap_ab",false);
        addRootBackgroundOption(experiments);
        addNote(experiments,"O modo ROOT lê exclusivamente o dispositivo de controle "+
            "selecionado. Não funciona em Android sem root, pode exigir autorização "+
            "no Magisk e ainda precisa ser testado com cada modelo de controle. "+
            "Reinicie o servidor após mudar.");
        addSwitch(experiments,"TCP sem espera de agrupamento",
            "Ativa TCP_NODELAY (padrão). Reinicie o servidor após mudar.",
            "experimental_tcp_nodelay",true);
        addNote(experiments,"A opção TCP_NODELAY pode reduzir o atraso causado "+
            "pelo agrupamento de pacotes, mas não controla o ping da rede Xbox.");
        content.addView(experiments,ui.margin(-1,-2,8));
        section("CONTROLE PREFERIDO",
            "Escolha um dispositivo Android ou mantenha a captura automática.");
        LinearLayout controllers=ui.card(true);
        addControllerChoices(controllers);
        content.addView(controllers,ui.margin(-1,-2,8));


        Button reset=ui.button("Restaurar todas as configurações",false);
        reset.setOnClickListener(v->ExtendedDialog.confirm(this,
            "Restaurar configurações?",
            "Temas, orientação, controle preferido, compatibilidade, botões "+
            "virtuais e ajustes de rede serão restaurados ao padrão.",
            "Restaurar",()->{
                prefs.edit().clear().apply();
                MobileService.INPUT.setDeadzone(.10f);
                MobileService.INPUT.reset();
                recreate();
            }));
        content.addView(reset,ui.margin(-1,d(52),15));
    }
    private void addOrientationPicker(LinearLayout panel,String key,
        String[] values,String[] labels,boolean wholeApp) {
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Button[] choices=new Button[values.length];
        for(int i=0;i<values.length;i++) {
            final String value=values[i];
            Button b=ui.button(labels[i],false);
            b.setTextSize(12);
            choices[i]=b;
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,d(49),1);
            if(i>0)bp.leftMargin=d(7);
            row.addView(b,bp);
            b.setOnClickListener(v->{
                prefs.edit().putString(key,value).apply();
                for(int j=0;j<choices.length;j++) {
                    boolean selected=values[j].equals(value);
                    choices[j].setTextColor(selected?ui.accentText:ui.text);
                    choices[j].setBackground(ui.shape(selected?ui.accent:ui.surface,11));
                }
                if(wholeApp)OrientationPrefs.apply(this,"app_orientation","auto");
            });
        }
        String selected=prefs.getString(key,wholeApp?"auto":"landscape");
        for(int i=0;i<choices.length;i++){
            boolean active=values[i].equals(selected);
            choices[i].setTextColor(active?ui.accentText:ui.text);
            choices[i].setBackground(ui.shape(active?ui.accent:ui.surface,11));
        }
        panel.addView(row,ui.margin(-1,d(53),4));
    }
    private void addScale(LinearLayout panel,String title,String key,
                          int fallback,int min,int max) {
        TextView label=ui.label(title+": "+prefs.getInt(key,fallback)+"%",
            14,true,ui.text);
        panel.addView(label,ui.margin(-1,d(36),8));
        SeekBar slider=new SeekBar(this);
        slider.setMax(max-min);
        slider.setProgress(Math.max(0,Math.min(max-min,prefs.getInt(key,fallback)-min)));
        slider.setProgressTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        slider.setThumbTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser) {
                int value=min+progress;
                label.setText(title+": "+value+"%");
                if(fromUser)prefs.edit().putInt(key,value).apply();
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        panel.addView(slider,ui.margin(-1,d(43),0));
    }
    private void addControllerChoices(LinearLayout target){
        int selected=prefs.getInt("selected_controller",-1);
        Button auto=ui.button((selected<0?"✓  ":"")+"Detecção automática",selected<0);
        auto.setOnClickListener(v->{prefs.edit().putInt("selected_controller",-1).apply();recreate();});
        target.addView(auto,ui.margin(-1,d(48),3));
        int count=0;
        for(int id:android.view.InputDevice.getDeviceIds()){
            android.view.InputDevice device=android.view.InputDevice.getDevice(id);
            if(!InputDevices.isController(device))continue;
            count++;
            Button choose=ui.button((selected==id?"✓  ":"")+device.getName(),selected==id);
            choose.setOnClickListener(v->{
                prefs.edit().putInt("selected_controller",id).apply();recreate();
            });
            target.addView(choose,ui.margin(-1,d(49),8));
        }
        if(count==0)addNote(target,"Nenhum dispositivo físico encontrado. "+
            "Conecte o controle via Bluetooth ou USB e abra esta página novamente.");
    }
    private void section(String title,String sub) {
        content.addView(ui.label(title,14,true,ui.accent),ui.margin(-1,d(30),14));
        TextView detail=ui.label(sub,13,false,ui.muted);
        detail.setPadding(0,0,0,d(9));
        content.addView(detail);
    }
    private void addNote(LinearLayout target,String message){
        TextView note=ui.label(message,13,false,ui.muted);
        note.setPadding(0,d(4),0,d(7));
        target.addView(note);
    }
    private void addRootBackgroundOption(LinearLayout target){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0,d(11),0,d(11));
        LinearLayout labels=new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(ui.label("Capturar em segundo plano (ROOT)",15,true,ui.text));
        labels.addView(ui.label("Permite ler botões e analógicos ao minimizar, usando acesso root opcional.",
            12,false,ui.muted),ui.margin(-1,-2,3));
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(this);
        toggle.setShowText(false);
        toggle.setChecked(prefs.getBoolean("experimental_root_background",false));
        toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        final boolean[] internalUpdate={false};
        toggle.setOnCheckedChangeListener((control,checked)->{
            if(internalUpdate[0])return;
            if(!checked){
                prefs.edit().putBoolean("experimental_root_background",false).apply();
                return;
            }
            internalUpdate[0]=true;
            toggle.setChecked(false);
            internalUpdate[0]=false;
            ExtendedDialog.confirm(this,"Captura experimental ROOT",
                "O Android não entrega analógicos de controle a aplicativos sem foco. "+
                "Esse modo usa su/getevent para ler exclusivamente um controle físico "+
                "pelo dispositivo /dev/input, mesmo com a janela minimizada.\n\n"+
                "É necessário conceder root ao Oberon Extended Mobile. "+
                "Isso dá privilégios elevados ao processo leitor e deve ser usado "+
                "somente se você confia neste aplicativo.\n\n"+
                "Ele não é compatível com todos os aparelhos/controladores. "+
                "Ative e reinicie o servidor para testar.",
                "Ativar ROOT",()->{
                    prefs.edit().putBoolean("experimental_root_background",true).apply();
                    internalUpdate[0]=true;
                    toggle.setChecked(true);
                    internalUpdate[0]=false;
                });
        });
        row.addView(toggle);
        target.addView(row);
    }
    private void addSwitch(LinearLayout target,String title,String help,String pref,boolean fallback){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0,d(8),0,d(8));
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(ui.label(title,15,true,ui.text));
        labels.addView(ui.label(help,12,false,ui.muted),ui.margin(-1,-2,3));
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(this);
        toggle.setShowText(false);
        toggle.setChecked(prefs.getBoolean(pref,fallback));
        toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(ui.accent));
        toggle.setOnCheckedChangeListener((button,checked)->prefs.edit().putBoolean(pref,checked).apply());
        row.addView(toggle);
        target.addView(row);
    }
    @Override public void finish() {
        super.finish();
        MobileUi.exit(this);
    }
}
