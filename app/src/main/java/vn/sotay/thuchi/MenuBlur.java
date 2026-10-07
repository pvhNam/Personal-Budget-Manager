package vn.sotay.thuchi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** A live sample of the scene below the floating menu. */
public class MenuBlur extends View {
    private LiveBlur blur;
    private final RectF bounds=new RectF();
    public MenuBlur(Context context,AttributeSet attrs) {
        super(context,attrs);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void bind(View scene) {
        if(blur!=null)blur.release();blur=new LiveBlur(scene,this,this::invalidate);invalidate();
    }
    void refresh(){if(blur!=null&&blur.update())invalidate();}
    @Override protected void onDetachedFromWindow(){if(blur!=null)blur.release();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas) {
        if(blur==null||LiveBlur.isCapturing())return;
        bounds.set(0,0,getWidth(),getHeight());blur.draw(canvas,bounds);
    }
}
