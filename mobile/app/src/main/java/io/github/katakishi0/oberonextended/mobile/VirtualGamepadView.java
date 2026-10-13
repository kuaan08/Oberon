package io.github.katakishi0.oberonextended.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * Full-screen multi-touch controller. Two dedicated pointer IDs own the sticks
 * while other fingers press triggers/buttons. Portrait and landscape have
 * separately designed, non-overlapping hit targets.
 */
public final class VirtualGamepadView extends View {
    private final InputState input;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private MobileUi theme;
    private boolean haptics;
    private boolean animations=true;
    private float sizeScale=1f,stickScale=1f,faceScale=1f;
    private int opacity=100;
    private int leftPointer=-1,rightPointer=-1,heldButtons;
    private float lx,ly,rx,ry,lt,rt;
    private long pressAt;

    private static final class Layout {
        final boolean portrait;
        final float w,h,stickR,faceR,gap;
        float leftX,leftY,rightX,rightY,faceX,faceY,dpadX,dpadY,topY,shoulderY,centerY;
        Layout(float width,float height,float scale,float sticks,float faces){
            w=width;h=height;portrait=h>w;
            if(portrait){
                stickR=w*.145f*scale*sticks;
                faceR=w*.067f*scale*faces;
                gap=w*.097f*scale*faces;
                leftX=w*.245f;leftY=h*.76f;rightX=w*.755f;rightY=h*.76f;
                faceX=w*.75f;faceY=h*.385f;
                dpadX=w*.245f;dpadY=h*.385f;
                topY=h*.175f;shoulderY=h*.245f;centerY=h*.565f;
            }else{
                stickR=Math.min(h*.17f,w*.105f)*scale*sticks;
                faceR=h*.071f*scale*faces;
                gap=h*.113f*scale*faces;
                leftX=w*.19f;leftY=h*.57f;rightX=w*.677f;rightY=h*.65f;
                faceX=w*.843f;faceY=h*.47f;
                dpadX=w*.385f;dpadY=h*.75f;
                topY=h*.22f;shoulderY=h*.33f;centerY=h*.465f;
            }
        }
        float centerLeft(){return portrait?w*.40f:w*.47f;}
        float centerRight(){return portrait?w*.60f:w*.55f;}
        float guideY(){return portrait?h*.63f:h*.60f;}
        float triggerW(){return portrait?w*.18f:w*.13f;}
        float triggerH(){return portrait?h*.050f:h*.080f;}
        float triggerX(int index){
            if(portrait)return w*(index==0?.14f:index==1?.36f:index==2?.64f:.86f);
            return w*(index==0?.14f:index==1?.31f:index==2?.69f:.86f);
        }
    }

    public VirtualGamepadView(Context context,InputState input){
        super(context);
        this.input=input;setFocusable(true);
        theme=MobileUi.from(context);
        setLayerType(View.LAYER_TYPE_HARDWARE,null);
    }
    public void setPalette(MobileUi palette){theme=palette;invalidate();}
    public void setHaptics(boolean value){haptics=value;}
    public void setAnimations(boolean value){animations=value;}
    public void setScale(float value){sizeScale=Math.max(.8f,Math.min(1.2f,value));invalidate();}
    public void setIndividualSizes(float stick,float face){
        stickScale=Math.max(.7f,Math.min(1.35f,stick));
        faceScale=Math.max(.7f,Math.min(1.35f,face));invalidate();
    }
    public void setOpacity(int percent){opacity=Math.max(45,Math.min(100,percent));invalidate();}

    private void fill(int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);}
    private void stroke(int color,float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);paint.setStrokeWidth(width);
    }
    private void title(Canvas c,String label,float x,float y,float size,int color){
        fill(color);
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size);
        c.drawText(label,x,y-(paint.ascent()+paint.descent())/2,paint);
    }
    private void rounded(Canvas c,float cx,float cy,float width,float height,int color,float radius){
        fill(color);
        c.drawRoundRect(new RectF(cx-width/2,cy-height/2,cx+width/2,cy+height/2),
            radius,radius,paint);
    }
    private void face(Canvas c,String label,float x,float y,float radius,int button,int letterColor){
        boolean pressed=(heldButtons&button)!=0;
        fill(pressed?theme.accent:theme.surface);
        c.drawCircle(x,y,radius,paint);
        stroke(pressed?theme.accent:theme.border,Math.max(2,radius*.075f));
        c.drawCircle(x,y,radius,paint);
        if(pressed) {
            fill(Color.argb(50,255,255,255));c.drawCircle(x,y,radius*.81f,paint);
        }
        title(c,label,x,y,radius*.90f,pressed?theme.accentText:letterColor);
    }
    private void stick(Canvas c,float x,float y,float radius,float dx,float dy,String id){
        fill(theme.surface);c.drawCircle(x,y,radius,paint);
        stroke(theme.border,Math.max(2,radius*.05f));c.drawCircle(x,y,radius,paint);
        stroke(theme.accent,Math.max(2,radius*.023f));c.drawCircle(x,y,radius*.68f,paint);
        fill(theme.accent);
        c.drawCircle(x+dx*radius*.64f,y+dy*radius*.64f,radius*.38f,paint);
        title(c,id,x,y-radius-20,Math.max(13,radius*.19f),theme.muted);
    }
    private void trigger(Canvas c,Layout l,int i,String name,boolean pressed,float pressure) {
        float x=l.triggerX(i);
        float y=i==0||i==3?l.topY:l.shoulderY;
        float width=l.triggerW(),height=l.triggerH();
        rounded(c,x,y,width,height,pressed?theme.accent:theme.surface,height*.32f);
        title(c,name,x,y,Math.max(13,height*.39f),pressed?theme.accentText:theme.accent);
        if(pressure>0 && (i==0||i==3)) {
            rounded(c,x,y+height*.43f,width*pressure,height*.08f,theme.accent,height*.04f);
        }
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        Layout l=new Layout(getWidth(),getHeight(),sizeScale,stickScale,faceScale);
        if(l.w<=0||l.h<=0)return;
        c.drawColor(theme.bg);
        int layer=c.saveLayerAlpha(0,0,l.w,l.h,Math.round(opacity*255/100f));
        boolean portrait=l.portrait;
        title(c,"CONTROLE VIRTUAL",l.w*.50f,l.h*(portrait?.115f:.110f),
            Math.max(13,Math.min(l.w,l.h)*.026f),theme.muted);
        trigger(c,l,0,"LT",lt>0,lt);
        trigger(c,l,1,"LB",(heldButtons&InputState.LB)!=0,0);
        trigger(c,l,2,"RB",(heldButtons&InputState.RB)!=0,0);
        trigger(c,l,3,"RT",rt>0,rt);

        float centerSize=portrait?l.w*.046f:l.h*.068f;
        face(c,"VIEW",l.centerLeft(),l.centerY,centerSize,InputState.VIEW,theme.text);
        face(c,"MENU",l.centerRight(),l.centerY,centerSize,InputState.START,theme.text);
        face(c,"XBOX",l.w*.5f,l.guideY(),
            centerSize*.82f,InputState.GUIDE,theme.accent);

        float d=l.gap;
        float dpadR=l.faceR*.79f;
        face(c,"↑",l.dpadX,l.dpadY-d,dpadR,InputState.UP,theme.text);
        face(c,"↓",l.dpadX,l.dpadY+d,dpadR,InputState.DOWN,theme.text);
        face(c,"←",l.dpadX-d,l.dpadY,dpadR,InputState.LEFT,theme.text);
        face(c,"→",l.dpadX+d,l.dpadY,dpadR,InputState.RIGHT,theme.text);

        face(c,"Y",l.faceX,l.faceY-d,l.faceR,InputState.Y,Color.rgb(255,208,83));
        face(c,"A",l.faceX,l.faceY+d,l.faceR,InputState.A,Color.rgb(123,232,135));
        face(c,"X",l.faceX-d,l.faceY,l.faceR,InputState.X,Color.rgb(110,184,255));
        face(c,"B",l.faceX+d,l.faceY,l.faceR,InputState.B,Color.rgb(255,126,118));

        stick(c,l.leftX,l.leftY,l.stickR,lx,ly,"L");
        stick(c,l.rightX,l.rightY,l.stickR,rx,ry,"R");
        float small=portrait?l.w*.042f:l.h*.045f;
        face(c,"L3",l.leftX,l.leftY+l.stickR*1.50f,small,
            InputState.L3,theme.text);
        face(c,"R3",l.rightX,l.rightY+l.stickR*1.50f,small,
            InputState.R3,theme.text);

        c.restoreToCount(layer);
        if(animations && SystemClock.uptimeMillis()-pressAt<155){
            postInvalidateDelayed(16);
        }
    }

    private static boolean near(float x,float y,float cx,float cy,float radius){
        return Math.hypot(x-cx,y-cy)<=radius;
    }
    private int buttonAt(Layout l,float x,float y){
        float g=l.gap,r=l.faceR*1.30f;
        if(near(x,y,l.faceX,l.faceY-g,r))return InputState.Y;
        if(near(x,y,l.faceX,l.faceY+g,r))return InputState.A;
        if(near(x,y,l.faceX-g,l.faceY,r))return InputState.X;
        if(near(x,y,l.faceX+g,l.faceY,r))return InputState.B;
        r=l.faceR*1.18f;
        if(near(x,y,l.dpadX,l.dpadY-g,r))return InputState.UP;
        if(near(x,y,l.dpadX,l.dpadY+g,r))return InputState.DOWN;
        if(near(x,y,l.dpadX-g,l.dpadY,r))return InputState.LEFT;
        if(near(x,y,l.dpadX+g,l.dpadY,r))return InputState.RIGHT;
        r=(l.portrait?l.w*.046f:l.h*.068f)*1.40f;
        if(near(x,y,l.centerLeft(),l.centerY,r))return InputState.VIEW;
        if(near(x,y,l.centerRight(),l.centerY,r))return InputState.START;
        if(near(x,y,l.w*.50f,l.guideY(),r*.85f))return InputState.GUIDE;
        r=(l.portrait?l.w*.042f:l.h*.045f)*1.45f;
        if(near(x,y,l.leftX,l.leftY+l.stickR*1.5f,r))return InputState.L3;
        if(near(x,y,l.rightX,l.rightY+l.stickR*1.5f,r))return InputState.R3;
        for(int i=0;i<4;i++){
            float bx=l.triggerX(i),by=i==0||i==3?l.topY:l.shoulderY;
            if(Math.abs(x-bx)<=l.triggerW()*.6f &&
                Math.abs(y-by)<=l.triggerH()*.68f){
                return i==0?-1:i==1?InputState.LB:i==2?InputState.RB:-2;
            }
        }
        return 0;
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        int action=e.getActionMasked(),changed=e.getActionIndex();
        if(action==MotionEvent.ACTION_CANCEL){release();return true;}
        Layout l=new Layout(getWidth(),getHeight(),sizeScale,stickScale,faceScale);
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
            int id=e.getPointerId(changed);
            float x=e.getX(changed),y=e.getY(changed);
            if(leftPointer==-1 &&
                near(x,y,l.leftX,l.leftY,l.stickR*1.25f))leftPointer=id;
            else if(rightPointer==-1 &&
                near(x,y,l.rightX,l.rightY,l.stickR*1.25f))rightPointer=id;
        }
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
            int id=e.getPointerId(changed);
            if(id==leftPointer)leftPointer=-1;
            if(id==rightPointer)rightPointer=-1;
        }
        float nlx=0,nly=0,nrx=0,nry=0,nlt=0,nrt=0;
        int pressed=0;
        for(int i=0;i<e.getPointerCount();i++){
            if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP)&&i==changed)
                continue;
            int id=e.getPointerId(i);
            float x=e.getX(i),y=e.getY(i);
            if(id==leftPointer){
                nlx=limit((x-l.leftX)/(l.stickR*.70f));
                nly=limit((y-l.leftY)/(l.stickR*.70f));continue;
            }
            if(id==rightPointer){
                nrx=limit((x-l.rightX)/(l.stickR*.70f));
                nry=limit((y-l.rightY)/(l.stickR*.70f));continue;
            }
            int bit=buttonAt(l,x,y);
            if(bit==-1)nlt=1;
            else if(bit==-2)nrt=1;
            else pressed|=bit;
        }
        if((pressed&~heldButtons)!=0 || (nlt>0&&lt==0) || (nrt>0&&rt==0)) {
            pressAt=SystemClock.uptimeMillis();
            if(haptics)performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
        lx=nlx;ly=nly;rx=nrx;ry=nry;lt=nlt;rt=nrt;heldButtons=pressed;
        input.setTouch(lx,ly,rx,ry,lt,rt,pressed);
        invalidate();
        return true;
    }
    private static float limit(float f){return Math.max(-1,Math.min(1,f));}
    public void release(){
        leftPointer=rightPointer=-1;
        lx=ly=rx=ry=lt=rt=0;heldButtons=0;
        input.setTouch(0,0,0,0,0,0,0);
        invalidate();
    }
}
