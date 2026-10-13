package io.github.katakishi0.oberonextended.mobile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Checks public GitHub Releases without downloading or installing anything silently. */
public final class UpdatesActivity extends Activity {
    private static final String RELEASES="https://github.com/kuaan08/Oberon/releases";
    private MobileUi ui;
    private TextView result;
    private Button action;
    private String target=RELEASES;
    private int dp(float v){return MobileUi.dp(this,v);}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        OrientationPrefs.apply(this,"app_orientation","auto");
        ui=MobileUi.from(this);
        getWindow().setStatusBarColor(ui.bg);
        getWindow().setNavigationBarColor(ui.bg);
        LinearLayout layout=new LinearLayout(this);
        layout.setBackgroundColor(ui.bg);layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18),dp(18),dp(18),dp(22));
        setContentView(layout);
        Button back=ui.button("‹  Voltar",false);back.setOnClickListener(v->finish());
        layout.addView(back,new LinearLayout.LayoutParams(dp(118),dp(47)));
        TextView title=ui.label("Atualizações",27,true,ui.text);
        layout.addView(title,ui.margin(-1,dp(58),14));
        LinearLayout card=ui.card(true);
        card.addView(ui.label("CANAL EXPERIMENTAL",13,true,ui.accent));
        card.addView(ui.label("Oberon Extended Mobile",22,true,ui.text),
            ui.margin(-1,dp(45),9));
        card.addView(ui.label("Instalado: "+version(),15,false,ui.muted));
        result=ui.label("Toque em Verificar para procurar uma versão publicada no GitHub.",
            15,false,ui.text);
        result.setPadding(0,dp(16),0,dp(12));
        card.addView(result);
        Button verify=ui.button("Verificar atualizações",true);
        verify.setOnClickListener(v->{verify.setEnabled(false);check(verify);});
        card.addView(verify,ui.margin(-1,dp(50),12));
        action=ui.button("Abrir GitHub Releases",false);
        action.setOnClickListener(v->{
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(target)));}
            catch(Exception e){ExtendedDialog.info(this,"Não foi possível abrir o link",e.getMessage());}
        });
        card.addView(action,ui.margin(-1,dp(50),8));
        layout.addView(card);
        TextView note=ui.label("As versões de teste (Alpha/Beta) também são verificadas. " +
            "Uma atualização só é instalada com sua autorização; o Android pode exigir " +
            "confirmação e permissão para instalar APKs.",13,false,ui.muted);
        layout.addView(note,ui.margin(-1,-2,18));
    }
    private String version(){
        try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
        catch(Exception e){return "desconhecida";}
    }
    private void check(Button verify){
        result.setText("Procurando atualizações...");
        new Thread(()->{
            String message;
            String destination=RELEASES;
            try{
                URL url=new URL("https://api.github.com/repos/kuaan08/Oberon/releases?per_page=15");
                HttpURLConnection conn=(HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(6000);conn.setReadTimeout(8000);
                conn.setRequestProperty("Accept","application/vnd.github+json");
                conn.setRequestProperty("User-Agent","OberonExtendedMobile-Android");
                try{
                    if(conn.getResponseCode()!=200)throw new java.io.IOException(
                        "GitHub respondeu "+conn.getResponseCode());
                    try(InputStream is=conn.getInputStream();
                        ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                        byte[] buffer=new byte[4096];int n;
                        while((n=is.read(buffer))!=-1)bytes.write(buffer,0,n);
                        JSONArray releases=new JSONArray(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
                        JSONObject newest=null;
                        for(int i=0;i<releases.length();i++){
                            JSONObject item=releases.getJSONObject(i);
                            if(item.optBoolean("draft",false))continue;
                            newest=item;break;
                        }
                        if(newest==null){
                            message="Ainda não há uma versão publicada em Releases. "+
                                "As compilações de desenvolvimento podem estar disponíveis no GitHub Actions.";
                            destination="https://github.com/kuaan08/Oberon/actions/workflows/oberon-extended-mobile.yml";
                        } else {
                            String tag=newest.optString("tag_name","");
                            int compare=compareVersions(tag,version());
                            destination=newest.optString("html_url",RELEASES);
                            if(compare>0){
                                message="Nova versão publicada: "+tag+"\n\n"+
                                    "Abra a página oficial para conferir as mudanças e baixar o APK.";
                                JSONArray assets=newest.optJSONArray("assets");
                                if(assets!=null)for(int j=0;j<assets.length();j++){
                                    JSONObject asset=assets.getJSONObject(j);
                                    if(asset.optString("name","").endsWith(".apk")){
                                        destination=asset.optString("browser_download_url",destination);break;
                                    }
                                }
                            } else {
                                message="Versão instalada: "+version()+"\n"+
                                    "Última versão publicada: "+tag+"\n\n"+
                                    "Nenhuma versão mais recente encontrada em Releases.";
                            }
                        }
                    }
                }finally{conn.disconnect();}
            }catch(Exception ex){
                message="Não foi possível verificar agora: "+ex.getMessage()+
                    "\nConfira sua conexão e tente novamente.";
            }
            final String m=message,d=destination;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                result.setText(m);target=d;verify.setEnabled(true);
                action.setText(d.endsWith(".apk")?"Abrir download oficial":"Abrir página no GitHub");
            });
        },"Extended-Update-Check").start();
    }
    static int compareVersions(String left,String right){
        int[] a=numeric(left),b=numeric(right);
        for(int i=0;i<4;i++){if(a[i]!=b[i])return Integer.compare(a[i],b[i]);}
        // A stable release is newer than a prerelease of the same numeric version.
        boolean aPre=left.contains("-"),bPre=right.contains("-");
        if(aPre!=bPre)return aPre?-1:1;
        return 0;
    }
    private static int[] numeric(String tag){
        String clean=tag.replaceFirst("^[vV]","");
        String[] sections=clean.split("[.-]");
        int[] result=new int[4];
        int idx=0;
        for(String part:sections){
            if(idx>=4)break;
            if(part.matches("\\d+"))result[idx++]=Integer.parseInt(part);
            else break;
        }
        return result;
    }
}
