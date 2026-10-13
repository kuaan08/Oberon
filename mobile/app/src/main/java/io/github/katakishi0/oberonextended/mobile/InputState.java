package io.github.katakishi0.oberonextended.mobile;

public final class InputState {
    public static final int A=1, B=2, X=4, Y=8, VIEW=16, GUIDE=32, START=64,
        L3=128, R3=256, LB=512, RB=1024, UP=2048, DOWN=4096,
        LEFT=8192, RIGHT=16384;
    public static final class Snapshot {
        public final float lx,ly,rx,ry,lt,rt;
        public final int buttons;
        public Snapshot(float lx,float ly,float rx,float ry,float lt,float rt,int buttons) {
            this.lx=clamp(lx,-1,1);this.ly=clamp(ly,-1,1);
            this.rx=clamp(rx,-1,1);this.ry=clamp(ry,-1,1);
            this.lt=clamp(lt,0,1);this.rt=clamp(rt,0,1);
            this.buttons=buttons;
        }
    }
    private float touchLx,touchLy,touchRx,touchRy,touchLt,touchRt;
    private float physicalLx,physicalLy,physicalRx,physicalRy,physicalLt,physicalRt;
    private float digitalLt,digitalRt;
    private int touchButtons,physicalButtons,physicalHat;
    private boolean usePhysical;
    private float deadzone=0.10f;
    private String lastController="No controller detected";

    public synchronized void setUsePhysical(boolean value) {
        usePhysical=value;
        touchButtons=0;touchLx=touchLy=touchRx=touchRy=touchLt=touchRt=0;
    }
    public synchronized boolean usePhysical(){return usePhysical;}
    public synchronized void setDeadzone(float value){deadzone=clamp(value,0,0.35f);}
    public synchronized float deadzone(){return deadzone;}
    public synchronized String lastController(){return lastController;}
    public synchronized void setControllerName(String value){lastController=value;}
    public synchronized void setTouch(float lx,float ly,float rx,float ry,float lt,float rt,int buttons){
        touchLx=lx;touchLy=ly;touchRx=rx;touchRy=ry;
        touchLt=lt;touchRt=rt;touchButtons=buttons;
    }
    public synchronized void setPhysicalAxes(float lx,float ly,float rx,float ry,float lt,float rt) {
        physicalLx=lx;physicalLy=ly;physicalRx=rx;physicalRy=ry;
        physicalLt=lt;physicalRt=rt;
    }
    public synchronized void setPhysicalDigitalTrigger(boolean left,float pressure){
        if(left)digitalLt=pressure;else digitalRt=pressure;
    }
    public synchronized void setPhysicalHat(float x,float y){
        physicalHat=0;
        if(x>.5f)physicalHat|=RIGHT;
        if(x<-.5f)physicalHat|=LEFT;
        if(y>.5f)physicalHat|=DOWN;
        if(y<-.5f)physicalHat|=UP;
    }
    public synchronized void setPhysicalButton(int bit,boolean pressed){
        if(pressed)physicalButtons|=bit;else physicalButtons&=~bit;
    }
    public synchronized Snapshot snapshot() {
        return usePhysical
            ? new Snapshot(physicalLx,physicalLy,physicalRx,physicalRy,
                Math.max(physicalLt,digitalLt),Math.max(physicalRt,digitalRt),
                physicalButtons|physicalHat)
            : new Snapshot(touchLx,touchLy,touchRx,touchRy,touchLt,touchRt,touchButtons);
    }
    /** Clears only the physical gamepad; does not disturb active touch controls. */
    public synchronized void clearPhysical() {
        physicalLx=physicalLy=physicalRx=physicalRy=physicalLt=physicalRt=0;
        digitalLt=digitalRt=0;
        physicalButtons=physicalHat=0;
    }
    public synchronized void reset() {
        touchLx=touchLy=touchRx=touchRy=touchLt=touchRt=0;
        physicalLx=physicalLy=physicalRx=physicalRy=physicalLt=physicalRt=0;
        digitalLt=digitalRt=0;
        touchButtons=physicalButtons=physicalHat=0;
    }
    private static float clamp(float v,float min,float max){
        if(!Float.isFinite(v))return 0;
        return Math.max(min,Math.min(max,v));
    }
}
