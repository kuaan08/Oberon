package io.github.katakishi0.oberonextended.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.InputDevice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Stable-ish per-device routing for up to four independently operated gamepads. */
public final class ControllerSlots {
    private final InputState[] pads=new InputState[4];
    private final Map<Integer,Integer> assigned=new HashMap<>();
    private final SharedPreferences prefs;

    public ControllerSlots(Context context,InputState primary){
        pads[0]=primary;
        for(int i=1;i<4;i++){pads[i]=new InputState();pads[i].setUsePhysical(true);}
        prefs=context.getSharedPreferences(MobileUi.PREFS,Context.MODE_PRIVATE);
    }
    public synchronized void refresh(){
        ArrayList<Integer> ids=new ArrayList<>();
        for(int id:InputDevice.getDeviceIds()){
            if(InputDevices.isController(InputDevice.getDevice(id)))ids.add(id);
        }
        Collections.sort(ids);
        int preferred=prefs.getInt("selected_controller",-1);
        if(ids.remove(Integer.valueOf(preferred)))ids.add(0,preferred);
        if(!prefs.getBoolean("multi_controller_enabled",true)&&ids.size()>1)
            ids.subList(1,ids.size()).clear();
        // Release the slot of an unplugged device without clearing other devices.
        ArrayList<Integer> stale=new ArrayList<>();
        for(Map.Entry<Integer,Integer> entry:assigned.entrySet())
            if(!ids.contains(entry.getKey()) || ids.indexOf(entry.getKey())>=4){
                if(entry.getValue()>0)pads[entry.getValue()].clearPhysical();
                stale.add(entry.getKey());
            }
        for(Integer id:stale)assigned.remove(id);
        // Preserve established slots while connected; new pads claim the next free slot.
        for(Integer id:ids){
            if(assigned.containsKey(id))continue;
            for(int i=0;i<4;i++){
                if(!assigned.containsValue(i)){
                    assigned.put(id,i);
                    if(i>0)pads[i].clearPhysical();
                    break;
                }
            }
        }
    }
    public synchronized InputState forDevice(int deviceId){
        refresh();
        Integer slot=assigned.get(deviceId);
        return slot==null?null:pads[slot];
    }
    public synchronized InputState.Snapshot[] snapshots(){
        refresh();
        InputState.Snapshot[] snapshots=new InputState.Snapshot[4];
        if(!pads[0].usePhysical()) {
            snapshots[0]=pads[0].snapshot();return snapshots;
        }
        // Keep slot zero active: the original Xbox receiver uses this sentinel
        // to validate packets; other slots have their own presence marker.
        snapshots[0]=pads[0].snapshot();
        for(int i=1;i<4;i++)if(assigned.containsValue(i))
            snapshots[i]=pads[i].snapshot();
        return snapshots;
    }
    public synchronized int deviceForSlot(int slot){
        for(Map.Entry<Integer,Integer> entry:assigned.entrySet())
            if(entry.getValue()==slot)return entry.getKey();
        return -1;
    }
    public synchronized int activeCount(){
        refresh();return assigned.size();
    }
    public synchronized void clear(){
        for(int i=0;i<4;i++)pads[i].clearPhysical();
        assigned.clear();
    }
}
