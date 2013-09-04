package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.compatibility.Compatibility;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    /**
     * Scales an image to the desired boundings and encodes to file.
     *
     * @param filePath
     *            Image to read
     * @param maxXY
     *            boundings
     * @return String filename and path, NULL if something fails
     */
    public static String readScaleAndWriteImage(final String filePath, final int maxXY) {
        if (maxXY <= 0) {
            return filePath;
        }
        BitmapFactory.Options sizeOnlyOptions = new BitmapFactory.Options();
        sizeOnlyOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, sizeOnlyOptions);
        final int myMaxXY = Math.max(sizeOnlyOptions.outHeight, sizeOnlyOptions.outWidth);
        final int sampleSize = myMaxXY / maxXY;
        Bitmap image;
        if (sampleSize > 1) {
            BitmapFactory.Options sampleOptions = new BitmapFactory.Options();
            sampleOptions.inSampleSize = sampleSize;
            image = BitmapFactory.decodeFile(filePath, sampleOptions);
        } else {
            image = BitmapFactory.decodeFile(filePath);
        }
        if (image == null) {
            return null;
        }
        final BitmapDrawable scaledImage = scaleBitmapTo(image, maxXY, maxXY);
        final String uploadFilename = ImageUtils.getOutputImageFile().getPath();
        storeBitmap(scaledImage.getBitmap(), Bitmap.CompressFormat.JPEG, 75, uploadFilename);
        return uploadFilename;
    }

    /** Create a File for saving an image or video */
    public static File getOutputImageFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
    
        File mediaStorageDir = new File(Compatibility.getExternalPictureDir(), "cgeo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
    
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!FileUtils.mkdirs(mediaStorageDir)) {
                return null;
            }
        }
    
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    }

    public static Uri getOutputImageFileUri() {
        final File file = getOutputImageFile();
        if (file == null) {
            return null;
        }
        return Uri.fromFile(file);
    }
}
