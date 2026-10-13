package io.github.katakishi0.oberonextended.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.InputDevice;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Explicitly opted-in experimental ROOT input capture. Reads ONLY the evdev
 * gamepad chosen in Android settings, never touchscreen or keyboard devices.
 * Root is required because a normal Android service is not given joystick
 * MotionEvents once its Activity loses focus.
 */
public final class RootGamepadCapture {
    public interface Status { void onChange(String status); }
    private static final Pattern DEVICE=Pattern.compile(
        "(?:add device\\s+\\d+:\\s*)(/dev/input/event\\d+)");
    private static final Pattern NAME=Pattern.compile("name:\\s*\"([^\"]+)\"");
    private static final Pattern RANGE=Pattern.compile(
        "(ABS_[A-Z0-9_]+)\\s*:\\s*value\\s+(-?\\d+),\\s*min\\s+(-?\\d+),\\s*max\\s+(-?\\d+)");
    private final InputState state;
    private final SharedPreferences prefs;
    private final Status status;
    private volatile boolean running;
    private volatile boolean capturing;
    private volatile String description="Desativado";
    private volatile Process process;
    private Thread worker;
    private static final class Bounds {
        final int min,max; final int initial;
        Bounds(int min,int max,int initial){this.min=min;this.max=max;this.initial=initial;}
    }
    private final Map<String,Bounds> ranges=new HashMap<>();
    private final Map<String,Integer> axes=new HashMap<>();
    private float digitalLt,digitalRt;

    public RootGamepadCapture(Context ctx,InputState state,Status status){
        this.state=state;
        this.prefs=ctx.getSharedPreferences(MobileUi.PREFS,Context.MODE_PRIVATE);
        this.status=status;
    }
    public boolean isCapturing(){return capturing;}
    public String description(){return description;}
    private void note(String message){
        description=message;
        status.onChange(message);
    }
    public synchronized void start(){
        if(running)return;
        running=true;
        worker=new Thread(this::loop,"Extended-RootGamepad");
        worker.setDaemon(true);worker.start();
    }
    public synchronized void stop(){
        running=false;
        capturing=false;
        Process p=process;
        if(p!=null)p.destroy();
        if(worker!=null)worker.interrupt();
        state.clearPhysical();
        note("Desativado");
    }
    private void loop(){
        while(running){
            try{
                note("Solicitando permissão ROOT…");
                String devices=readCommand("getevent -lp",18000);
                String target=findGamepad(devices);
                if(target==null){
                    note("Controle não localizado no /dev/input. Reconecte e tente novamente.");
                    sleep(5000);continue;
                }
                ranges.clear();axes.clear();
                String details=readCommand("getevent -lp "+target,7000);
                parseRanges(details);
                for(Map.Entry<String,Bounds> e:ranges.entrySet())
                    axes.put(e.getKey(),e.getValue().initial);
                // Never execute a user-supplied path. The device string is
                // extracted from a strict /dev/input/event[0-9]+ pattern.
                Process p=new ProcessBuilder("su","-c","getevent -l "+target)
                    .redirectErrorStream(true).start();
                process=p;
                capturing=true;
                state.clearPhysical();
                note("Capturando controle em segundo plano: "+target);
                try(BufferedReader in=new BufferedReader(new InputStreamReader(
                    p.getInputStream(),StandardCharsets.UTF_8))){
                    String line;
                    while(running&&(line=in.readLine())!=null){
                        consume(line);
                    }
                }finally{
                    capturing=false;
                    p.destroy();
                    process=null;
                    state.clearPhysical();
                }
                if(running){note("Leitura interrompida. Tentando reconectar controle…");sleep(2500);}
            }catch(Exception e){
                capturing=false;
                state.clearPhysical();
                if(running){note("ROOT indisponível: "+safe(e.getMessage()));sleep(5000);}
            }
        }
    }
    private static String safe(String message){
        if(message==null)return "erro de leitura";
        return message.substring(0,Math.min(110,message.length()));
    }
    private void sleep(long ms){
        try{Thread.sleep(ms);}catch(InterruptedException e){Thread.currentThread().interrupt();}
    }
    private String readCommand(String command,long timeoutMs) throws IOException {
        Process p=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
        process=p;
        Thread watchdog=new Thread(()->{
            try{Thread.sleep(timeoutMs);if(p.isAlive())p.destroy();}
            catch(InterruptedException ignored){}
        },"Extended-RootTimeout");
        watchdog.setDaemon(true);watchdog.start();
        StringBuilder text=new StringBuilder();
        try(BufferedReader r=new BufferedReader(new InputStreamReader(
                p.getInputStream(),StandardCharsets.UTF_8))){
            String line;
            while((line=r.readLine())!=null&&running&&text.length()<1000000)
                text.append(line).append('\n');
        }finally{p.destroy();process=null;}
        if(text.length()==0)throw new IOException("Sem saída do su/getevent");
        return text.toString();
    }
    private String findGamepad(String all){
        String wanted=null;
        int selected=prefs.getInt("selected_controller",-1);
        if(selected>=0){
            InputDevice explicit=InputDevice.getDevice(selected);
            if(InputDevices.isController(explicit))wanted=explicit.getName();
        }
        if(wanted==null){
            for(int id:InputDevice.getDeviceIds()){
                InputDevice device=InputDevice.getDevice(id);
                if(InputDevices.isController(device)){
                    wanted=device.getName();break;
                }
            }
        }
        if(wanted==null)return null;
        Matcher m=DEVICE.matcher(all);
        while(m.find()){
            String path=m.group(1);
            int stop=all.indexOf("add device ",m.end());
            if(stop<0)stop=all.length();
            String section=all.substring(m.end(),stop);
            Matcher name=NAME.matcher(section);
            if(name.find()&&name.group(1).equalsIgnoreCase(wanted))
                return path;
        }
        return null;
    }
    private void parseRanges(String details){
        Matcher m=RANGE.matcher(details);
        while(m.find()){
            try{
                ranges.put(m.group(1),new Bounds(
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4)),
                    Integer.parseInt(m.group(2))));
            }catch(NumberFormatException ignored){}
        }
    }
    private void consume(String line){
        String[] parts=line.trim().split("\\s+");
        for(int i=0;i+2<parts.length;i++){
            if(parts[i].equals("EV_KEY")){
                int bit=key(parts[i+1]);
                boolean down=parts[i+2].equals("DOWN")||parts[i+2].equals("REPEAT")
                    ||parts[i+2].equals("00000001")||parts[i+2].equals("00000002");
                if(bit!=0)state.setPhysicalButton(bit,down);
                if(parts[i+1].equals("BTN_TL2")){digitalLt=down?1:0;state.setPhysicalDigitalTrigger(true,digitalLt);}
                if(parts[i+1].equals("BTN_TR2")){digitalRt=down?1:0;state.setPhysicalDigitalTrigger(false,digitalRt);}
                return;
            }
            if(parts[i].equals("EV_ABS")){
                try{axes.put(parts[i+1],(int)Long.parseLong(parts[i+2],16));}
                catch(NumberFormatException ignored){}
                return;
            }
            if(parts[i].equals("EV_SYN")&&parts[i+1].equals("SYN_REPORT")){
                flushAxes();return;
            }
        }
    }
    private static int key(String key){
        switch(key){
            case "BTN_A":case "BTN_SOUTH":return InputState.A;
            case "BTN_B":case "BTN_EAST":return InputState.B;
            case "BTN_X":case "BTN_WEST":return InputState.X;
            case "BTN_Y":case "BTN_NORTH":return InputState.Y;
            case "BTN_SELECT":case "BTN_BACK":return InputState.VIEW;
            case "BTN_START":return InputState.START;
            case "BTN_MODE":return InputState.GUIDE;
            case "BTN_TL":return InputState.LB;
            case "BTN_TR":return InputState.RB;
            case "BTN_THUMBL":return InputState.L3;
            case "BTN_THUMBR":return InputState.R3;
            case "KEY_UP":return InputState.UP;
            case "KEY_DOWN":return InputState.DOWN;
            case "KEY_LEFT":return InputState.LEFT;
            case "KEY_RIGHT":return InputState.RIGHT;
            default:return 0;
        }
    }
    private float stick(String label){
        Integer value=axes.get(label);
        Bounds r=ranges.get(label);
        if(value==null||r==null||r.max<=r.min)return 0;
        double center=((double)r.min+r.max)/2d;
        double extent=(r.max-r.min)/2d;
        return (float)Math.max(-1,Math.min(1,(value-center)/extent));
    }
    private float trigger(String label){
        Integer value=axes.get(label);
        Bounds r=ranges.get(label);
        if(value==null||r==null||r.max<=r.min)return 0;
        return (float)Math.max(0,Math.min(1,((double)value-r.min)/(r.max-r.min)));
    }
    private void flushAxes(){
        boolean experimental=prefs.getBoolean("experimental_enabled",false);
        boolean alternate=experimental&&prefs.getBoolean("experimental_axes_rxry",false);
        boolean rxry=ranges.containsKey("ABS_RX")&&ranges.containsKey("ABS_RY");
        boolean rightRxRy=alternate||rxry;
        float rx=stick(rightRxRy?"ABS_RX":"ABS_Z");
        float ry=stick(rightRxRy?"ABS_RY":"ABS_RZ");
        float lt=trigger(ranges.containsKey("ABS_BRAKE")?"ABS_BRAKE":"ABS_Z");
        float rt=trigger(ranges.containsKey("ABS_GAS")?"ABS_GAS":"ABS_RZ");
        if(!rightRxRy){
            lt=trigger("ABS_BRAKE");
            rt=trigger("ABS_GAS");
        }
        boolean invert=experimental&&prefs.getBoolean("experimental_invert_y",false);
        state.setPhysicalAxes(stick("ABS_X"),stick("ABS_Y")*(invert?-1:1),
            rx,ry*(invert?-1:1),lt,rt);
        state.setPhysicalHat(stick("ABS_HAT0X"),stick("ABS_HAT0Y"));
    }

    // Exposed for unit tests with no actual root or Android input devices.
    static int testButton(String label){return key(label);}
}
