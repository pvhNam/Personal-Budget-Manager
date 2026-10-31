package vn.sotay.thuchi;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** A live frosted background; form fields are drawn sharply above it. */
final class DialogBlurDrawable extends Drawable {
    private final LiveBlur blur;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF face=new RectF();
    private final Path clip=new Path();
    private final float radius,stroke;
    DialogBlurDrawable(View scene,View host) {
        radius=28*host.getResources().getDisplayMetrics().density;
        stroke=host.getResources().getDisplayMetrics().density;
        blur=new LiveBlur(scene,host,this::invalidateSelf);
    }
    @Override protected void onBoundsChange(Rect bounds) {
        face.set(bounds);clip.reset();clip.addRoundRect(face,radius,radius,Path.Direction.CW);
    }
    @Override public void draw(Canvas canvas) {
        int saved=canvas.save();canvas.clipPath(clip);
        blur.draw(canvas,face);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xb8182239);canvas.drawRoundRect(face,radius,radius,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor(0x426f85a6);
        canvas.drawRoundRect(face,radius,radius,paint);canvas.restoreToCount(saved);
    }
    @Override public void getOutline(Outline outline){outline.setRoundRect(getBounds(),radius);}
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
