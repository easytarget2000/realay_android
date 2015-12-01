package org.eztarget.realay.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.util.Log;

/**
 * Created by michel on 01/12/14.
 *
 */
public class ImageEditor {

    private static final String TAG = ImageEditor.class.getSimpleName();

    public static Bitmap cropBitmapSquared(Bitmap source) {
        if (source == null) return null;
        final int width = source.getWidth();
        final int height = source.getHeight();

        if (height == width) return source;

        if (width >= height) {
            return Bitmap.createBitmap(
                    source,
                    width / 2 - height / 2,
                    0,
                    height,
                    height
            );

        } else {
            return Bitmap.createBitmap(
                    source,
                    0,
                    height / 2 - width / 2,
                    width,
                    width
            );
        }
    }

    public static Bitmap cropBitmapCircle(Bitmap source) {
        if (source == null) return null;

        final int diameter = Math.min(source.getWidth(), source.getHeight());

        final Bitmap circleBitmap;
        try {
            circleBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError error) {
            Log.e(TAG, error.toString());
            return null;
        }

        final BitmapShader shader;
        shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);

        final float radius = diameter / 2f;

        final Canvas canvas = new Canvas(circleBitmap);
        canvas.drawCircle(radius, radius, radius, paint);

        return circleBitmap;
    }

    public static Bitmap getLargeNotificationIcon(Context context, Bitmap original) {
        if (context == null) return original;
        if (original == null) return null;

        final Resources res = context.getResources();
        final int width;

        final int api = Build.VERSION.SDK_INT;
        if (api < Build.VERSION_CODES.LOLLIPOP){
            if (api < Build.VERSION_CODES.HONEYCOMB) {
                width = 128;
            } else {
                width = (int) (res.getDimension(android.R.dimen.notification_large_icon_width));
            }

            final Bitmap scaledIcon = ImageEditor.limitDimensions(original, width);
            return ImageEditor.cropBitmapSquared(scaledIcon);
        } else{
            // Circles look more appropriate in Material Android.
            width = (int) (res.getDimension(android.R.dimen.notification_large_icon_width));
            return ImageEditor.getCircleBitmap(original, width);
        }
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap, int radius) {
        if (bitmap == null) return null;
        Bitmap scaledBitmap;
        if (bitmap.getWidth() != radius || bitmap.getHeight() != radius) {
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, radius, radius, false);
        } else {
            scaledBitmap = bitmap;
        }
        final int sideLength = scaledBitmap.getHeight();
        Bitmap croppedBmp = Bitmap.createBitmap(sideLength, sideLength, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(croppedBmp);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, sideLength, sideLength);

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);

        final float sideLengthHalf = sideLength * .5f;

        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(sideLengthHalf, sideLengthHalf, sideLengthHalf, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaledBitmap, rect, rect, paint);

        return croppedBmp;
    }

    public static final int HI_RES_MAX_PIXEL = 2048;

    public static final int LO_RES_MAX_PIXEL = 164;

    public static Bitmap limitDimensions(Bitmap source, final int size) {
        if (source == null) return null;

        final int width = source.getWidth();
        final int height = source.getHeight();

        if (width <= size && height <= size) return source;

        final int dstWidth, dstHeight;
        if (width > height) {
            final float ratio = (float) height / (float) width;
            dstWidth = size;
            dstHeight = (int) (size * ratio);
        } else {
            final float ratio = (float) width / (float) height;
            dstHeight = size;
            dstWidth = (int) (size * ratio);
        }

        return Bitmap.createScaledBitmap(source, dstWidth, dstHeight, true);
    }
}
