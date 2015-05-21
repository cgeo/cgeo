package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Image;
import cgeo.geocaching.R;
import cgeo.geocaching.compatibility.Compatibility;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.Observable;
import rx.Scheduler.Worker;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.Base64;
import android.util.Base64InputStream;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public final class ImageUtils {
    private static final int[] ORIENTATIONS = {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_ROTATE_270
    };

    private static final int[] ROTATION = { 90, 180, 270 };
    private static final int MAX_DISPLAY_IMAGE_XY = 800;

    // Images whose URL contains one of those patterns will not be available on the Images tab
    // for opening into an external application.
    private final static String[] NO_EXTERNAL = { "geocheck.org" };

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
        final Point displaySize = Compatibility.getDisplaySize();
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
        final Point displaySize = Compatibility.getDisplaySize();
        // Restrict image size to 800 x 800 to prevent OOM on tablets
        final int maxWidth = Math.min(displaySize.x - 25, MAX_DISPLAY_IMAGE_XY);
        final int maxHeight = Math.min(displaySize.y - 25, MAX_DISPLAY_IMAGE_XY);
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
            final FileOutputStream out = new FileOutputStream(pathOfOutputImage);
            final BufferedOutputStream bos = new BufferedOutputStream(out);
            bitmap.compress(format, quality, bos);
            bos.flush();
            bos.close();
        } catch (final IOException e) {
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
        final Bitmap image = readDownsampledImage(filePath, maxXY, maxXY);
        if (image == null) {
            return null;
        }
        final BitmapDrawable scaledImage = scaleBitmapTo(image, maxXY, maxXY);
        final File tempImageFile = getOutputImageFile();
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
        } catch (final IOException e) {
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
        if (decodedImage != null) {
            for (int i = 0; i < ORIENTATIONS.length; i++) {
                if (orientation == ORIENTATIONS[i]) {
                    final Matrix matrix = new Matrix();
                    matrix.postRotate(ROTATION[i]);
                    return Bitmap.createBitmap(decodedImage, 0, 0, decodedImage.getWidth(), decodedImage.getHeight(), matrix, true);
                }
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
        final File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "cgeo");

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
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
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
        for (final String entry : patterns) {
            if (StringUtils.containsIgnoreCase(url, entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decode a base64-encoded string and save the result into a file.
     *
     * @param inString the encoded string
     * @param outFile the file to save the decoded result into
     */
    public static void decodeBase64ToFile(final String inString, final File outFile) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            decodeBase64ToStream(inString, out);
        } catch (final IOException e) {
            Log.e("HtmlImage.decodeBase64ToFile: cannot write file for decoded inline image", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Decode a base64-encoded string and save the result into a stream.
     *
     * @param inString
     *            the encoded string
     * @param out
     *            the stream to save the decoded result into
     */
    public static void decodeBase64ToStream(final String inString, final OutputStream out) throws IOException {
        Base64InputStream in = null;
        try {
            in = new Base64InputStream(new ByteArrayInputStream(inString.getBytes(TextUtils.CHARSET_ASCII)), Base64.DEFAULT);
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static BitmapDrawable getTransparent1x1Drawable(final Resources res) {
        return new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.image_no_placement));
    }

    /**
     * Add images present in the HTML description to the existing collection.
     *  @param images a collection of images
     * @param geocode the common title for images in the description
     * @param htmlText the HTML description to be parsed, can be repeated
     */
    public static void addImagesFromHtml(final Collection<Image> images, final String geocode, final String... htmlText) {
        final Set<String> urls = new LinkedHashSet<>();
        for (final Image image : images) {
            urls.add(image.getUrl());
        }
        for (final String text: htmlText) {
            Html.fromHtml(StringUtils.defaultString(text), new ImageGetter() {
                @Override
                public Drawable getDrawable(final String source) {
                    if (!urls.contains(source) && canBeOpenedExternally(source)) {
                        images.add(new Image(source, StringUtils.defaultString(geocode)));
                        urls.add(source);
                    }
                    return null;
                }
            }, null);
        }
    }

    /**
     * Container which can hold a drawable (initially an empty one) and get a newer version when it
     * becomes available. It also invalidates the view the container belongs to, so that it is
     * redrawn properly.
     * <p/>
     * When a new version of the drawable is available, it is put into a queue and, if needed (no other elements
     * waiting in the queue), a refresh is launched on the UI thread. This refresh will empty the queue (including
     * elements arrived in the meantime) and ensures that the view is uploaded only once all the queued requests have
     * been handled.
     */
    public static class ContainerDrawable extends BitmapDrawable implements Action1<Drawable> {
        final private static Object lock = new Object(); // Used to lock the queue to determine if a refresh needs to be scheduled
        final private static LinkedBlockingQueue<ImmutablePair<ContainerDrawable, Drawable>> REDRAW_QUEUE = new LinkedBlockingQueue<>();
        final private static Set<TextView> VIEWS = new HashSet<>();  // Modified only on the UI thread
        final private static Worker UI_WORKER = AndroidSchedulers.mainThread().createWorker();
        final private static Action0 REDRAW_QUEUED_DRAWABLES = new Action0() {
            @Override
            public void call() {
                redrawQueuedDrawables();
            }
        };

        private Drawable drawable;
        final protected WeakReference<TextView> viewRef;

        @SuppressWarnings("deprecation")
        public ContainerDrawable(@NonNull final TextView view, final Observable<? extends Drawable> drawableObservable) {
            viewRef = new WeakReference<>(view);
            drawable = null;
            setBounds(0, 0, 0, 0);
            drawableObservable.subscribe(this);
        }

        @Override
        public final void draw(final Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }

        @Override
        public final void call(final Drawable newDrawable) {
            final boolean needsRedraw;
            synchronized (lock) {
                // Check for emptyness inside the call to match the behaviour in redrawQueuedDrawables().
                needsRedraw = REDRAW_QUEUE.isEmpty();
                REDRAW_QUEUE.add(ImmutablePair.of(this, newDrawable));
            }
            if (needsRedraw) {
                UI_WORKER.schedule(REDRAW_QUEUED_DRAWABLES);
            }
        }

        /**
         * Update the container with the new drawable. Called on the UI thread.
         *
         * @param newDrawable the new drawable
         * @return the view to update
         * @throws IllegalStateException if the view does not exist anymore
         */
        protected TextView updateDrawable(final Drawable newDrawable) {
            setBounds(0, 0, newDrawable.getIntrinsicWidth(), newDrawable.getIntrinsicHeight());
            drawable = newDrawable;
            final TextView view = viewRef.get();
            if (view == null) {
                throw new IllegalStateException("text view has been deleted while its images are still in use");
            }
            return view;
        }

        private static void redrawQueuedDrawables() {
            if (!REDRAW_QUEUE.isEmpty()) {
                // Add a small margin so that drawables arriving between the beginning of the allocation and the draining
                // of the queue might be absorbed without reallocation.
                final List<ImmutablePair<ContainerDrawable, Drawable>> toRedraw = new ArrayList<>(REDRAW_QUEUE.size() + 16);
                synchronized (lock) {
                    // Empty the queue inside the lock to match the check done in call().
                    REDRAW_QUEUE.drainTo(toRedraw);
                }
                for (final ImmutablePair<ContainerDrawable, Drawable> redrawable : toRedraw) {
                    VIEWS.add(redrawable.left.updateDrawable(redrawable.right));
                }
                for (final TextView view : VIEWS) {
                    view.setText(view.getText());
                }
                VIEWS.clear();
            }
        }

    }

    /**
     * Image that automatically scales to fit a line of text in the containing {@link TextView}.
     */
    public final static class LineHeightContainerDrawable extends ContainerDrawable {
        public LineHeightContainerDrawable(@NonNull final TextView view, final Observable<? extends Drawable> drawableObservable) {
            super(view, drawableObservable);
        }

        @Override
        protected TextView updateDrawable(final Drawable newDrawable) {
            final TextView view = super.updateDrawable(newDrawable);
            setBounds(scaleImageToLineHeight(newDrawable, view));
            return view;
        }
    }

    public static boolean canBeOpenedExternally(final String source) {
        return !containsPattern(source, NO_EXTERNAL);
    }

    public static Rect scaleImageToLineHeight(final Drawable drawable, final TextView view) {
        final int lineHeight = (int) (view.getLineHeight() * 0.8);
        final int width = drawable.getIntrinsicWidth() * lineHeight / drawable.getIntrinsicHeight();
        return new Rect(0, 0, width, lineHeight);
    }

    public static Bitmap convertToBitmap(final Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // handle solid colors, which have no width
        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
