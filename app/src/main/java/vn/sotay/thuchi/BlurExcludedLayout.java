package vn.sotay.thuchi;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/** Keep the floating menu out of its own backdrop sample. */
public class BlurExcludedLayout extends FrameLayout {
    public BlurExcludedLayout(Context context,AttributeSet attrs){super(context,attrs);setWillNotDraw(false);}
    @Override public void draw(Canvas canvas){if(!LiveBlur.isCapturing())super.draw(canvas);}
    @Override protected void dispatchDraw(Canvas canvas){if(!LiveBlur.isCapturing())super.dispatchDraw(canvas);}
}
