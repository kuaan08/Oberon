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
        ui=MobileUi.from(this);
        getWindow().setStatusBarColor(ui.bg);
        getWindow().setNavigationBarColor(ui.bg);
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

        section("TELA DO CONTROLE", "Escolha como o controle virtual ocupa toda a tela.");
        LinearLayout orientation=ui.card(true);
        RadioGroup orientations=new RadioGroup(this);
        orientations.setOrientation(RadioGroup.HORIZONTAL);
        String selected=prefs.getString("touch_orientation","landscape");
        for(String value:new String[]{"portrait","landscape"}){
            RadioButton option=new RadioButton(this);
            option.setText(value.equals("portrait")?"Vertical":"Horizontal");
            option.setTextColor(ui.text);option.setTextSize(15);
            option.setButtonTintList(android.content.res.ColorStateList.valueOf(ui.accent));
            option.setPadding(d(6),d(5),d(6),d(5));
            option.setChecked(selected.equals(value));
            option.setOnClickListener(v->prefs.edit().putString("touch_orientation",value).apply());
            orientations.addView(option,new RadioGroup.LayoutParams(0,d(48),1f));
        }
        orientation.addView(orientations);
        addNote(orientation, "A tela de toque fica em tela cheia, com botão Voltar. A orientação do painel segue a posição do celular.");
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
        content.addView(orientation,ui.margin(-1,-2,7));

        section("EXPERIÊNCIA", "Animações e resposta visual podem ser ajustadas.");
        LinearLayout options=ui.card(true);
        addSwitch(options,"Animações suaves","Transições e feedback nos botões", "animations",true);
        addSwitch(options,"Vibração ao tocar","Pequenas vibrações nos botões virtuais (se disponíveis)", "haptics",true);
        addSwitch(options,"Começar em Bluetooth / USB","Mostrar o modo físico ao abrir o app", "default_physical",true);
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

        Button reset=ui.button("Restaurar todas as configurações",false);
        reset.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
            .setTitle("Restaurar padrões?")
            .setMessage("Temas, orientação, vibração, animações e zona morta voltarão ao padrão.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Restaurar",(dialog,which)->{
                prefs.edit().clear().apply();
                MobileService.INPUT.setDeadzone(.10f);
                MobileService.INPUT.reset();
                recreate();
            }).show());
        content.addView(reset,ui.margin(-1,d(52),15));
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
