package vn.sotay.thuchi;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** One focusable, labeled touch target per series, including zero-value columns. */
public class BarColumn extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long amount, maximum;
    private final int color;
    private Shader fill;
    private float reveal=1;
    private android.animation.ValueAnimator animation;
    public BarColumn(Context context) { this(context,0,1,0xff247c83); }
    public BarColumn(Context context, long amount, long maximum, int color) {
        super(context); this.amount=amount; this.maximum=Math.max(1,maximum); this.color=color;
        setFocusable(true); setClickable(true);
        setBackgroundResource(android.R.drawable.list_selector_background);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        fill=new LinearGradient(0,0,0,Math.max(1,h),color,(color & 0x00ffffff)|0xb0000000,Shader.TileMode.CLAMP);
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(!Motion.enabled())return;
        animation=android.animation.ValueAnimator.ofFloat(0,1);reveal=0;
        animation.setDuration(420);animation.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animation.addUpdateListener(value->{reveal=(float)value.getAnimatedValue();invalidate();});animation.start();
    }
    @Override protected void onDetachedFromWindow(){if(animation!=null)animation.cancel();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density=getResources().getDisplayMetrics().density;
        float bottom=getHeight()-8*density, top=12*density;
        paint.setShader(null);paint.setColor(0x18395769);paint.setStrokeWidth(density);
        for(int i=0;i<=4;i++) {float y=top+(bottom-top)*i/4;canvas.drawLine(0,y,getWidth(),y,paint);}
        float height=amount==0?2*density:Math.max(4*density,(bottom-top)*amount/maximum);
        float left=getWidth()/2f-10*density,right=getWidth()/2f+10*density;
        paint.setColor(Color.WHITE);
        paint.setShader(fill);
        canvas.drawRoundRect(left,bottom-height*reveal,right,bottom,6*density,6*density,paint);
        paint.setShader(null);
    }
}
