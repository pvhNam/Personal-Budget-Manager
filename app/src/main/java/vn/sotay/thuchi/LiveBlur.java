package vn.sotay.thuchi;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewTreeObserver;
import java.util.Arrays;

/** Samples the actual scene before each requested frame, without scheduling idle frames. */
final class LiveBlur implements View.OnAttachStateChangeListener {
    private static int captureDepth;
    private final View source,target;
    private final Runnable redraw;
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF destination=new RectF();
    private final int[] origin=new int[2],position=new int[2];
    private final SoftBlur.Workspace workspace=new SoftBlur.Workspace();
    private final float padding;
    private Bitmap sample,frosted;
    private int[] current,previous;
    private int width,height;
    private boolean hasFrame,released;
    private ViewTreeObserver sourceObserver,targetObserver;
    private final ViewTreeObserver.OnPreDrawListener frame=this::beforeFrame;
    private boolean beforeFrame(){
        if(update())redraw.run();
        return true;
    }

    LiveBlur(View source,View target,Runnable redraw) {
        this.source=source;this.target=target;this.redraw=redraw;
        padding=32*target.getResources().getDisplayMetrics().density;
        target.addOnAttachStateChangeListener(this);
        if(target.isAttachedToWindow())start();
    }
    static boolean isCapturing(){return captureDepth>0;}
    private void start() {
        if(released||sourceObserver!=null)return;
        sourceObserver=source.getViewTreeObserver();sourceObserver.addOnPreDrawListener(frame);
        targetObserver=target.getViewTreeObserver();
        if(targetObserver!=sourceObserver)targetObserver.addOnPreDrawListener(frame);
    }
    @Override public void onViewAttachedToWindow(View view){start();}
    @Override public void onViewDetachedFromWindow(View view){stop();}
    private void stop() {
        if(sourceObserver!=null&&sourceObserver.isAlive())sourceObserver.removeOnPreDrawListener(frame);
        if(targetObserver!=null&&targetObserver!=sourceObserver&&targetObserver.isAlive())targetObserver.removeOnPreDrawListener(frame);
        sourceObserver=null;targetObserver=null;
        if(sample!=null){sample.recycle();sample=null;}
        if(frosted!=null){frosted.recycle();frosted=null;}
        current=null;previous=null;hasFrame=false;workspace.clear();
    }
    void release(){released=true;target.removeOnAttachStateChangeListener(this);stop();}

    boolean update() {
        if(released||isCapturing()||!target.isShown()||source.getWidth()==0||source.getHeight()==0)return false;
        int w=target.getWidth(),h=target.getHeight();if(w<=0||h<=0)return false;
        // Capture extra pixels outside the glass so blur does not smear a cropped edge.
        float scale=1f/(3*target.getResources().getDisplayMetrics().density);
        int sw=Math.max(1,(int)Math.ceil((w+2*padding)*scale));
        int sh=Math.max(1,(int)Math.ceil((h+2*padding)*scale));
        if(sample==null||sample.getWidth()!=sw||sample.getHeight()!=sh) {
            if(sample!=null)sample.recycle();if(frosted!=null)frosted.recycle();
            sample=Bitmap.createBitmap(sw,sh,Bitmap.Config.ARGB_8888);
            frosted=Bitmap.createBitmap(sw,sh,Bitmap.Config.ARGB_8888);
            current=new int[sw*sh];previous=new int[sw*sh];hasFrame=false;
        }
        if(width!=w||height!=h){width=w;height=h;hasFrame=false;}
        Canvas capture=new Canvas(sample);capture.drawColor(0xffedf4f7);
        capture.scale(sw/(w+2*padding),sh/(h+2*padding));
        source.getLocationOnScreen(origin);target.getLocationOnScreen(position);
        capture.translate(padding+origin[0]-position[0],padding+origin[1]-position[1]);
        // Parent drawing applies the real ScrollView offset and every child animation.
        captureDepth++;
        try{source.draw(capture);}finally{captureDepth--;}
        sample.getPixels(current,0,sw,0,0,sw,sh);
        if(hasFrame&&Arrays.equals(current,previous))return false;
        System.arraycopy(current,0,previous,0,current.length);
        frosted.setPixels(current,0,sw,0,0,sw,sh);
        SoftBlur.apply(frosted,3,workspace,3);hasFrame=true;return true;
    }
    void draw(Canvas canvas,RectF bounds) {
        if(!hasFrame)update();
        if(frosted==null||!hasFrame)return;
        float px=padding*bounds.width()/width,py=padding*bounds.height()/height;
        destination.set(bounds.left-px,bounds.top-py,bounds.right+px,bounds.bottom+py);
        canvas.drawBitmap(frosted,null,destination,paint);
    }
}
