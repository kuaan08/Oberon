package io.github.katakishi0.oberonextended.mobile;

import org.junit.Test;
import static org.junit.Assert.*;

public class OberonProtocolTest {
    private static int shortAt(byte[] data,int offset){
        return (short)((data[offset]&0xff)|((data[offset+1]&0xff)<<8));
    }
    @Test public void neutralPacketMatchesXboxReceiver(){
        byte[] data=OberonProtocol.encode(new InputState.Snapshot(0,0,0,0,0,0,0),.10f);
        assertEquals(100,data.length);assertEquals(255,data[0]&255);
        assertEquals(0,data[25]);assertEquals(0,data[50]);assertEquals(0,data[75]);
    }
    @Test public void buttonsAndTriggersMatchOriginalProtocol(){
        InputState.Snapshot state=new InputState.Snapshot(1,-1,0,0,1,.5f,
            InputState.A|InputState.X|InputState.LB|InputState.START|InputState.UP);
        byte[] data=OberonProtocol.encode(state,0);
        assertEquals(0xAA,data[13]&0xFF);
        assertEquals(0x10,data[14]&0xFF);
        assertEquals(32767,shortAt(data,9));
        assertTrue(shortAt(data,11)>=16300);
        assertTrue(shortAt(data,3)<0);
    }
    @Test public void deadzoneAndFullTilt(){
        byte[] slight=OberonProtocol.encode(new InputState.Snapshot(.05f,.05f,0,0,0,0,0),.10f);
        assertEquals(0,shortAt(slight,1));assertEquals(0,shortAt(slight,3));
        byte[] full=OberonProtocol.encode(new InputState.Snapshot(1,0,0,0,0,0,0),.10f);
        assertEquals(32767,shortAt(full,1));
    }
    @Test public void physicalAndTouchStateCanBeSwitched(){
        InputState s=new InputState();
        s.setTouch(0,0,0,0,0,0,InputState.A);
        s.setPhysicalButton(InputState.B,true);
        assertEquals(InputState.A,s.snapshot().buttons);
        s.setUsePhysical(true);
        assertEquals(InputState.B,s.snapshot().buttons);
    }

    @Test public void fourControllerPacketHasDistinctActiveSlots(){
        InputState.Snapshot first=new InputState.Snapshot(1,0,0,0,0,0,InputState.A);
        InputState.Snapshot second=new InputState.Snapshot(0,0,0,0,.5f,0,InputState.B);
        InputState.Snapshot fourth=new InputState.Snapshot(0,0,0,-1,0,1,InputState.X);
        byte[] packet=OberonProtocol.encode(new InputState.Snapshot[]{
            first,second,null,fourth
        },0);
        assertEquals(100,packet.length);
        assertEquals(255,packet[0]&255);
        assertEquals(255,packet[25]&255);
        assertEquals(0,packet[50]&255);
        assertEquals(255,packet[75]&255);
        assertEquals(0x80,packet[13]&255);
        assertEquals(0x40,packet[38]&255);
        assertEquals(0x20,packet[88]&255);
        assertEquals(32767,shortAt(packet,1));
        assertTrue(shortAt(packet,34)>=16000);
        assertEquals(32767,shortAt(packet,86));
    }
    @Test public void disconnectedSecondSlotIsFullyZeroed(){
        byte[] bytes=OberonProtocol.encode(new InputState.Snapshot[]{
            new InputState.Snapshot(0,0,0,0,0,0,0),null,null,null
        },.1f);
        for(int i=25;i<100;i++)assertEquals(0,bytes[i]);
    }
}
