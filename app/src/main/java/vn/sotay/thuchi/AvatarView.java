package vn.sotay.thuchi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

/** Circular photo with an initial while a photo is absent or still loading. */
public final class AvatarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap avatar;
    private String initial = "B";

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setFocusable(true);
    }

    public void setProfile(String name, Bitmap photo) {
        String trimmed = name == null ? "" : name.trim();
        initial = trimmed.isEmpty() ? "B" : new String(Character.toChars(trimmed.codePointAt(0))).toUpperCase(Locale.ROOT);
        avatar = photo;
        setContentDescription(getContext().getString(R.string.avatar_accessibility,
                trimmed.isEmpty() ? getContext().getString(R.string.avatar_default_name) : trimmed));
        invalidate();
    }

    public boolean hasPhoto() { return avatar != null; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - getResources().getDisplayMetrics().density;
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff247c83);
        if (avatar != null && !avatar.isRecycled()) {
            BitmapShader shader = new BitmapShader(avatar, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            float scale = radius * 2 / Math.min(avatar.getWidth(), avatar.getHeight());
            Matrix transform = new Matrix();
            transform.setScale(scale, scale);
            transform.postTranslate(cx - avatar.getWidth() * scale / 2, cy - avatar.getHeight() * scale / 2);
            shader.setLocalMatrix(transform);
            paint.setShader(shader);
        }
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);
        if (avatar == null) {
            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(radius * .85f);
            canvas.drawText(initial, cx, cy - (paint.ascent() + paint.descent()) / 2, paint);
        }
        if (isPressed() || isFocused()) {
            paint.setColor(0x33000000);
            canvas.drawCircle(cx, cy, radius, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getResources().getDisplayMetrics().density * 2);
        paint.setColor(0xccffffff);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    @Override protected void drawableStateChanged() { super.drawableStateChanged(); invalidate(); }
}
