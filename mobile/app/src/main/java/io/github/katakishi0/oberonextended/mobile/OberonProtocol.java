package io.github.katakishi0.oberonextended.mobile;

/**
 * Wire format used by the original Oberon Xbox UWP receiver:
 * 4 slots, 25 bytes each, LE int16 axes/triggers, SDL button bit layout.
 * Android Y+ points down; the Xbox receiver inverts Y during decoding.
 */
public final class OberonProtocol {
    public static final int PORT=26401, PACKET_SIZE=100;
    private OberonProtocol(){}
    public static byte[] encode(InputState.Snapshot state,float deadzone) {
        byte[] p=new byte[PACKET_SIZE];
        encodeSlot(p,0,state,deadzone);
        return p;
    }
    /** 4 x 25-byte slots as decoded by the Xbox UWP receiver. Null means disconnected. */
    public static byte[] encode(InputState.Snapshot[] slots,float deadzone){
        byte[] result=new byte[PACKET_SIZE];
        for(int i=0;i<4;i++){
            if(slots!=null && i<slots.length && slots[i]!=null)
                encodeSlot(result,i,slots[i],deadzone);
        }
        return result;
    }
    private static void encodeSlot(byte[] p,int slot,InputState.Snapshot state,float deadzone){
        if(slot<0||slot>=4||state==null)return;
        int off=slot*25;
        p[off]=(byte)0xFF;
        writeStick(p,off+1,off+3,state.lx,state.ly,deadzone);
        writeStick(p,off+5,off+7,state.rx,state.ry,deadzone);
        write16(p,off+9,Math.round(state.lt*32767));
        write16(p,off+11,Math.round(state.rt*32767));
        int b=state.buttons;
        if((b&InputState.A)!=0)p[off+13]|=(byte)0x80;
        if((b&InputState.B)!=0)p[off+13]|=0x40;
        if((b&InputState.X)!=0)p[off+13]|=0x20;
        if((b&InputState.Y)!=0)p[off+13]|=0x10;
        if((b&InputState.LB)!=0)p[off+13]|=0x08;
        if((b&InputState.RB)!=0)p[off+13]|=0x04;
        if((b&InputState.START)!=0)p[off+13]|=0x02;
        if((b&InputState.VIEW)!=0)p[off+13]|=0x01;
        if((b&InputState.L3)!=0)p[off+14]|=0x40;
        if((b&InputState.R3)!=0)p[off+14]|=0x20;
        if((b&InputState.UP)!=0)p[off+14]|=0x10;
        if((b&InputState.DOWN)!=0)p[off+14]|=0x08;
        if((b&InputState.LEFT)!=0)p[off+14]|=0x04;
        if((b&InputState.RIGHT)!=0)p[off+14]|=0x02;
        if((b&InputState.GUIDE)!=0)p[off+14]|=0x01;
    }
    private static void writeStick(byte[] p,int xi,int yi,float x,float y,float dz){
        dz=Math.max(0,Math.min(.35f,dz));
        double magnitude=Math.hypot(x,y);
        if(magnitude<=dz)return;
        double corrected=Math.min(1,(magnitude-dz)/(1-dz));
        write16(p,xi,(int)Math.round((x/magnitude)*corrected*32767));
        write16(p,yi,(int)Math.round((y/magnitude)*corrected*32767));
    }
    private static void write16(byte[] p,int i,int value){
        int v=Math.max(-32767,Math.min(32767,value));
        p[i]=(byte)v;p[i+1]=(byte)(v>>>8);
    }
}
