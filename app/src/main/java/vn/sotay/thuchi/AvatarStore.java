package vn.sotay.thuchi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/** Owns only the app's small, private copy; picker URI access is never needed later. */
final class AvatarStore {
    static final int SIZE = 512;
    private final Context context;
    private final AtomicFile file;

    AvatarStore(Context context) {
        this(context, new File(context.getFilesDir(), "profile-avatar.jpg"));
    }

    AvatarStore(Context context, File destination) {
        this.context = context.getApplicationContext();
        file = new AtomicFile(destination);
    }

    Bitmap load() throws IOException {
        try (InputStream input = file.openRead()) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap == null) throw new IOException("Invalid saved avatar");
            return bitmap;
        } catch (FileNotFoundException missing) { return null; }
    }

    Bitmap replace(Uri uri) throws IOException {
        Bitmap decoded = decode(uri);
        Bitmap cropped = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(cropped);
            canvas.drawColor(0xffedf4f7);
            int side = Math.min(decoded.getWidth(), decoded.getHeight());
            int left = (decoded.getWidth() - side) / 2;
            int top = (decoded.getHeight() - side) / 2;
            canvas.drawBitmap(decoded, new Rect(left, top, left + side, top + side),
                    new Rect(0, 0, SIZE, SIZE), new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            FileOutputStream output = null;
            try {
                output = file.startWrite();
                if (!cropped.compress(Bitmap.CompressFormat.JPEG, 90, output))
                    throw new IOException("Could not encode avatar");
                file.finishWrite(output);
            } catch (IOException | RuntimeException error) {
                file.failWrite(output);
                throw error;
            }
            return cropped;
        } catch (IOException | RuntimeException error) {
            cropped.recycle();
            throw error;
        } finally {
            decoded.recycle();
        }
    }

    void remove() { file.delete(); }

    private Bitmap decode(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= 28) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri),
                    (decoder, info, source) -> {
                        int width = info.getSize().getWidth(), height = info.getSize().getHeight();
                        float scale = Math.min(1f, (float) SIZE / Math.max(width, height));
                        decoder.setTargetSize(Math.max(1, Math.round(width * scale)),
                                Math.max(1, Math.round(height * scale)));
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    });
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = open(uri)) { BitmapFactory.decodeStream(input, null, options); }
        if (options.outWidth <= 0 || options.outHeight <= 0) throw new IOException("Not an image");
        options.inJustDecodeBounds = false;
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > SIZE * 2)
            options.inSampleSize *= 2;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded;
        try (InputStream input = open(uri)) { decoded = BitmapFactory.decodeStream(input, null, options); }
        if (decoded == null) throw new IOException("Could not decode image");
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream input = open(uri)) {
            orientation = new ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException ignored) { /* Formats without EXIF already have their display orientation. */ }
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_180: matrix.setRotate(180); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.setScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE: matrix.setRotate(90); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_90: matrix.setRotate(90); break;
            case ExifInterface.ORIENTATION_TRANSVERSE: matrix.setRotate(-90); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_270: matrix.setRotate(-90); break;
            default: return decoded;
        }
        Bitmap rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);
        if (rotated != decoded) decoded.recycle();
        return rotated;
    }

    private InputStream open(Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) throw new IOException("Image is unavailable");
        return input;
    }
}
