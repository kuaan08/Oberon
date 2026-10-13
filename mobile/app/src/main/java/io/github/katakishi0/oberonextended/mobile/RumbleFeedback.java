package io.github.katakishi0.oberonextended.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.InputDevice;

/** Xbox response's four motor values per slot -> Android device vibrators, if exposed. */
public final class RumbleFeedback {
    private final Context context;
    private final ControllerSlots controllers;
    private final SharedPreferences prefs;
    private final long[] lastPulse=new long[4];
    private final int[] lastStrength=new int[4];
    private final Vibrator[] active=new Vibrator[4];
    RumbleFeedback(Context context,ControllerSlots controllers){
        this.context=context;
        this.controllers=controllers;
        this.prefs=context.getSharedPreferences(MobileUi.PREFS,Context.MODE_PRIVATE);
    }
    synchronized void accept(byte[] request){
        if(!prefs.getBoolean("controller_rumble",true)||request.length!=17){
            stop();return;
        }
        long now=SystemClock.elapsedRealtime();
        for(int slot=0;slot<4;slot++){
            int start=1+slot*4;
            int left=request[start]&255,right=request[start+1]&255;
            int triggerL=request[start+2]&255,triggerR=request[start+3]&255;
            int strength=Math.max(Math.max(left,right),Math.max(triggerL,triggerR));
            Vibrator motor=deviceVibrator(slot);
            if(motor==null||!motor.hasVibrator())continue;
            if(strength==0){
                if(lastStrength[slot]!=0)motor.cancel();
                lastStrength[slot]=0;
                continue;
            }
            if(now-lastPulse[slot]<75 && lastStrength[slot]!=0)continue;
            try {
                if(Build.VERSION.SDK_INT>=26){
                    motor.vibrate(VibrationEffect.createOneShot(120,
                        Math.max(1,Math.min(255,strength))));
                }else motor.vibrate(120);
                active[slot]=motor;
                lastPulse[slot]=now;
                lastStrength[slot]=strength;
            }catch(RuntimeException ignored){
                // Many Bluetooth controller drivers expose no vibrator, even
                // though the controller itself has motors. Never claim success.
            }
        }
    }
    private Vibrator deviceVibrator(int slot){
        int id=controllers.deviceForSlot(slot);
        InputDevice device=id<0?null:InputDevice.getDevice(id);
        if(device!=null){
            try{
                Vibrator v=device.getVibrator();
                if(v!=null&&v.hasVibrator())return v;
            }catch(RuntimeException ignored){}
        }
        // Only the on-screen virtual pad may use the phone motor, if opted in.
        if(slot==0&&!MobileService.INPUT.usePhysical()&&
           prefs.getBoolean("touch_rumble_feedback",false)){
            return (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        return null;
    }
    synchronized void stop(){
        for(int i=0;i<4;i++){
            if(active[i]!=null){try{active[i].cancel();}catch(RuntimeException ignored){}}
            active[i]=null;lastStrength[i]=0;
        }
    }
}
