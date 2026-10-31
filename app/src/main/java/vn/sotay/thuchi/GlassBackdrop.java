package vn.sotay.thuchi;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** A separate blurred color layer keeps all foreground text and controls sharp. */
public class GlassBackdrop extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Shader[] glows=new Shader[3];
    private Bitmap frosted;
    private final Rect sourceRect=new Rect();
    private final RectF fullBounds=new RectF();
    private final int[] origin=new int[2], target=new int[2];
    public GlassBackdrop(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        glows[0]=glow(w*.9f,h*.12f,w*.85f,0xff1a2d3e);
        glows[1]=glow(w*.08f,h*.5f,w*.95f,0xff222440);
        glows[2]=glow(w*.95f,h*.88f,w*.9f,0xff172c3a);
        if(frosted!=null)frosted.recycle();
        frosted=Bitmap.createBitmap(Math.max(1,w/6),Math.max(1,h/6),Bitmap.Config.ARGB_8888);
        Canvas sample=new Canvas(frosted);sample.scale(frosted.getWidth()/(float)Math.max(1,w),frosted.getHeight()/(float)Math.max(1,h));
        drawColors(sample);SoftBlur.apply(frosted,5);
    }
    @Override protected void onDraw(Canvas canvas) {
        if(frosted==null){drawColors(canvas);return;}
        paint.setShader(null);paint.setColor(Color.WHITE);paint.setFilterBitmap(true);
        fullBounds.set(0,0,getWidth(),getHeight());canvas.drawBitmap(frosted,null,fullBounds,paint);
    }
    private void drawColors(Canvas canvas) {
        canvas.drawColor(0xff11182d);
        for(Shader shader:glows) {paint.setShader(shader);canvas.drawRect(0,0,getWidth(),getHeight(),paint);}
    }
    void drawBehind(Canvas canvas,View host,RectF bounds) {
        if(frosted==null)return;
        getLocationInWindow(origin);host.getLocationInWindow(target);
        float sx=frosted.getWidth()/(float)getWidth(),sy=frosted.getHeight()/(float)getHeight();
        int x=target[0]-origin[0],y=target[1]-origin[1];
        sourceRect.set(Math.round(x*sx),Math.round(y*sy),Math.round((x+host.getWidth())*sx),Math.round((y+host.getHeight())*sy));
        paint.setShader(null);paint.setFilterBitmap(true);paint.setColor(Color.WHITE);
        canvas.drawBitmap(frosted,sourceRect,bounds,paint);
    }
    private Shader glow(float x, float y, float radius, int color) {
        return new RadialGradient(x,y,Math.max(1,radius),color,color & 0x00ffffff,Shader.TileMode.CLAMP);
    }
}
