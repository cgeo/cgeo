package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.Display;
import android.view.WindowManager;

public class ImageHelper {
    /**
     * Scales a bitmap to the given bounds
     *
     * @param image
     *            The bitmap to scale
     * @return BitmapDrawable The scaled image
     */
    public static BitmapDrawable scaleBitmapToFitDisplay(Bitmap image) {
        final Display display = ((WindowManager) cgeoapplication.getInstance().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int maxWidth = display.getWidth() - 25;
        final int maxHeight = display.getHeight() - 25;

        final int imgWidth = image.getWidth();
        final int imgHeight = image.getHeight();

        int width;
        int height;

        Bitmap result = null;
        if (imgWidth > maxWidth || imgHeight > maxHeight) {
            final double ratio = Math.min((double) maxHeight / (double) imgHeight, (double) maxWidth / (double) imgWidth);
            width = (int) Math.ceil(imgWidth * ratio);
            height = (int) Math.ceil(imgHeight * ratio);

            try {
                result = Bitmap.createScaledBitmap(image, width, height, true);
            } catch (Exception e) {
                Log.d("ImageHelper.scaleBitmapToFitDisplay: Failed to scale image");
                return null;
            }
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
