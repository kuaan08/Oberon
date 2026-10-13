package io.github.katakishi0.oberonextended.mobile;

import android.os.Binder;
import android.os.RemoteException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs with the Shizuku UserService's shell UID (2000 on non-root).
 * It never elevates to root, chmods nodes, or changes SELinux settings.
 * Device selection is restricted to a matching, recognized gamepad input node.
 */
public final class ShizukuInputUserService extends IShizukuInputService.Stub {
    private static final Pattern DEVICE=Pattern.compile("add device\\s+\\d+:\\s*(/dev/input/event\\d+)");
    private static final Pattern NAME=Pattern.compile("name:\\s*\"([^\"]+)\"");
    private volatile boolean active;
    private volatile Process process;
    private volatile IShizukuInputSink sink;
    private Thread reader;
    public ShizukuInputUserService(){}
    public ShizukuInputUserService(android.content.Context ignored){}

    @Override public String whoami(){return "UID "+android.os.Process.myUid();}
    @Override public synchronized void start(String chosenName,IShizukuInputSink callback) {
        stop();
        sink=callback;
        active=true;
        reader=new Thread(()->loop(chosenName),"Oberon-Shizuku-Evdev");
        reader.setDaemon(true);reader.start();
    }
    @Override public synchronized void stop() {
        active=false;
        if(process!=null){process.destroy();process=null;}
        if(reader!=null){reader.interrupt();reader=null;}
        sink=null;
    }
    public void destroy(){stop();}
    private void message(String value){
        IShizukuInputSink current=sink;
        if(current!=null)try{current.onStatus(value);}catch(RemoteException ignored){}
    }
    private void ranges(String value){
        IShizukuInputSink current=sink;
        if(current!=null)try{current.onRanges(value);}catch(RemoteException ignored){}
    }
    private void events(String value){
        IShizukuInputSink current=sink;
        if(current!=null)try{current.onEvents(value);}catch(RemoteException ignored){}
    }
    private void loop(String chosenName){
        try{
            message("Shizuku conectado: "+whoami()+". Verificando acesso ao controle...");
            String listing=command("getevent","-lp");
            String path=findGamepad(listing,chosenName);
            if(path==null){
                message("Shizuku: controle não encontrado nos nós de entrada. Confira o nome ou o acesso ADB.");
                return;
            }
            String capabilities=command("getevent","-lp",path);
            ranges(capabilities);
            Process readerProcess=new ProcessBuilder("getevent","-l",path)
                .redirectErrorStream(true).start();
            process=readerProcess;
            message("Shizuku: lendo "+path+" (ADB/shell); captura experimental ativa.");
            try(BufferedReader input=new BufferedReader(new InputStreamReader(
                    readerProcess.getInputStream(),StandardCharsets.UTF_8))){
                String line;StringBuilder batch=new StringBuilder();
                while(active&&(line=input.readLine())!=null) {
                    if(line.toLowerCase().contains("permission denied")){
                        message("Shizuku: permissão negada para "+path+
                            ". ADB/shell não pode ler esse controle neste aparelho.");
                        return;
                    }
                    batch.append(line).append('\n');
                    if(line.contains("SYN_REPORT")||batch.length()>1400){
                        events(batch.toString());
                        batch.setLength(0);
                    }
                }
                if(batch.length()>0)events(batch.toString());
            }
            if(active)message("Shizuku: leitura encerrada (dispositivo desconectado ou bloqueado).");
        }catch(Exception e){
            if(active)message("Shizuku falhou: "+e.getClass().getSimpleName()+" — "+
                String.valueOf(e.getMessage()));
        }finally{
            if(process!=null)process.destroy();
            process=null;
        }
    }
    private String command(String... arguments)throws IOException {
        Process proc=new ProcessBuilder(arguments).redirectErrorStream(true).start();
        process=proc;
        StringBuilder text=new StringBuilder();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
            proc.getInputStream(),StandardCharsets.UTF_8))){
            String line;
            while(active&&(line=reader.readLine())!=null&&text.length()<250000)
                text.append(line).append('\n');
        }finally{proc.destroy();process=null;}
        if(text.toString().toLowerCase().contains("permission denied"))
            throw new IOException("ADB/shell sem permissão para /dev/input.");
        return text.toString();
    }
    static String findGamepad(String listing,String chosenName){
        if(chosenName==null||chosenName.isEmpty())return null;
        Matcher devices=DEVICE.matcher(listing);
        while(devices.find()){
            int end=listing.indexOf("add device ",devices.end());
            if(end<0)end=listing.length();
            String block=listing.substring(devices.end(),end);
            Matcher m=NAME.matcher(block);
            if(!m.find()||!m.group(1).equalsIgnoreCase(chosenName))continue;
            String caps=block.toUpperCase();
            // A keyboard, touchscreen or motion sensor must never be selected
            // just because its name happens to contain "controller".
            if(!caps.contains("BTN_GAMEPAD")&&!caps.contains("BTN_SOUTH")&&
                !caps.contains("BTN_A")&&!caps.contains("BTN_JOYSTICK"))
                continue;
            return devices.group(1);
        }
        return null;
    }
}
