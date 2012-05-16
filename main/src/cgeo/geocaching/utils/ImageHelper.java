package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.Display;
import android.view.WindowManager;

public class ImageHelper {

    // Do not let this class be instantiated, this is a utility class.
    private ImageHelper() {
    }

    /**
     * Scales a bitmap to the given bounds
     *
     * @param image
     *            The bitmap to scale
     * @return BitmapDrawable The scaled image
     */
    public static BitmapDrawable scaleBitmapToFitDisplay(final Bitmap image) {
        final cgeoapplication app = cgeoapplication.getInstance();
        final Display display = ((WindowManager) app.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int maxWidth = display.getWidth() - 25;
        final int maxHeight = display.getHeight() - 25;

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

}
