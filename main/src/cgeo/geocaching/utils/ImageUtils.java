package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.storage.LocalStorage;

import android.app.Application;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Base64;
import android.util.Base64InputStream;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import androidx.exifinterface.media.ExifInterface;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.atomic.AtomicLong;
import static java.io.File.separator;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class ImageUtils {

    private static final int[] ORIENTATIONS = {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_ROTATE_270
    };

    private static final AtomicLong IMG_COUNTER = new AtomicLong(0);

    private static final int[] ROTATION = { 90, 180, 270 };
    private static final int MAX_DISPLAY_IMAGE_XY = 800;

    // Images whose URL contains one of those patterns will not be available on the Images tab
    // for opening into an external application.
    private static final String[] NO_EXTERNAL = { "geocheck.org" };

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
    @NonNull
    public static BitmapDrawable scaleBitmapToFitDisplay(@NonNull final Bitmap image) {
        final Point displaySize = DisplayUtils.getDisplaySize();
        final int maxWidth = displaySize.x - 25;
        final int maxHeight = displaySize.y - 25;
        return scaleBitmapTo(image, maxWidth, maxHeight);
    }

    /**
     * Reads and scales an image to the device display size.
     *
     * @param imageData
     *            The image data to read and scale
     * @return Bitmap The scaled image or Null if source image can't be read
     */
    @Nullable
    public static Bitmap readAndScaleImageToFitDisplay(@NonNull final Uri imageData) {
        final Point displaySize = DisplayUtils.getDisplaySize();
        // Restrict image size to 800 x 800 to prevent OOM on tablets
        final int maxWidth = Math.min(displaySize.x - 25, MAX_DISPLAY_IMAGE_XY);
        final int maxHeight = Math.min(displaySize.y - 25, MAX_DISPLAY_IMAGE_XY);

        final Bitmap image = readDownsampledImage(imageData, maxWidth, maxHeight);
        if (image == null) {
            return null;
        }
        final BitmapDrawable scaledImage = scaleBitmapTo(image, maxWidth, maxHeight);
        return scaledImage.getBitmap();
    }

    /**
     * Scales a bitmap to the given bounds if it is larger, otherwise returns the original bitmap (except when "force" is set to true)
     *
     * @param image
     *            The bitmap to scale
     * @return BitmapDrawable The scaled image
     */
    @NonNull
    private static BitmapDrawable scaleBitmapTo(@NonNull final Bitmap image, final int maxWidth, final int maxHeight) {
        final Application app = CgeoApplication.getInstance();
        Bitmap result = image;
        int width = image.getWidth();
        int height = image.getHeight();
        final int realMaxWidth = maxWidth <= 0 ? width : maxWidth;
        final int realMaxHeight = maxHeight <= 0 ? height : maxHeight;
        final boolean imageTooLarge = width > realMaxWidth || height > realMaxHeight;

        if (imageTooLarge) {
            final double ratio = Math.min((double) realMaxHeight / (double) height, (double) realMaxWidth / (double) width);
            width = (int) Math.ceil(width * ratio);
            height = (int) Math.ceil(height * ratio);
            result = Bitmap.createScaledBitmap(image, width, height, true);
        }

        final BitmapDrawable resultDrawable = new BitmapDrawable(app.getResources(), result);
        resultDrawable.setBounds(new Rect(0, 0, width, height));

        return resultDrawable;
    }

    /**
     * Store a bitmap to uri.
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
    public static void storeBitmap(final Bitmap bitmap, final Bitmap.CompressFormat format, final int quality, final Uri targetUri) {
        final BufferedOutputStream bos = null;
        try {
            bitmap.compress(format, quality, CgeoApplication.getInstance().getApplicationContext().getContentResolver().openOutputStream(targetUri));
        } catch (final IOException e) {
            Log.e("ImageHelper.storeBitmap", e);
        } finally {
            IOUtils.closeQuietly(bos);
        }
    }

    public static class ScaleImageResult {
        public final Uri imageUri;
        public final int width;
        public final int height;
        public final boolean wasCopied;

        public ScaleImageResult(final Uri imageUri, final int width, final int height, final boolean wasCopied) {
            this.imageUri = imageUri;
            this.width = width;
            this.height = height;
            this.wasCopied = wasCopied;
        }
    }

    /**
     * This method will scale down a given image and remove EXIF information. During this process the image might be copied,
     * so returned Uri might not be same than the one put in.
     *
     * @param originalImageUri Image to read
     * @param maxXY bounds. If <= 0 then no scaling will happen. This might also mean that the image is not copied (depends on preserveOriginal parameter)
     * @param preserveOriginal if true then a copy will always be made and the original image is not deleted. If false and a copy is made, then the original uri is deleted.
     * @return scale image result, <tt>null</tt> if something fails
     */
    @Nullable
    public static ScaleImageResult readScaleAndWriteImage(@NonNull final Uri originalImageUri, final int maxXY, final boolean preserveOriginal) {

        if (maxXY <= 0 && !preserveOriginal) {
            final BitmapFactory.Options sizeOnlyOptions = getBitmapSizeOptions(openImageStream(originalImageUri));
            return new ScaleImageResult(originalImageUri, sizeOnlyOptions.outWidth, sizeOnlyOptions.outHeight, false);
        }
        final Bitmap image = readDownsampledImage(originalImageUri, maxXY, maxXY);
        if (image == null) {
            return null;
        }

        final Uri newImageUri = createNewImageUri(false);
        if (newImageUri == null) {
            Log.e("ImageUtils.readScaleAndWriteImage: unable to write scaled image");
            return null;
        }

        final BitmapDrawable scaledImage = scaleBitmapTo(image, maxXY, maxXY);
        storeBitmap(scaledImage.getBitmap(), Bitmap.CompressFormat.JPEG, 75, newImageUri);

        if (!preserveOriginal) {
            deleteImage(originalImageUri);
        }
        return new ScaleImageResult(newImageUri, scaledImage.getBitmap().getWidth(), scaledImage.getBitmap().getHeight(), true);
    }

    /**
     * Reads and scales an image with downsampling in one step to prevent memory consumption.
     *
     * @param imageUri image to read
     * @param maxX The desired width. If <= 0 then actual bitmap width is used
     * @param maxY The desired height. If <= 0 then actual bitmap height is used
     * @return Bitmap the image or null if image can't be read
     */
    @Nullable
    private static Bitmap readDownsampledImage(@NonNull final Uri imageUri, final int maxX, final int maxY) {

        final int orientation = getImageOrientation(imageUri);
        final BitmapFactory.Options sizeOnlyOptions = getBitmapSizeOptions(openImageStream(imageUri));
        final int myMaxXY = Math.max(sizeOnlyOptions.outHeight, sizeOnlyOptions.outWidth);
        final int maxXY = Math.max(maxX <= 0 ? sizeOnlyOptions.outWidth : maxX, maxY <= 0 ? sizeOnlyOptions.outHeight : maxY);
        final int sampleSize = maxXY <= 0 ? 1 : myMaxXY / maxXY;
        final BitmapFactory.Options sampleOptions = new BitmapFactory.Options();
        if (sampleSize > 1) {
            sampleOptions.inSampleSize = sampleSize;
        }

        try (InputStream imageStream = openImageStream(imageUri)) {
            if (imageStream == null) {
                return null;
            }
            final Bitmap decodedImage = BitmapFactory.decodeStream(imageStream, null, sampleOptions);
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
        } catch (final IOException e) {
            Log.e("ImageUtils.readDownsampledImage(decode)", e);
        }
        return null;
    }

    private static int getImageOrientation(@NonNull final Uri imageUri) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream imageStream = openImageStream(imageUri)) {
            if (imageStream != null) {
                final ExifInterface exif = new ExifInterface(imageStream);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (final IOException e) {
            Log.e("ImageUtils.getImageOrientation(ExifIf)", e);
        }
        return orientation;
    }

    /** stream will be consumed and closed by method */
    @NonNull
    private static BitmapFactory.Options getBitmapSizeOptions(@NonNull final InputStream imageStream) {
        if (imageStream == null) {
            return null;
        }
        BitmapFactory.Options sizeOnlyOptions = null;
        try {
            sizeOnlyOptions = new BitmapFactory.Options();
            sizeOnlyOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(imageStream, null, sizeOnlyOptions);
        } finally {
            IOUtils.closeQuietly(imageStream);
        }

        return sizeOnlyOptions;
    }

    @Nullable
    public static ImmutablePair<Integer, Integer> getImageSize(@Nullable final Uri imageData) {
        if (imageData == null) {
            return null;
        }
        try (InputStream imageStream = openImageStream(imageData)) {
            if (imageStream == null) {
                return null;
            }
            final Bitmap bm = BitmapFactory.decodeStream(imageStream);
            return bm == null ? null : new ImmutablePair<>(bm.getWidth(), bm.getHeight());
        } catch (IOException e) {
            Log.e("ImageUtils.getImageSize", e);
        }
        return null;
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
            in = new Base64InputStream(new ByteArrayInputStream(inString.getBytes(StandardCharsets.US_ASCII)), Base64.DEFAULT);
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @NonNull
    public static BitmapDrawable getTransparent1x1Drawable(final Resources res) {
        return new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.image_no_placement));
    }

    /**
     * Add images present in the HTML description to the existing collection.
     * @param images a collection of images
     * @param geocode the common title for images in the description
     * @param htmlText the HTML description to be parsed, can be repeated
     */
    public static void addImagesFromHtml(final Collection<Image> images, final String geocode, final String... htmlText) {
        final Set<String> urls = new LinkedHashSet<>();
        for (final Image image : images) {
            urls.add(image.getUrl());
        }
        for (final String text: htmlText) {
            HtmlCompat.fromHtml(StringUtils.defaultString(text), HtmlCompat.FROM_HTML_MODE_LEGACY, source -> {
                if (!urls.contains(source) && canBeOpenedExternally(source)) {
                    images.add(new Image.Builder()
                            .setUrl(source)
                            .setTitle(StringUtils.defaultString(geocode))
                            .build());
                    urls.add(source);
                }
                return null;
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
    public static class ContainerDrawable extends BitmapDrawable implements Consumer<Drawable> {
        private static final Object lock = new Object(); // Used to lock the queue to determine if a refresh needs to be scheduled
        private static final LinkedBlockingQueue<ImmutablePair<ContainerDrawable, Drawable>> REDRAW_QUEUE = new LinkedBlockingQueue<>();
        private static final Set<TextView> VIEWS = new HashSet<>();  // Modified only on the UI thread, from redrawQueuedDrawables
        private static final Runnable REDRAW_QUEUED_DRAWABLES = ContainerDrawable::redrawQueuedDrawables;

        private Drawable drawable;
        protected final WeakReference<TextView> viewRef;

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
        public final void accept(final Drawable newDrawable) {
            final boolean needsRedraw;
            synchronized (lock) {
                // Check for emptiness inside the call to match the behaviour in redrawQueuedDrawables().
                needsRedraw = REDRAW_QUEUE.isEmpty();
                REDRAW_QUEUE.add(ImmutablePair.of(this, newDrawable));
            }
            if (needsRedraw) {
                AndroidSchedulers.mainThread().scheduleDirect(REDRAW_QUEUED_DRAWABLES);
            }
        }

        /**
         * Update the container with the new drawable. Called on the UI thread.
         *
         * @param newDrawable the new drawable
         * @return the view to update or <tt>null</tt> if the view is not alive anymore
         */
        protected TextView updateDrawable(final Drawable newDrawable) {
            setBounds(0, 0, newDrawable.getIntrinsicWidth(), newDrawable.getIntrinsicHeight());
            drawable = newDrawable;
            return viewRef.get();
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
                    final TextView view = redrawable.left.updateDrawable(redrawable.right);
                    if (view != null) {
                        VIEWS.add(view);
                    }
                }
                for (final TextView view : VIEWS) {
                    // This forces the relayout of the text around the updated images.
                    view.setText(view.getText());
                }
                VIEWS.clear();
            }
        }

    }

    /**
     * Image that automatically scales to fit a line of text in the containing {@link TextView}.
     */
    public static final class LineHeightContainerDrawable extends ContainerDrawable {
        public LineHeightContainerDrawable(@NonNull final TextView view, final Observable<? extends Drawable> drawableObservable) {
            super(view, drawableObservable);
        }

        @Override
        protected TextView updateDrawable(final Drawable newDrawable) {
            final TextView view = super.updateDrawable(newDrawable);
            if (view != null) {
                setBounds(scaleImageToLineHeight(newDrawable, view));
            }
            return view;
        }
    }

    public static boolean canBeOpenedExternally(final String source) {
        return !containsPattern(source, NO_EXTERNAL);
    }

    @NonNull
    public static Rect scaleImageToLineHeight(final Drawable drawable, final TextView view) {
        final int lineHeight = (int) (view.getLineHeight() * 0.8);
        final int width = drawable.getIntrinsicWidth() * lineHeight / drawable.getIntrinsicHeight();
        return new Rect(0, 0, width, lineHeight);
    }

    @Nullable
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

    public static File copyImageToTemporaryFile(final Image img) {
        if (img == null || img.getUri() == null) {
            return null;
        }
        final File targetFile = FileUtils.getUniqueNamedFile(new File(LocalStorage.getInternalCgeoDirectory(), "offline_log_image.tmp"));
        OutputStream os = null;
        InputStream is = null;
        try {
            is = openImageStream(img.getUri());
            if (is == null) {
                return null;
            }
            os = new FileOutputStream(targetFile);
            IOUtils.copy(is, os);
        } catch (IOException ioe) {
            Log.w("Problem copying img '" + img + "' to '" + targetFile + "'", ioe);
            return null;
        } finally {
            IOUtils.closeQuietly(is, os);
        }
        return targetFile;
    }


    // --- SAF: the following methods must be migrated

    /** Create a new Uri for saving an image
     * */
    @Nullable
    public static Uri createNewImageUri(final boolean forShare) {

        //TODO: shall be replaced by SAF storage access later

        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        final File mediaStorageDir = LocalStorage.getLogPictureDirectory();
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !FileUtils.mkdirs(mediaStorageDir)) {
            Log.e("ImageUtils.getOutputImageFile: cannot create media storage directory");
            return null;
        }

        // Create a media file name
        final String timeStamp = new SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(new Date());
        final File newFile = new File(mediaStorageDir.getPath() + separator + (forShare ? "shared-" : "") + "IMG" + IMG_COUNTER.addAndGet(1) + "-" + timeStamp + ".jpg");

        if (forShare) {
            return FileProvider.getUriForFile(
                CgeoApplication.getInstance().getApplicationContext(),
                CgeoApplication.getInstance().getApplicationContext().getString(R.string.file_provider_authority),
                newFile);
        }

        return Uri.fromFile(newFile);
    }


    @Nullable
    private static InputStream openImageStream(final Uri imageUri) {
        //TODO: this method must be replaced with a call to SAF framework later
        try {
            return CgeoApplication.getInstance().getApplicationContext().getContentResolver().openInputStream(imageUri);
        } catch (IOException ioe) {
            Log.w("Could not open inputstream for file " + imageUri, ioe);
            return null;
        }

    }

    public static boolean deleteImage(final Uri uri) {
        if (uri == null) {
            return false;
        }
        if (uri.toString().startsWith("file")) {
            return new File(uri.getPath()).delete();
        }
        //TODO: this method must be replaced with a call to SAF framework later
        if (uri.toString().startsWith("content")) {
            return CgeoApplication.getInstance().getApplicationContext().getContentResolver().delete(uri, null, null) > 0;
        }

        return false;
    }

    public static String getImageLocationForUserDisplay(final Image image) {
        //TODO: this method must be adapted for SAF
        return image.getUrl();
    }

    /** Returns image name and size in bytes */
    public static ImmutablePair<String, Long> getImageFileInfos(final Image image) {

        if (image == null) {
            return new ImmutablePair<>("", 0l);
        }

        if (image.isLocalFile()) {
            return new ImmutablePair<>(image.getFile().getName(), image.getFile().length());
        }

        //TODO: following must be ADAPTED for SAF framework later
        if (image.getUri() != null && image.getUri().toString().startsWith("file")) {
            return new ImmutablePair<>(image.getUri().getLastPathSegment(), new File (image.getUri().getPath()).length());
        }

        return new ImmutablePair<>("", 0l);
    }


}
