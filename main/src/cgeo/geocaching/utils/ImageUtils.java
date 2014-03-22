package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.compatibility.Compatibility;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ImageUtils {
    private static final int[] ORIENTATIONS = new int[] {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_ROTATE_270
    };

    private static final int[] ROTATION = new int[] { 90, 180, 270 };

    private ImageUtils() {
        // Do not let this class be instantiated, this is a utility class.
    }

    /**
     * Scales a bitmap to the device display size.
     *
     * @param image
     *            The image Bitmap representation to scale
     * @return BitmapDrawable The scaled image
     */
    public static BitmapDrawable scaleBitmapToFitDisplay(@NonNull final Bitmap image) {
        Point displaySize = Compatibility.getDisplaySize();
        final int maxWidth = displaySize.x - 25;
        final int maxHeight = displaySize.y - 25;
        return scaleBitmapTo(image, maxWidth, maxHeight);
    }

    /**
     * Reads and scales an image file to the device display size.
     *
     * @param filename
     *            The image file to read and scale
     * @return Bitmap The scaled image or Null if source image can't be read
     */
    @Nullable
    public static Bitmap readAndScaleImageToFitDisplay(@NonNull final String filename) {
        Point displaySize = Compatibility.getDisplaySize();
        final int maxWidth = displaySize.x - 25;
        final int maxHeight = displaySize.y - 25;
        final Bitmap image = readDownsampledImage(filename, maxWidth, maxHeight);
        if (image == null) {
            return null;
        }
        final BitmapDrawable scaledImage = scaleBitmapTo(image, maxWidth, maxHeight);
        return scaledImage.getBitmap();
    }

    /**
     * Scales a bitmap to the given bounds if it is larger, otherwise returns the original bitmap.
     *
     * @param image
     *            The bitmap to scale
     * @return BitmapDrawable The scaled image
     */
    @NonNull
    private static BitmapDrawable scaleBitmapTo(@NonNull final Bitmap image, final int maxWidth, final int maxHeight) {
        final CgeoApplication app = CgeoApplication.getInstance();
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
     * Scales an image to the desired bounds and encodes to file.
     *
     * @param filePath
     *            Image to read
     * @param maxXY
     *            bounds
     * @return filename and path, <tt>null</tt> if something fails
     */
    @Nullable
    public static String readScaleAndWriteImage(@NonNull final String filePath, final int maxXY) {
        if (maxXY <= 0) {
            return filePath;
        }
        Bitmap image = readDownsampledImage(filePath, maxXY, maxXY);
        if (image == null) {
            return null;
        }
        final BitmapDrawable scaledImage = scaleBitmapTo(image, maxXY, maxXY);
        final File tempImageFile = ImageUtils.getOutputImageFile();
        if (tempImageFile == null) {
            Log.e("ImageUtils.readScaleAndWriteImage: unable to write scaled image");
            return null;
        }
        final String uploadFilename = tempImageFile.getPath();
        storeBitmap(scaledImage.getBitmap(), Bitmap.CompressFormat.JPEG, 75, uploadFilename);
        return uploadFilename;
    }

    /**
     * Reads and scales an image file with downsampling in one step to prevent memory consumption.
     *
     * @param filePath
     *            The file to read
     * @param maxX
     *            The desired width
     * @param maxY
     *            The desired height
     * @return Bitmap the image or null if file can't be read
     */
    @Nullable
    public static Bitmap readDownsampledImage(@NonNull final String filePath, final int maxX, final int maxY) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            final ExifInterface exif = new ExifInterface(filePath);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            Log.e("ImageUtils.readDownsampledImage", e);
        }
        final BitmapFactory.Options sizeOnlyOptions = new BitmapFactory.Options();
        sizeOnlyOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, sizeOnlyOptions);
        final int myMaxXY = Math.max(sizeOnlyOptions.outHeight, sizeOnlyOptions.outWidth);
        final int maxXY = Math.max(maxX, maxY);
        final int sampleSize = myMaxXY / maxXY;
        final BitmapFactory.Options sampleOptions = new BitmapFactory.Options();
        if (sampleSize > 1) {
            sampleOptions.inSampleSize = sampleSize;
        }
        final Bitmap decodedImage = BitmapFactory.decodeFile(filePath, sampleOptions);
        for (int i = 0; i < ORIENTATIONS.length; i++) {
            if (orientation == ORIENTATIONS[i]) {
                final Matrix matrix = new Matrix();
                matrix.postRotate(ROTATION[i]);
                return Bitmap.createBitmap(decodedImage, 0, 0, decodedImage.getWidth(), decodedImage.getHeight(), matrix, true);
            }
        }
        return decodedImage;
    }

    /** Create a File for saving an image or video
     *
     * @return the temporary image file to use, or <tt>null</tt> if the media directory could
     * not be created.
     * */
    @Nullable
    public static File getOutputImageFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Compatibility.getExternalPictureDir(), "cgeo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!FileUtils.mkdirs(mediaStorageDir)) {
                Log.e("ImageUtils.getOutputImageFile: cannot create media storage directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    }

    @Nullable
    public static Uri getOutputImageFileUri() {
        final File file = getOutputImageFile();
        if (file == null) {
            return null;
        }
        return Uri.fromFile(file);
    }

    /**
     * Check if the URL contains one of the given substrings.
     *
     * @param url the URL to check
     * @param patterns a list of substrings to check against
     * @return <tt>true</tt> if the URL contains at least one of the patterns, <tt>false</tt> otherwise
     */
    public static boolean containsPattern(final String url, final String[] patterns) {
        for (String entry : patterns) {
            if (StringUtils.containsIgnoreCase(url, entry)) {
                return true;
            }
        }
        return false;
    }
}
