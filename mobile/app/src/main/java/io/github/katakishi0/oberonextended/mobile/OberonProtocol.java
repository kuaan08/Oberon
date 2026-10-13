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
        p[0]=(byte)0xFF; // First gamepad active; remaining slots inactive.
        writeStick(p,1,3,state.lx,state.ly,deadzone);
        writeStick(p,5,7,state.rx,state.ry,deadzone);
        write16(p,9,Math.round(state.lt*32767));
        write16(p,11,Math.round(state.rt*32767));
        int b=state.buttons;
        if((b&InputState.A)!=0)p[13]|=(byte)0x80;
        if((b&InputState.B)!=0)p[13]|=0x40;
        if((b&InputState.X)!=0)p[13]|=0x20;
        if((b&InputState.Y)!=0)p[13]|=0x10;
        if((b&InputState.LB)!=0)p[13]|=0x08;
        if((b&InputState.RB)!=0)p[13]|=0x04;
        if((b&InputState.START)!=0)p[13]|=0x02;
        if((b&InputState.VIEW)!=0)p[13]|=0x01;
        if((b&InputState.L3)!=0)p[14]|=0x40;
        if((b&InputState.R3)!=0)p[14]|=0x20;
        if((b&InputState.UP)!=0)p[14]|=0x10;
        if((b&InputState.DOWN)!=0)p[14]|=0x08;
        if((b&InputState.LEFT)!=0)p[14]|=0x04;
        if((b&InputState.RIGHT)!=0)p[14]|=0x02;
        if((b&InputState.GUIDE)!=0)p[14]|=0x01;
        return p;
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
