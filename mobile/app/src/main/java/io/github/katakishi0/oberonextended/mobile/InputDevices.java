package io.github.katakishi0.oberonextended.mobile;

import android.view.InputDevice;
import android.view.MotionEvent;
import java.lang.reflect.Method;
import java.util.Locale;

/** Device identification, source filtering, and optional Android battery API. */
public final class InputDevices {
    private InputDevices(){}
    public static boolean isController(InputDevice device){
        if(device==null||device.isVirtual())return false;
        int source=device.getSources();
        return (source&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD ||
            (source&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK ||
            (source&InputDevice.SOURCE_DPAD)==InputDevice.SOURCE_DPAD;
    }
    public static String family(InputDevice device){
        String n=device.getName().toLowerCase(Locale.ROOT);
        if(n.contains("xbox")||device.getVendorId()==1118)return "Xbox ou compatível";
        if(n.contains("dualshock")||n.contains("dualsense")||
            n.contains("playstation")||device.getVendorId()==1356)return "PlayStation";
        if(n.contains("nintendo")||n.contains("joy-con")||device.getVendorId()==1406)
            return "Nintendo";
        if(n.contains("8bitdo"))return "8BitDo / genérico";
        return "Controle Android / genérico";
    }
    public static String battery(InputDevice device) {
        // getBatteryState() is available on newer Android versions, but absent on
        // older OS builds and some drivers. Use reflection for safe fallback.
        try{
            Method get=device.getClass().getMethod("getBatteryState");
            Object state=get.invoke(device);
            if(state==null)return "desconhecida (driver)";
            Method present=state.getClass().getMethod("isPresent");
            if(!Boolean.TRUE.equals(present.invoke(state)))
                return "não informada pelo controle";
            Method capacity=state.getClass().getMethod("getCapacity");
            float value=((Number)capacity.invoke(state)).floatValue();
            if(Float.isNaN(value)||value<0||value>1)return "indisponível";
            return Math.round(value*100)+"%";
        }catch(Exception|LinkageError ex){
            return "não suportada pelo Android/driver";
        }
    }
    public static String axisName(int axis){
        switch(axis){
            case MotionEvent.AXIS_X:return "X";
            case MotionEvent.AXIS_Y:return "Y";
            case MotionEvent.AXIS_Z:return "Z";
            case MotionEvent.AXIS_RZ:return "RZ";
            case MotionEvent.AXIS_RX:return "RX";
            case MotionEvent.AXIS_RY:return "RY";
            case MotionEvent.AXIS_HAT_X:return "D-pad X";
            case MotionEvent.AXIS_HAT_Y:return "D-pad Y";
            case MotionEvent.AXIS_LTRIGGER:return "LT";
            case MotionEvent.AXIS_RTRIGGER:return "RT";
            case MotionEvent.AXIS_BRAKE:return "Freio";
            case MotionEvent.AXIS_GAS:return "Acelerador";
            default:return "Axis "+axis;
        }
    }
}
