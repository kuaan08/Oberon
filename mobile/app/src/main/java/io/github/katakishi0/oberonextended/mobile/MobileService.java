package io.github.katakishi0.oberonextended.mobile;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import java.io.IOException;

public final class MobileService extends Service {
    public static final String ACTION_START="io.github.katakishi0.oberonextended.START";
    public static final String ACTION_STOP="io.github.katakishi0.oberonextended.STOP";
    private static final String CHANNEL="oberon_connection";
    public static final InputState INPUT=new InputState();
    private static volatile MobileService instance;
    private OberonWebSocketServer server;
    private volatile String status="Server stopped";
    public static boolean running(){return instance!=null&&instance.server!=null&&instance.server.isRunning();}
    public static String currentStatus(){return instance==null?"Server stopped":instance.status;}
    public static int connectedClients(){return instance==null||instance.server==null?0:instance.server.clientCount();}

    @Override public void onCreate(){
        super.onCreate();instance=this;
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Oberon connection",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps the local Xbox controller connection alive.");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        server=new OberonWebSocketServer(INPUT,(message,clients)->{
            status=message;
            NotificationManager nm=getSystemService(NotificationManager.class);
            if(nm!=null&&running())nm.notify(26401,notification(message));
        });
    }
    private Notification notification(String message){
        Intent activity=new Intent(this,MainActivity.class);
        PendingIntent open=PendingIntent.getActivity(this,0,activity,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent=new Intent(this,MobileService.class).setAction(ACTION_STOP);
        PendingIntent stop=PendingIntent.getService(this,1,stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL)
            .setSmallIcon(R.drawable.ic_oberon)
            .setContentTitle("Oberon Extended Mobile")
            .setContentText(message)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(
                android.R.drawable.ic_media_pause,"Stop",stop).build())
            .build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?ACTION_START:intent.getAction();
        if(ACTION_STOP.equals(action)){stopSelf();return START_NOT_STICKY;}
        try{
            Notification initial=notification("Starting Xbox server");
            if(Build.VERSION.SDK_INT>=29)
                startForeground(26401,initial,ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            else startForeground(26401,initial);
            server.start();
            status="Waiting for Xbox";
        }catch(IOException|RuntimeException e){
            status="Could not start: "+e.getMessage();
            stopSelf();
        }
        return START_STICKY;
    }
    @Override public void onDestroy(){
        if(server!=null)server.stop();
        INPUT.reset();
        instance=null;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
