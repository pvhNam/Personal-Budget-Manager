package vn.sotay.thuchi;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Only the backdrop is blurred; the button's label remains a normal foreground. */
final class FrostedDrawable extends Drawable {
    private final View host;
    private final boolean primary;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF face=new RectF();
    private final Path clip=new Path();
    private final float radius;
    FrostedDrawable(View host,boolean primary) {this.host=host;this.primary=primary;radius=20*host.getResources().getDisplayMetrics().density;}
    @Override protected void onBoundsChange(Rect bounds) {
        face.set(bounds);clip.reset();clip.addRoundRect(face,radius,radius,Path.Direction.CW);
    }
    @Override public void draw(Canvas canvas) {
        int save=canvas.save();canvas.clipPath(clip);
        GlassBackdrop backdrop=host.getRootView().findViewById(R.id.glass_backdrop);
        if(backdrop!=null&&backdrop.getVisibility()==View.VISIBLE)backdrop.drawBehind(canvas,host,face);
        paint.setColor(primary?0xc8246f7c:0x60ffffff);paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(face,radius,radius,paint);
        paint.setColor(primary?0x55ffffff:0xdfffffff);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(host.getResources().getDisplayMetrics().density);
        canvas.drawRoundRect(face,radius,radius,paint);canvas.restoreToCount(save);paint.setStyle(Paint.Style.FILL);
    }
    @Override public void getOutline(Outline outline){outline.setRoundRect(getBounds(),(int)radius);}
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
