package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.Display;
import android.view.WindowManager;

/**
 * Helper class for image operations. Code that is use in several classes should go here.
 * 
 * @author marco-jacob
 * 
 */
public class ImageHelper {

    /**
     * Private constructor to prevent from creating this static helper class
     */
    private ImageHelper() {
    }

    /**
     * Fit the bitmap to the given display bounds and therefore scale the bitmap
     * or keep the original image if it is smaller than the display.
     *
     * @param image
     *            The bitmap to scale
     * @return BitmapDrawable The image fitting the bounds of display-25 or NULL if given image is null
     */
    public static BitmapDrawable scaleBitmapToFitDisplay(Bitmap image) {
        if (image == null) {
            return null;
        }

        final Display display = ((WindowManager) cgeoapplication.getInstance().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int maxWidth = display.getWidth() - 25;
        final int maxHeight = display.getHeight() - 25;

        final int imgWidth = image.getWidth();
        final int imgHeight = image.getHeight();

        int width;
        int height;

        Bitmap result;
        if (imgWidth > maxWidth || imgHeight > maxHeight) {
            final double ratio = Math.min((double) maxHeight / (double) imgHeight, (double) maxWidth / (double) imgWidth);
            width = (int) Math.ceil(imgWidth * ratio);
            height = (int) Math.ceil(imgHeight * ratio);
            result = Bitmap.createScaledBitmap(image, width, height, true);
        } else {
            result = image;
            width = imgWidth;
            height = imgHeight;
        }

        BitmapDrawable resultDrawable = new BitmapDrawable(cgeoapplication.getInstance().getResources(), result);
        resultDrawable.setBounds(new Rect(0, 0, width, height));
        return resultDrawable;
    }

}
