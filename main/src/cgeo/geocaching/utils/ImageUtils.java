package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.ImageGalleryView;

import android.app.Activity;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Base64InputStream;
import android.webkit.MimeTypeMap;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

public final class ImageUtils {

    private static final String OFFLINE_LOG_IMAGE_PRAEFIX = "cgeo-image-";

    private static final int[] ORIENTATIONS = {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_ROTATE_270
    };

    private static final int[] ROTATION = {90, 180, 270};
    private static final int MAX_DISPLAY_IMAGE_XY = 800;

    // Images whose URL contains one of those patterns will not be available on the Images tab
    // for opening into an external application.
    private static final String[] NO_EXTERNAL = {"geocheck.org"};

    public static class ImageFolderCategoryHandler implements ImageGalleryView.EditableCategoryHandler {

        private final Folder folder;

        public ImageFolderCategoryHandler(final String geocode) {
            final String suffix = StringUtils.right(geocode, 2);
            folder = Folder.fromFolder(PersistableFolder.SPOILER_IMAGES.getFolder(),
                    suffix.substring(1) + "/" + suffix.charAt(0) + "/" + geocode);
        }

        @Override
        public Collection<Image> getAllImages() {
            return CollectionStream.of(ContentStorage.get().list(folder))
                    .map(fi -> new Image.Builder().setUrl(fi.uri)
                            .setTitle(getTitleFromName(fi.name))
                            .setCategory(Image.ImageCategory.OWN)
                            .setContextInformation("Stored: " + Formatter.formatDateTime(fi.lastModified))
                            .build()).toList();
        }

        @Override
        public Collection<Image> add(final Collection<Image> images) {
            final Collection<Image> resultCollection = new ArrayList<>();
            for (Image img : images) {
                final String filename = getFilenameFromUri(img.getUri());
                final String title = getTitleFromName(filename);
                final Uri newUri = ContentStorage.get().copy(img.getUri(), folder, FileNameCreator.forName(filename), false);
                resultCollection.add(img.buildUpon().setUrl(newUri).setTitle(title)
                        .setCategory(Image.ImageCategory.OWN)
                        .setContextInformation("Stored: " + Formatter.formatDateTime(System.currentTimeMillis()))
                        .build());
            }
            return resultCollection;
        }

        @Override
        public void delete(final Image image) {
            ContentStorage.get().delete(image.uri);
        }

        private String getFilenameFromUri(final Uri uri) {
            String filename = ContentStorage.get().getName(uri);
            if (filename == null) {
                filename = UriUtils.getLastPathSegment(uri);
            }
            if (!filename.contains(".")) {
                filename += ".jpg";
            }
            return filename;
        }

        private String getTitleFromName(final String filename) {
            String title = filename == null ? "-" : filename;
            final int idx = title.lastIndexOf(".");
            if (idx > 0) {
                title = title.substring(0, idx);
            }
            return title;

        }
    }


    private ImageUtils() {
        // Do not let this class be instantiated, this is a utility class.
    }

    /**
     * Scales a bitmap to the device display size.
     *
     * @param image The image Bitmap representation to scale
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
     * @param imageData The image data to read and scale
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
     * @param image The bitmap to scale
     * @return BitmapDrawable The scaled image
     */
    @NonNull
    private static BitmapDrawable scaleBitmapTo(@NonNull final Bitmap image, final int maxWidth, final int maxHeight) {
        final Application app = CgeoApplication.getInstance();
        Bitmap result = image;
        final ImmutableTriple<Integer, Integer, Boolean> scaledSize = calculateScaledImageSizes(image.getWidth(), image.getHeight(), maxWidth, maxHeight);

        if (scaledSize.right) {
            result = Bitmap.createScaledBitmap(image, scaledSize.left, scaledSize.middle, true);
        }

        final BitmapDrawable resultDrawable = new BitmapDrawable(app.getResources(), result);
        resultDrawable.setBounds(new Rect(0, 0, scaledSize.left, scaledSize.middle));

        return resultDrawable;
    }

    public static ImmutableTriple<Integer, Integer, Boolean> calculateScaledImageSizes(final int originalWidth, final int originalHeight, final int maxWidth, final int maxHeight) {
        int width = originalWidth;
        int height = originalHeight;
        final int realMaxWidth = maxWidth <= 0 ? width : maxWidth;
        final int realMaxHeight = maxHeight <= 0 ? height : maxHeight;
        final boolean imageTooLarge = width > realMaxWidth || height > realMaxHeight;

        if (!imageTooLarge) {
            return new ImmutableTriple<>(width, height, false);
        }

        final double ratio = Math.min((double) realMaxHeight / (double) height, (double) realMaxWidth / (double) width);
        width = (int) Math.ceil(width * ratio);
        height = (int) Math.ceil(height * ratio);
        return new ImmutableTriple<>(width, height, true);
    }

    /**
     * Store a bitmap to uri.
     *
     * @param bitmap    The bitmap to store
     * @param format    The image format
     * @param quality   The image quality
     * @param targetUri Path to store to
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

    public static File compressImageToFile(@NonNull final Uri imageUri) {
        return scaleAndCompressImageToTemporaryFile(imageUri, -1, 100);
    }

    public static File scaleAndCompressImageToTemporaryFile(@NonNull final Uri imageUri, final int maxXY, final int compressQuality) {

        final Bitmap image = readDownsampledImage(imageUri, maxXY, maxXY);
        if (image == null) {
            return null;
        }

        final File targetFile = FileUtils.getUniqueNamedFile(new File(LocalStorage.getExternalPrivateCgeoDirectory(), "temporary_image.jpg"));
        final Uri newImageUri = Uri.fromFile(targetFile);
        if (newImageUri == null) {
            Log.e("ImageUtils.readScaleAndWriteImage: unable to write scaled image");
            return null;
        }

        final BitmapDrawable scaledImage = scaleBitmapTo(image, maxXY, maxXY);
        storeBitmap(scaledImage.getBitmap(), Bitmap.CompressFormat.JPEG, compressQuality <= 0 ? 75 : compressQuality, newImageUri);

        return targetFile;
    }

    /**
     * Reads and scales an image with downsampling in one step to prevent memory consumption.
     *
     * @param imageUri image to read
     * @param maxX     The desired width. If <= 0 then actual bitmap width is used
     * @param maxY     The desired height. If <= 0 then actual bitmap height is used
     * @return Bitmap the image or null if image can't be read
     */
    @Nullable
    private static Bitmap readDownsampledImage(@NonNull final Uri imageUri, final int maxX, final int maxY) {

        final BitmapFactory.Options sizeOnlyOptions = getBitmapSizeOptions(openImageStream(imageUri));
        if (sizeOnlyOptions == null) {
            return null;
        }
        final int myMaxXY = Math.max(sizeOnlyOptions.outHeight, sizeOnlyOptions.outWidth);
        final int maxXY = Math.max(maxX <= 0 ? sizeOnlyOptions.outWidth : maxX, maxY <= 0 ? sizeOnlyOptions.outHeight : maxY);
        final int sampleSize = maxXY <= 0 ? 1 : myMaxXY / maxXY;
        final BitmapFactory.Options sampleOptions = new BitmapFactory.Options();
        if (sampleSize > 1) {
            sampleOptions.inSampleSize = sampleSize;
        }

        return readDownsampledImageInternal(imageUri, sampleOptions);
    }

    private static Bitmap readDownsampledImageInternal(final Uri imageUri, final BitmapFactory.Options sampleOptions) {
        final int orientation = getImageOrientation(imageUri);

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

    /**
     * stream will be consumed and closed by method
     */
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
     * @param url      the URL to check
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
     * @param outFile  the file to save the decoded result into
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
     * @param inString the encoded string
     * @param out      the stream to save the decoded result into
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
     *
     * @param images   a collection of images
     * @param geocode  the common title for images in the description
     * @param htmlText the HTML description to be parsed, can be repeated
     */
    public static void addImagesFromHtml(final Collection<Image> images, final String geocode, final String... htmlText) {
        final Set<String> urls = new LinkedHashSet<>();
        for (final Image image : images) {
            urls.add(image.getUrl());
        }
        forEachImageUrlInHtml(source -> {
                if (!urls.contains(source) && canBeOpenedExternally(source)) {
                    images.add(new Image.Builder()
                            .setUrl(source)
                            .setTitle(StringUtils.defaultString(geocode))
                            .setCategory(Image.ImageCategory.LISTING)
                            .build());
                    urls.add(source);
                }
            }, htmlText);
    }

    public static void forEachImageUrlInHtml(final androidx.core.util.Consumer<String> callback, final String ... htmlText) {
        for (final String text : htmlText) {
            HtmlCompat.fromHtml(StringUtils.defaultString(text), HtmlCompat.FROM_HTML_MODE_LEGACY, source -> {
                callback.accept(source);
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

    @Nullable
    private static InputStream openImageStream(final Uri imageUri) {
        return ContentStorage.get().openForRead(imageUri);
    }

    public static boolean deleteImage(final Uri uri) {
        if (uri != null && StringUtils.isNotBlank(uri.toString())) {
            return ContentStorage.get().delete(uri);
        }
        return false;
    }

    /**
     * Returns image name and size in bytes
     */
    public static ContentStorage.FileInformation getImageFileInfos(final Image image) {
        return ContentStorage.get().getFileInfo(image.getUri());
    }

    /**
     * Creates a new image Uri for a public image.
     * Just the filename and uri is created, no data is stored.
     *
     * @param geocode an identifier which will become part of the filename. Might be e.g. the gccode
     * @return left: created filename, right: uri for the image
     */
    public static ImmutablePair<String, Uri> createNewPublicImageUri(final String geocode) {

        final String imageFileName = FileNameCreator.OFFLINE_LOG_IMAGE.createName(geocode == null ? "x" : geocode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            return new ImmutablePair<>(imageFileName,
                    CgeoApplication.getInstance().getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values));
        }

        //the following only works until Version Q
        final File imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        final File image = new File(imageDir, imageFileName);

        //go through file provider so we can share the Uri with e.g. camera app
        return new ImmutablePair<>(imageFileName, FileProvider.getUriForFile(
                CgeoApplication.getInstance().getApplicationContext(),
                CgeoApplication.getInstance().getApplicationContext().getString(R.string.file_provider_authority),
                image));
    }

    /**
     * Create a new image Uri for an offline log image
     */
    public static ImmutablePair<String, Uri> createNewOfflineLogImageUri(final String geocode) {
        final String imageFileName = FileNameCreator.OFFLINE_LOG_IMAGE.createName(geocode == null ? "shared" : geocode);
        return new ImmutablePair<>(imageFileName, Uri.fromFile(getFileForOfflineLogImage(imageFileName)));
    }

    public static Image toLocalLogImage(final String geocode, final Uri imageUri) {
        //create new image
        final String imageFileName = FileNameCreator.OFFLINE_LOG_IMAGE.createName(geocode == null ? "shared" : geocode);
        final Folder folder = Folder.fromFile(getFileForOfflineLogImage(imageFileName).getParentFile());
        final Uri targetUri = ContentStorage.get().copy(imageUri, folder, FileNameCreator.forName(imageFileName), false);

        return new Image.Builder().setUrl(targetUri).build();
    }

    public static void deleteOfflineLogImagesFor(final String geocode, final List<Image> keep) {
        if (geocode == null) {
            return;
        }
        final Set<String> filenamesToKeep = CollectionStream.of(keep).map(i -> i.getFile() == null ? null : i.getFile().getName()).toSet();
        final String fileNamePraefix = OFFLINE_LOG_IMAGE_PRAEFIX + geocode;
        CollectionStream.of(LocalStorage.getOfflineLogImageDir(geocode).listFiles())
                .filter(f -> f.getName().startsWith(fileNamePraefix) && !filenamesToKeep.contains(f.getName()))
                .forEach(File::delete);
    }

    public static boolean deleteOfflineLogImageFile(final Image delete) {
        final File imageFile = getFileForOfflineLogImage(delete.getFileName());
        return imageFile.isFile() && imageFile.delete();
    }

    public static File getFileForOfflineLogImage(final String imageFileName) {
        //extract geocode
        String geocode = null;
        if (imageFileName.startsWith(OFFLINE_LOG_IMAGE_PRAEFIX)) {
            final int idx = imageFileName.indexOf("-", OFFLINE_LOG_IMAGE_PRAEFIX.length());
            if (idx >= 0) {
                geocode = imageFileName.substring(OFFLINE_LOG_IMAGE_PRAEFIX.length(), idx);
            }
        }
        return new File(LocalStorage.getOfflineLogImageDir(geocode), imageFileName);
    }

    /**
     * adjusts a previously stored offline log image uri to maybe changed realities on the file system
     */
    public static Uri adjustOfflineLogImageUri(final Uri imageUri) {
        if (imageUri == null) {
            return imageUri;
        }

        // if image folder was moved, try to find image in actual folder using its name
        if (UriUtils.isFileUri(imageUri)) {
            final File imageFileCandidate = new File(imageUri.getPath());
            if (!imageFileCandidate.isFile()) {
                return Uri.fromFile(getFileForOfflineLogImage(imageFileCandidate.getName()));
            }
        }

        return imageUri;
    }

    public static void viewImageInStandardApp(final Activity activity, final Uri imgUri, final String geocode) {

        if (activity == null || imgUri == null) {
            return;
        }

        final Uri imageFileUri = getLocalImageFileUriForSharing(activity, imgUri, geocode);

        try {
            final Intent intent = new Intent().setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(imageFileUri, mimeTypeForUrl(imageFileUri.toString()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (final Exception e) {
            Log.e("ImageUtils.viewImageInStandardApp", e);
        }
    }

    /**
     * gets or creates local file and shareable Uri for given image
     */
    public static Uri getLocalImageFileUriForSharing(final Context context, final Uri imgUri, final String geocode) {

        if (imgUri == null) {
            return null;
        }

        final String storeCode = StringUtils.isBlank(geocode) ? "shared" : geocode;

        Uri imageUri = imgUri;

        if (!UriUtils.isFileUri(imgUri)) {
            //try to find local file in cache image storage
            final File file = LocalStorage.getGeocacheDataFile(storeCode, imgUri.toString(), true, true);
            if (file.exists()) {
                imageUri = Uri.fromFile(file);
            }
        }

        File file = UriUtils.isFileUri(imageUri) ? UriUtils.toFile(imageUri) : null;
        if (file == null || !file.exists()) {
            file = compressImageToFile(imageUri);
            file.deleteOnExit();
        }
        final String authority = context.getString(R.string.file_provider_authority);
        return FileProvider.getUriForFile(context, authority, file);

    }

    private static String mimeTypeForUrl(final String url) {
        return StringUtils.defaultString(MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url)), "image/*");
    }

    public static void initializeImageGallery(final ImageGalleryView imageGallery, final String geocode, final Collection<Image> images) {
        imageGallery.clear();
        imageGallery.setup(geocode);
        if (geocode != null) {
            imageGallery.setEditableCategory(Image.ImageCategory.OWN.getI18n(), new ImageFolderCategoryHandler(geocode));
        }
        if (images != null) {
            imageGallery.addImages(images);
        }
    }

}
