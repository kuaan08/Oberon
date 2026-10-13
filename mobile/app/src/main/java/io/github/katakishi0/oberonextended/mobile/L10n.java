package io.github.katakishi0.oberonextended.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Localized UI text with explicit language choice and automatic system-locale
 * detection. Translation dictionaries are shipped offline (no external API).
 * Languages without a complete dictionary fall back to English.
 */
public final class L10n {
    public static final String[] CODES={
        "system","en","pt","es","fr","de","it","ru","zh","ja","ko",
        "hi","ar","tr","id","nl","pl","uk","vi","th"
    };
    public static final String[] NAMES={
        "System language","English","Português","Español","Français",
        "Deutsch","Italiano","Русский","中文","日本語","한국어",
        "हिन्दी","العربية","Türkçe","Bahasa Indonesia","Nederlands",
        "Polski","Українська","Tiếng Việt","ไทย"
    };
    private static JSONObject translations;
    private L10n(){}
    static String language(Context context){
        SharedPreferences prefs=context.getSharedPreferences(MobileUi.PREFS,Context.MODE_PRIVATE);
        String configured=prefs.getString("app_language","system");
        if("system".equals(configured))configured=Locale.getDefault().getLanguage();
        if("in".equals(configured))configured="id";
        if("iw".equals(configured))configured="he";
        return configured;
    }
    public static String selected(Context context){
        return context.getSharedPreferences(MobileUi.PREFS,Context.MODE_PRIVATE)
            .getString("app_language","system");
    }
    public static void set(Context context,String code){
        if(!Arrays.asList(CODES).contains(code))code="system";
        context.getSharedPreferences(MobileUi.PREFS,Context.MODE_PRIVATE)
            .edit().putString("app_language",code).apply();
    }
    public static String titleForSystem(Context context){
        Locale locale=Locale.getDefault();
        return NAMES[0]+" ("+locale.getDisplayLanguage(locale)+")";
    }
    private static synchronized JSONObject data(Context ctx){
        if(translations!=null)return translations;
        try(InputStream stream=ctx.getAssets().open("translations.json");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;
            while((n=stream.read(buffer))!=-1)bytes.write(buffer,0,n);
            translations=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
        }catch(Exception ignored){translations=new JSONObject();}
        return translations;
    }
    /** Keep the original source text for Portuguese; all other languages fall back to English. */
    public static String t(Context context,String input){
        if(input==null||input.isEmpty())return input;
        String lang=language(context);
        if("pt".equals(lang))return input;
        JSONObject all=data(context);
        JSONObject english=all.optJSONObject("en");
        JSONObject chosen=all.optJSONObject(lang);
        if(chosen!=null&&chosen.has(input))return chosen.optString(input,input);
        if(english!=null&&english.has(input))return english.optString(input,input);
        // Dynamic values: translate stable prefixes and preserve numbers/device names.
        String prefix=null;
        if(english!=null){
            java.util.Iterator<String> keys=english.keys();
            while(keys.hasNext()){
                String k=keys.next();
                if((k.endsWith(": ")||k.startsWith("\n"))&&input.startsWith(k)
                    && (prefix==null||k.length()>prefix.length()))prefix=k;
            }
        }
        if(prefix!=null){
            String translated=chosen!=null?chosen.optString(prefix,""):null;
            if(translated==null||translated.isEmpty())translated=english.optString(prefix,prefix);
            return translated+input.substring(prefix.length());
        }
        // Multi-line assembled status messages are translated line by line.
        if(input.contains("\n")){
            String[] parts=input.split("\n",-1);
            StringBuilder output=new StringBuilder();
            for(int i=0;i<parts.length;i++){
                if(i>0)output.append('\n');
                if(!parts[i].equals(input))output.append(t(context,parts[i]));
            }
            return output.toString();
        }
        // Unknown strings fall back to the authored text. New UI text should
        // always be added to the English translation dictionary.
        return input;
    }
}
