package io.github.katakishi0.oberonextended.mobile;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

/**
 * A responsive multi-touch controller; every pointer is independently mapped
 * on each event, so face buttons, triggers and sticks can be held together.
 */
public final class VirtualGamepadView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final InputState input;
    private boolean light;
    private int accent=Color.rgb(110,234,148),surface=Color.rgb(29,49,36),
        foreground=Color.WHITE,muted=Color.rgb(147,173,154);
    private float leftX,leftY,rightX,rightY;
    private int holding;
    public VirtualGamepadView(Context context,InputState input){
        super(context);this.input=input;setLayerType(View.LAYER_TYPE_HARDWARE,null);
        setFocusable(true);
    }
    public void setLight(boolean l){
        light=l;accent=Color.rgb(l?26:110,l?143:234,l?70:148);
        surface=Color.rgb(l?230:29,l?239:49,l?232:36);
        foreground=l?Color.rgb(29,49,34):Color.WHITE;
        muted=l?Color.rgb(95,112,99):Color.rgb(147,173,154);
        invalidate();
    }
    private void color(int c){paint.setColor(c);paint.setStyle(Paint.Style.FILL);}
    private void ring(float cx,float cy,float r,boolean active){
        color(surface);canvas.drawCircle(cx,cy,r,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(2,r*.045f));
        paint.setColor(active?accent:muted);canvas.drawCircle(cx,cy,r,paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private Canvas canvas;
    private void label(String text,float x,float y,float size,int tint){
        color(tint);paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        paint.setTextSize(size);canvas.drawText(text,x,y,paint);
    }
    private void bubble(String text,float cx,float cy,float r,boolean active,int color){
        color(active?accent:surface);canvas.drawCircle(cx,cy,r,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setColor(active?accent:muted);
        paint.setStrokeWidth(2);canvas.drawCircle(cx,cy,r,paint);
        paint.setStyle(Paint.Style.FILL);
        label(text,cx,cy+r*.3f,r*.8f,active?(light?Color.WHITE:Color.rgb(7,20,10)):color);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);canvas=c;
        float w=getWidth(),h=getHeight(),s=Math.min(w/900f,h/430f);
        int bg=light?Color.rgb(245,249,246):Color.rgb(12,23,16);
        c.drawColor(bg);
        float lsx=w*.20f, lsy=h*.43f, rsx=w*.63f, rsy=h*.76f;
        float radius=Math.min(77*s,h*.185f);
        ring(lsx,lsy,radius,leftX!=0||leftY!=0);
        color(accent);c.drawCircle(lsx+leftX*radius*.55f,lsy+leftY*radius*.55f,radius*.39f,paint);
        ring(rsx,rsy,radius*.83f,rightX!=0||rightY!=0);
        color(accent);c.drawCircle(rsx+rightX*radius*.46f,rsy+rightY*radius*.46f,radius*.32f,paint);
        label("L",lsx,lsy-radius-11*s,15*s,muted);
        label("R",rsx,rsy-radius*.85f-11*s,15*s,muted);
        float dx=w*.20f,dy=h*.80f,dp=34*s;
        bubble("↑",dx,dy-dp,16*s,(holding&InputState.UP)!=0,foreground);
        bubble("↓",dx,dy+dp,16*s,(holding&InputState.DOWN)!=0,foreground);
        bubble("←",dx-dp,dy,16*s,(holding&InputState.LEFT)!=0,foreground);
        bubble("→",dx+dp,dy,16*s,(holding&InputState.RIGHT)!=0,foreground);
        float bx=w*.84f,by=h*.46f,sep=37*s;
        bubble("Y",bx,by-sep,25*s,(holding&InputState.Y)!=0,foreground);
        bubble("A",bx,by+sep,25*s,(holding&InputState.A)!=0,foreground);
        bubble("X",bx-sep,by,25*s,(holding&InputState.X)!=0,foreground);
        bubble("B",bx+sep,by,25*s,(holding&InputState.B)!=0,foreground);
        float top=h*.10f,tabWidth=70*s;
        label("LT",w*.17f,top,16*s,accent);label("LB",w*.34f,top,16*s,accent);
        label("RB",w*.67f,top,16*s,accent);label("RT",w*.84f,top,16*s,accent);
        bubble("VIEW",w*.44f,h*.3f,22*s,(holding&InputState.VIEW)!=0,foreground);
        bubble("MENU",w*.56f,h*.3f,22*s,(holding&InputState.START)!=0,foreground);
        canvas=null;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        int masked=event.getActionMasked();
        if(masked==MotionEvent.ACTION_CANCEL){release();return true;}
        float w=getWidth(),h=getHeight(),s=Math.min(w/900f,h/430f);
        int skip=(masked==MotionEvent.ACTION_UP||masked==MotionEvent.ACTION_POINTER_UP)
            ? event.getActionIndex():-1;
        float lx=0,ly=0,rx=0,ry=0,lt=0,rt=0;
        int buttons=0;
        for(int i=0;i<event.getPointerCount();i++){
            if(i==skip)continue;
            float x=event.getX(i),y=event.getY(i);
            float leftDist=(float)Math.hypot(x-w*.20f,y-h*.43f);
            float rightDist=(float)Math.hypot(x-w*.63f,y-h*.76f);
            float r=Math.min(100*s,h*.24f);
            if(leftDist<r){
                lx=clamp((x-w*.20f)/(r*.65f));
                ly=clamp((y-h*.43f)/(r*.65f));
            }else if(rightDist<r){
                rx=clamp((x-w*.63f)/(r*.65f));ry=clamp((y-h*.76f)/(r*.65f));
            }else if(y<h*.20f) {
                if(x<w*.25f)lt=1;else if(x<w*.49f)buttons|=InputState.LB;
                else if(x>w*.76f)rt=1;else if(x>w*.51f)buttons|=InputState.RB;
            }else if(in(x,y,w*.44f,h*.3f,27*s))buttons|=InputState.VIEW;
            else if(in(x,y,w*.56f,h*.3f,27*s))buttons|=InputState.START;
            else if(x>w*.72f && y<h*.71f){
                float bx=w*.84f,by=h*.46f,sep=37*s;
                if(in(x,y,bx,by-sep,30*s))buttons|=InputState.Y;
                else if(in(x,y,bx,by+sep,30*s))buttons|=InputState.A;
                else if(in(x,y,bx-sep,by,30*s))buttons|=InputState.X;
                else if(in(x,y,bx+sep,by,30*s))buttons|=InputState.B;
            }else if(x<w*.35f && y>h*.60f){
                float dx=x-w*.20f,dy=y-h*.80f;
                if(Math.abs(dx)>Math.abs(dy)){
                    if(dx>16*s)buttons|=InputState.RIGHT;
                    if(dx< -16*s)buttons|=InputState.LEFT;
                }else{
                    if(dy>16*s)buttons|=InputState.DOWN;
                    if(dy< -16*s)buttons|=InputState.UP;
                }
            }
        }
        leftX=lx;leftY=ly;rightX=rx;rightY=ry;holding=buttons;
        input.setTouch(lx,ly,rx,ry,lt,rt,buttons);
        invalidate();
        return true;
    }
    private static boolean in(float x,float y,float cx,float cy,float r){
        return Math.hypot(x-cx,y-cy)<=r;
    }
    private static float clamp(float v){return Math.max(-1,Math.min(1,v));}
    public void release(){
        leftX=leftY=rightX=rightY=0;holding=0;
        input.setTouch(0,0,0,0,0,0,0);
        invalidate();
    }
}
