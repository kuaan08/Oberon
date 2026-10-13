package io.github.katakishi0.oberonextended.mobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.InputDevice;
import rikka.shizuku.Shizuku;

/** Coordinates the opt-in, unrooted Shizuku shell UserService. */
public final class ShizukuGamepadCapture {
    public interface Status {void onChange(String status);}
    private final Context context;
    private final SharedPreferences prefs;
    private RootGamepadCapture decoder;
    private final Status status;
    private volatile boolean started,connected;
    private volatile String description="Desativado";
    private IShizukuInputService remote;
    private Shizuku.UserServiceArgs args;
    private final Shizuku.OnBinderDeadListener binderDead=()->{
        connected=false;decoder.clearRemote();note("Shizuku foi interrompido.");
    };
    private final IShizukuInputSink sink=new IShizukuInputSink.Stub(){
        @Override public void onStatus(String text){note(text);}
        @Override public void onRanges(String text){decoder.prepareRemoteRanges(text);}
        @Override public void onEvents(String text){decoder.acceptRemoteEvents(text);}
    };
    private final ServiceConnection connection=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName name,IBinder binder){
            remote=IShizukuInputService.Stub.asInterface(binder);
            try{
                String chosen=controllerName();
                if(chosen==null){note("Shizuku: nenhum controle Android identificado.");return;}
                note("Shizuku: "+remote.whoami()+" — aguardando eventos do controle "+chosen);
                remote.start(chosen,sink);
                connected=true;
            }catch(Exception e){note("Shizuku: falha de conexão "+e.getMessage());}
        }
        @Override public void onServiceDisconnected(ComponentName name){
            remote=null;connected=false;decoder.clearRemote();
            note("Serviço Shizuku desconectado.");
        }
    };
    public ShizukuGamepadCapture(Context context,InputState input,Status status){
        this.context=context.getApplicationContext();
        this.status=status;
        prefs=context.getSharedPreferences(MobileUi.PREFS,Context.MODE_PRIVATE);
        decoder=new RootGamepadCapture(context,input,message->{});
    }
    public boolean isCapturing(){return connected&&description.contains("captura experimental ativa");}
    public String description(){return description;}
    private void note(String text){description=text;status.onChange(text);}
    private String controllerName(){
        int id=prefs.getInt("selected_controller",-1);
        InputDevice selected=InputDevice.getDevice(id);
        if(InputDevices.isController(selected))return selected.getName();
        for(int device:InputDevice.getDeviceIds()){
            InputDevice p=InputDevice.getDevice(device);
            if(InputDevices.isController(p))return p.getName();
        }
        return null;
    }
    public synchronized void start(){
        if(started)return;
        started=true;
        try{
            if(!Shizuku.pingBinder()){
                note("Shizuku não está iniciado. Abra o Shizuku e inicie via depuração sem fio.");
                started=false;return;
            }
            if(Shizuku.isPreV11()||
               Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED){
                note("Shizuku não autorizado. Conceda permissão em Configurações e reinicie o servidor.");
                started=false;return;
            }
            String chosen=controllerName();
            if(chosen==null){
                note("Conecte um controle Bluetooth/USB antes de iniciar o servidor.");
                started=false;return;
            }
            args=new Shizuku.UserServiceArgs(new ComponentName(context,ShizukuInputUserService.class))
                .daemon(false).processNameSuffix("gamepad").version(1).debuggable(false);
            Shizuku.addBinderDeadListener(binderDead);
            Shizuku.bindUserService(args,connection);
            note("Conectando ao serviço Shizuku (ADB/shell)...");
        }catch(Exception|LinkageError ex){
            note("Shizuku indisponível: "+ex.getMessage());
            started=false;
        }
    }
    public synchronized void stop(){
        try {
            if(remote!=null)remote.stop();
            if(args!=null&&Shizuku.pingBinder())
                Shizuku.unbindUserService(args,connection,true);
            Shizuku.removeBinderDeadListener(binderDead);
        }catch(Exception|LinkageError ignored){}
        started=false;connected=false;args=null;remote=null;
        decoder.clearRemote();
        note("Desativado");
    }
}
