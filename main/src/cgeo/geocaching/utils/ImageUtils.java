package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.compatibility.Compatibility;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class ImageUtils {

    private ImageUtils() {
        // Do not let this class be instantiated, this is a utility class.
    }

    /**
     * Scales a bitmap to the given bounds
     *
     * @param image
     *            The bitmap to scale
     * @return BitmapDrawable The scaled image
     */
    public static BitmapDrawable scaleBitmapToFitDisplay(final Bitmap image) {
        Point displaySize = Compatibility.getDisplaySize();
        final int maxWidth = displaySize.x - 25;
        final int maxHeight = displaySize.y - 25;
        return scaleBitmapTo(image, maxWidth, maxHeight);
    }

    /**
     * Scales a bitmap to the given bounds if it is larger, otherwise returns the original bitmap.
     *
     * @param image
     *            The bitmap to scale
     * @return BitmapDrawable The scaled image
     */
    public static BitmapDrawable scaleBitmapTo(final Bitmap image, final int maxWidth, final int maxHeight) {
        final cgeoapplication app = cgeoapplication.getInstance();
        Bitmap result = image;
        int width = image.getWidth();
        int height = image.getHeight();

        if (width > maxWidth || height > maxHeight) {
            final double ratio = Math.min((double) maxHeight / (double) height, (double) maxWidth / (double) width);
            width = (int) Math.ceil(width * ratio);
            height = (int) Math.ceil(height * ratio);
            result = Bitmap.createScaledBitmap(image, width, height, true);
        }

        final BitmapDrawable resultDrawable = new BitmapDrawable(app.getResources(), result);
        resultDrawable.setBounds(new Rect(0, 0, width, height));
        return resultDrawable;
    }

    /**
     * Store a bitmap to file.
     *
     * @param bitmap
     *            The bitmap to store
     * @param format
     *            The image format
     * @param quality
     *            The image quality
     * @param pathOfOutputImage
     *            Path to store to
     */
    public static void storeBitmap(final Bitmap bitmap, final Bitmap.CompressFormat format, final int quality, final String pathOfOutputImage) {
        try {
            FileOutputStream out = new FileOutputStream(pathOfOutputImage);
            BufferedOutputStream bos = new BufferedOutputStream(out);
            bitmap.compress(format, quality, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            Log.e("ImageHelper.storeBitmap", e);
        }
    }
}
