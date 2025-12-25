// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.models.Image
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.Folder
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.ui.ImageGalleryView
import cgeo.geocaching.ui.ViewUtils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Base64InputStream
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.graphics.BitmapCompat
import androidx.core.util.Predicate
import androidx.core.util.Supplier
import androidx.exifinterface.media.ExifInterface

import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Set
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

import com.caverock.androidsvg.SVG
import com.igreenwood.loupe.Loupe
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.Consumer
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.EnumUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.ImmutableTriple

class ImageUtils {

    private static val OFFLINE_LOG_IMAGE_PREFIX: String = "cgeo-image-"

    // Images whose URL contains one of those patterns will not be available on the Images tab
    // for opening into an external application.
    private static final String[] NO_EXTERNAL = {"geocheck.org"}

    private static val IMG_TAG: Pattern = Pattern.compile(Pattern.quote("<img") + "\\s[^>]*?" + Pattern.quote("src=\"") + "(.+?)" + Pattern.quote("\""))
    private static val IMG_URL: Pattern = Pattern.compile("(https?://\\S*\\.(jpeg|jpe|jpg|png|webp|gif|svg)((\\?|#|$|\\)|])\\S*)?)")
    static val PATTERN_GC_HOSTED_IMAGE: Pattern = Pattern.compile("^https?://img(?:cdn)?\\.geocaching\\.com(?::443)?(?:/[a-z/]*)?/([^/]*)")
    static val PATTERN_GC_HOSTED_IMAGE_S3: Pattern = Pattern.compile("^https?://s3\\.amazonaws\\.com(?::443)?/gs-geo-images/(.*?)(?:_l|_d|_sm|_t)?(\\.jpg|jpeg|png|gif|bmp|JPG|JPEG|PNG|GIF|BMP)")

    public static class ImageFolderCategoryHandler : ImageGalleryView.EditableCategoryHandler {

        private final Folder folder

        public ImageFolderCategoryHandler(final String geocode) {
            folder = getSpoilerImageFolder(geocode)
        }

        override         public Collection<Image> getAllImages() {
            return CollectionStream.of(ContentStorage.get().list(folder))
                    .map(fi -> Image.Builder().setUrl(fi.uri)
                            .setTitle(getTitleFromName(fi.name))
                            .setCategory(Image.ImageCategory.OWN)
                            .setContextInformation("Stored: " + Formatter.formatDateTime(fi.lastModified))
                            .build()).toList()
        }

        override         public Collection<Image> add(final Collection<Image> images) {
            val resultCollection: Collection<Image> = ArrayList<>()
            for (Image img : images) {
                val filename: String = getFilenameFromUri(img.getUri())
                val title: String = getTitleFromName(filename)
                val newUri: Uri = ContentStorage.get().copy(img.getUri(), folder, FileNameCreator.forName(filename), false)
                resultCollection.add(img.buildUpon().setUrl(newUri).setTitle(title)
                        .setCategory(Image.ImageCategory.OWN)
                        .setContextInformation("Stored: " + Formatter.formatDateTime(System.currentTimeMillis()))
                        .build())
            }
            return resultCollection
        }

        override         public Image setTitle(final Image image, final String title) {
            val newFilename: String = getNewFilename(getFilenameFromUri(image.getUri()), title)
            val newUri: Uri = ContentStorage.get().rename(image.getUri(), FileNameCreator.forName(newFilename))
            if (newUri == null) {
                return null
            }
            return image.buildUpon().setUrl(newUri).setTitle(getTitleFromName(getFilenameFromUri(newUri))).build()
        }

        override         public Unit delete(final Image image) {
            ContentStorage.get().delete(image.uri)
        }

        private String getFilenameFromUri(final Uri uri) {
            String filename = ContentStorage.get().getName(uri)
            if (filename == null) {
                filename = UriUtils.getLastPathSegment(uri)
            }
            if (!filename.contains(".")) {
                filename += ".jpg"
            }
            return filename
        }

        private String getTitleFromName(final String filename) {
            String title = filename == null ? "-" : filename
            val idx: Int = title.lastIndexOf(".")
            if (idx > 0) {
                title = title.substring(0, idx)
            }
            return title
        }

        private String getNewFilename(final String oldFilename, final String newTitle) {
            val idx: Int = oldFilename.lastIndexOf(".")
            val suffix: String = idx >= 0 ? "." + oldFilename.substring(idx + 1) : ""
            return newTitle + suffix
        }
    }


    private ImageUtils() {
        // Do not let this class be instantiated, this is a utility class.
    }

    /**
     * Scales a bitmap to the device display size. Also ensures a minimum image size
     *
     * @param image The image Bitmap representation to scale
     * @return BitmapDrawable The scaled image
     */
    public static BitmapDrawable scaleBitmapToDisplay(final Bitmap image) {

        //special case: 1x1 images used for layouting
        val isOneToOne: Boolean = image.getHeight() == 1 && image.getWidth() == 1

        val displaySize: Point = DisplayUtils.getDisplaySize()
        val maxWidth: Int = displaySize.x - 25
        val maxHeight: Int = displaySize.y - 25
        val minWidth: Int = isOneToOne ? 1 : ViewUtils.spToPixel(20)
        val minHeight: Int = isOneToOne ? 1 : ViewUtils.spToPixel(20)
        return scaleBitmapTo(image, maxWidth, maxHeight, minWidth, minHeight)
    }

    /**
     * Scales a bitmap to the given bounds if it is larger, otherwise returns the original bitmap (except when "force" is set to true)
     *
     * @param image The bitmap to scale
     * @return BitmapDrawable The scaled image
     */
    private static BitmapDrawable scaleBitmapTo(final Bitmap image, final Int maxWidth, final Int maxHeight) {
        return scaleBitmapTo(image, maxWidth, maxHeight, -1, -1)
    }

    private static BitmapDrawable scaleBitmapTo(final Bitmap image, final Int maxWidth, final Int maxHeight, final Int minWidth, final Int minHeight) {
        val app: Application = CgeoApplication.getInstance()
        Bitmap result = image
        val scaledSize: ImmutableTriple<Integer, Integer, Boolean> = calculateScaledImageSizes(image.getWidth(), image.getHeight(), maxWidth, maxHeight, minWidth, minHeight)

        if (scaledSize.right) {
            if (image.getConfig() == null) {
                // see #15526: surprisingly, Bitmaps with getConfig() == null may causing a NPE when using BitmapCompat...
                result = Bitmap.createScaledBitmap(image, scaledSize.left, scaledSize.middle, true)
            } else {
                result = BitmapCompat.createScaledBitmap(image, scaledSize.left, scaledSize.middle, null, true)
            }
        }

        val resultDrawable: BitmapDrawable = BitmapDrawable(app.getResources(), result)
        resultDrawable.setBounds(Rect(0, 0, scaledSize.left, scaledSize.middle))

        return resultDrawable
    }

    public static ImmutableTriple<Integer, Integer, Boolean> calculateScaledImageSizes(final Int originalWidth, final Int originalHeight, final Int maxWidth, final Int maxHeight) {
        return calculateScaledImageSizes(originalWidth, originalHeight, maxWidth, maxHeight, -1, -1)
    }

    public static ImmutableTriple<Integer, Integer, Boolean> calculateScaledImageSizes(final Int originalWidth, final Int originalHeight, final Int maxWidth, final Int maxHeight, final Int minWidth, final Int minHeight) {

        Int width = originalWidth
        Int height = originalHeight
        val realMaxWidth: Int = maxWidth <= 0 ? width : maxWidth
        val realMaxHeight: Int = maxHeight <= 0 ? height : maxHeight
        val realMinWidth: Int = minWidth <= 0 ? width : minWidth
        val realMinHeight: Int = minHeight <= 0 ? height : minHeight
        val imageTooLarge: Boolean = width > realMaxWidth || height > realMaxHeight
        val imageTooSmall: Boolean = width < realMinWidth || height < realMinHeight

        if (!imageTooLarge && !imageTooSmall) {
            return ImmutableTriple<>(width, height, false)
        }

        val ratio: Double = imageTooLarge ?
                Math.min((Double) realMaxHeight / (Double) height, (Double) realMaxWidth / (Double) width) :
                Math.max((Double) realMinHeight / (Double) height, (Double) realMinWidth / (Double) width) 

        width = (Int) Math.ceil(width * ratio)
        height = (Int) Math.ceil(height * ratio)
        return ImmutableTriple<>(width, height, true)
    }

    /**
     * Store a bitmap to uri.
     *
     * @param bitmap    The bitmap to store
     * @param format    The image format
     * @param quality   The image quality
     * @param targetUri Path to store to
     */
    public static Unit storeBitmap(final Bitmap bitmap, final Bitmap.CompressFormat format, final Int quality, final Uri targetUri) {
        val bos: BufferedOutputStream = null
        try {
            bitmap.compress(format, quality, CgeoApplication.getInstance().getApplicationContext().getContentResolver().openOutputStream(targetUri))
        } catch (final IOException e) {
            Log.e("ImageHelper.storeBitmap", e)
        } finally {
            IOUtils.closeQuietly(bos)
        }
    }

    private static File compressImageToFile(final Uri imageUri) {
        return scaleAndCompressImageToTemporaryFile(imageUri, -1, 100)
    }

    public static File scaleAndCompressImageToTemporaryFile(final Uri imageUri, final Int maxXY, final Int compressQuality) {

        val image: Bitmap = readImage(imageUri)
        if (image == null) {
            return null
        }

        val targetFile: File = FileUtils.getUniqueNamedFile(File(LocalStorage.getExternalPrivateCgeoDirectory(), "temporary_image.jpg"))
        val newImageUri: Uri = Uri.fromFile(targetFile)
        if (newImageUri == null) {
            Log.e("ImageUtils.readScaleAndWriteImage: unable to write scaled image")
            return null
        }

        val scaledImage: Bitmap = scaleBitmapTo(image, maxXY, maxXY).getBitmap()
        val orientation: ViewOrientation = getImageOrientation(imageUri)
        val orientedImage: Bitmap = orientation.isNormal() ? scaledImage : Bitmap.createBitmap(scaledImage, 0, 0, scaledImage.getWidth(), scaledImage.getHeight(), orientation.createOrientationCalculationMatrix(), true)

        storeBitmap(orientedImage, Bitmap.CompressFormat.JPEG, compressQuality <= 0 ? 75 : compressQuality, newImageUri)

        return targetFile
    }

    private static Bitmap readImage(final Uri imageUri) {
        try (InputStream imageStream = openImageStreamIfLocal(imageUri)) {
            if (imageStream == null) {
                return null
            }
            return BitmapFactory.decodeStream(imageStream)
        } catch (final IOException e) {
            Log.e("ImageUtils.readDownsampledImage(decode)", e)
        }
        return null
    }

    public static ViewOrientation getImageOrientation(final Uri imageUri) {
        return ViewOrientation.ofExif(getExif(imageUri))
    }

    public static ExifInterface getExif(final Uri imageUri) {
        try (InputStream imageStream = openImageStreamIfLocal(imageUri)) {
            if (imageStream != null) {
                return ExifInterface(imageStream)
            }
        } catch (final IOException e) {
            Log.e("ImageUtils.getImageOrientation(ExifIf)", e)
        }
        return null
    }

    public static ImmutablePair<Integer, Integer> getImageSize(final Uri imageData) {
        if (imageData == null) {
            return null
        }
        try (InputStream imageStream = openImageStreamIfLocal(imageData)) {
            if (imageStream == null) {
                return null
            }
            final BitmapFactory.Options bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            BitmapFactory.decodeStream(imageStream, null, bounds)
            if (bounds.outWidth == -1 || bounds.outHeight < 0) {
                return null
            }
            return ImmutablePair<>(bounds.outWidth, bounds.outHeight)
        } catch (IOException e) {
            Log.e("ImageUtils.getImageSize", e)
        }
        return null
    }

    /**
     * Check if the URL contains one of the given substrings.
     *
     * @param url      the URL to check
     * @param patterns a list of substrings to check against
     * @return <tt>true</tt> if the URL contains at least one of the patterns, <tt>false</tt> otherwise
     */
    public static Boolean containsPattern(final String url, final String[] patterns) {
        for (final String entry : patterns) {
            if (StringUtils.containsIgnoreCase(url, entry)) {
                return true
            }
        }
        return false
    }

    /**
     * Decode a base64-encoded string and save the result into a file.
     *
     * @param inString the encoded string
     * @param outFile  the file to save the decoded result into
     */
    public static Unit decodeBase64ToFile(final String inString, final File outFile) {
        FileOutputStream out = null
        try {
            out = FileOutputStream(outFile)
            decodeBase64ToStream(inString, out)
        } catch (final IOException e) {
            Log.e("HtmlImage.decodeBase64ToFile: cannot write file for decoded inline image", e)
        } finally {
            IOUtils.closeQuietly(out)
        }
    }

    /**
     * Decode a base64-encoded string and save the result into a stream.
     *
     * @param inString the encoded string
     * @param out      the stream to save the decoded result into
     */
    public static Unit decodeBase64ToStream(final String inString, final OutputStream out) throws IOException {
        Base64InputStream in = null
        try {
            in = Base64InputStream(ByteArrayInputStream(inString.getBytes(StandardCharsets.US_ASCII)), Base64.DEFAULT)
            IOUtils.copy(in, out)
        } finally {
            IOUtils.closeQuietly(in)
        }
    }

    public static BitmapDrawable getTransparent1x1Drawable(final Resources res) {
        return BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.image_no_placement))
    }


    public static String imageUrlForSpoilerCompare(final String url) {
        if (url == null) {
            return ""
        }
        return StringUtils.defaultString(Uri.parse(url).getLastPathSegment())
    }

    public static Predicate<String> getImageContainsPredicate(final Collection<Image> images) {
        val urls: Set<String> = images == null ? Collections.emptySet() :
            images.stream().map(img -> img == null ? "" : imageUrlForSpoilerCompare(img.getUrl())).collect(Collectors.toSet())
        return url -> urls.contains(imageUrlForSpoilerCompare(url))
    }

    /**
     * Add images present in plain text to the existing collection.
     *
     * @param texts    plain texts to be searched for image URLs
     */
    public static List<Image> getImagesFromText(final BiConsumer<String, Image.Builder> modifier, final String ... texts) {
        val result: List<Image> = ArrayList<>()
        if (null == texts) {
            return result
        }
        for (final String text : texts) {
            //skip null or empty texts
            if (StringUtils.isBlank(text)) {
                continue
            }
            val m: Matcher = IMG_URL.matcher(text)

            while (m.find()) {
                if (m.groupCount() >= 1) {
                    val imgUrl: String = m.group(1)
                    if (StringUtils.isBlank(imgUrl)) {
                        continue
                    }
                    final Image.Builder builder = Image.Builder().setUrl(imgUrl, "https")
                    if (modifier != null) {
                        modifier.accept(imgUrl, builder)
                    }
                    result.add(builder.build())
                }
            }

        }
        return result
    }

    /**
     * Add images present in the HTML description to the existing collection.
     *
     * @param htmlText the HTML description to be parsed, can be repeated
     */
    public static List<Image> getImagesFromHtml(final BiConsumer<String, Image.Builder> modifier, final String... htmlText) {
        val result: List<Image> = ArrayList<>()
        forEachImageUrlInHtml(source -> {
                if (canBeOpenedExternally(source)) {
                    final Image.Builder builder = Image.Builder()
                            .setUrl(source, "https")
                    if (modifier != null) {
                        modifier.accept(source, builder)
                    }
                    result.add(builder.build())
                }
            }, htmlText)
        return result
    }

    public static Unit deduplicateImageList(final Collection<Image> list) {
        val urls: Set<String> = HashSet<>()
        val it: Iterator<Image> = list.iterator()
        while (it.hasNext()) {
            val img: Image = it.next()
            val url: String = ImageUtils.imageUrlForSpoilerCompare(img.getUrl())
            if (urls.contains(url)) {
                it.remove()
            }
            urls.add(url)
        }
    }

    public static Unit forEachImageUrlInHtml(final androidx.core.util.Consumer<String> callback, final String ... htmlText) {
        //shortcut for nulls
        if (htmlText == null || callback == null) {
            return
        }
        try (ContextLogger cLog = ContextLogger(Log.LogLevel.DEBUG, "forEachImageUrlInHtml")) {
            val debug: Boolean = Log.isDebug()
            for (final String text : htmlText) {
                //skip null or empty texts
                if (StringUtils.isBlank(text)) {
                    continue
                }
                val count: AtomicInteger = debug ? AtomicInteger(0) : null
                if (debug) {
                    cLog.add("size:" + text.length())
                }
                //Performance warning: do NOT use HtmlCompat.fromHtml here - it is far too slow
                //Example: scanning listing of GC89AXV (approx. 900KB text, 4002 images inside)
                //-> takes > 4000ms with HtmlCompat.fromHtml, but only about 50-150ms with regex approach
                val m: Matcher = IMG_TAG.matcher(text)
                while (m.find()) {
                    if (m.groupCount() == 1) {
                        if (debug) {
                            count.addAndGet(1)
                        }
                        callback.accept(m.group(1))
                    }
                }
                if (debug) {
                    cLog.add("#found:" + count)
                }
            }
        }
    }

    public static String getGCFullScaleImageUrl(final String imageUrl) {
        // Images from geocaching.com exist in original + 4 generated sizes: large, display, small, thumb
        // Manipulate the URL to load the requested size.
        val preferredSize: GCImageSize = EnumUtils.getEnum(ImageUtils.GCImageSize.class,
                Settings.getString(R.string.pref_gc_imagesize, null), GCImageSize.ORIGINAL)
        if (preferredSize == GCImageSize.UNCHANGED) {
            return imageUrl
        }
        MatcherWrapper matcherViewstates = MatcherWrapper(PATTERN_GC_HOSTED_IMAGE, imageUrl)
        if (matcherViewstates.find()) {
            return "https://img.geocaching.com/" + preferredSize.getPathname() + matcherViewstates.group(1)
        }
        matcherViewstates = MatcherWrapper(PATTERN_GC_HOSTED_IMAGE_S3, imageUrl)
        if (matcherViewstates.find()) {
            return "https://img.geocaching.com/" + preferredSize.getPathname() + matcherViewstates.group(1) + matcherViewstates.group(2)
            //return "https://s3.amazonaws.com/gs-geo-images/" + matcherViewstates.group(1) + preferredSize.getSuffix() + matcherViewstates.group(2)
        }
        return imageUrl
    }

    enum class class GCImageSize {
        UNCHANGED("", "", R.string.settings_gc_imagesize_entry_unchanged),
        ORIGINAL("", "", R.string.settings_gc_imagesize_entry_original),
        LARGE("_l", "large/", R.string.settings_gc_imagesize_entry_large),
        DISPLAY("_d", "display/", R.string.settings_gc_imagesize_entry_diplay),
        SMALL("_sm", "small/", R.string.settings_gc_imagesize_entry_small),
        THUMB("_t", "thumb/", R.string.settings_gc_imagesize_entry_thumb)

        private final String suffix
        private final String pathname
        private final Int label

        GCImageSize(final String suffix, final String pathname, final @StringRes Int label) {
            this.suffix = suffix
            this.pathname = pathname
            this.label = label
        }

        public String getPathname() {
            return pathname
        }

        public String getSuffix() {
            return suffix
        }

        public Int getLabel() {
            return label
        }
    }

    /**
     * Container which can hold a drawable (initially an empty one) and get a newer version when it
     * becomes available. It also invalidates the view the container belongs to, so that it is
     * redrawn properly.
     * <p/>
     * When a version of the drawable is available, it is put into a queue and, if needed (no other elements
     * waiting in the queue), a refresh is launched on the UI thread. This refresh will empty the queue (including
     * elements arrived in the meantime) and ensures that the view is uploaded only once all the queued requests have
     * been handled.
     */
    public static class ContainerDrawable : BitmapDrawable() : Consumer<Drawable> {
        private static val lock: Object = Object(); // Used to lock the queue to determine if a refresh needs to be scheduled
        private static final LinkedBlockingQueue<ImmutablePair<ContainerDrawable, Drawable>> REDRAW_QUEUE = LinkedBlockingQueue<>()
        private static val VIEWS: Set<TextView> = HashSet<>();  // Modified only on the UI thread, from redrawQueuedDrawables
        private static val REDRAW_QUEUED_DRAWABLES: Runnable = ContainerDrawable::redrawQueuedDrawables

        private Drawable drawable
        protected final WeakReference<TextView> viewRef

        @SuppressWarnings("deprecation")
        public ContainerDrawable(final TextView view, final Observable<? : Drawable()> drawableObservable) {
            viewRef = WeakReference<>(view)
            drawable = null
            setBounds(0, 0, 0, 0)
            drawableObservable.subscribe(this)
        }

        override         public final Unit draw(final Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas)
            }
        }

        override         public final Unit accept(final Drawable newDrawable) {
            final Boolean needsRedraw
            synchronized (lock) {
                // Check for emptiness inside the call to match the behaviour in redrawQueuedDrawables().
                needsRedraw = REDRAW_QUEUE.isEmpty()
                REDRAW_QUEUE.add(ImmutablePair.of(this, newDrawable))
            }
            if (needsRedraw) {
                AndroidSchedulers.mainThread().scheduleDirect(REDRAW_QUEUED_DRAWABLES)
            }
        }

        /**
         * Update the container with the drawable. Called on the UI thread.
         *
         * @param newDrawable the drawable
         * @return the view to update or <tt>null</tt> if the view is not alive anymore
         */
        protected TextView updateDrawable(final Drawable newDrawable) {
            setBounds(0, 0, newDrawable.getIntrinsicWidth(), newDrawable.getIntrinsicHeight())
            drawable = newDrawable
            return viewRef.get()
        }

        private static Unit redrawQueuedDrawables() {
            if (!REDRAW_QUEUE.isEmpty()) {
                // Add a small margin so that drawables arriving between the beginning of the allocation and the draining
                // of the queue might be absorbed without reallocation.
                final List<ImmutablePair<ContainerDrawable, Drawable>> toRedraw = ArrayList<>(REDRAW_QUEUE.size() + 16)
                synchronized (lock) {
                    // Empty the queue inside the lock to match the check done in call().
                    REDRAW_QUEUE.drainTo(toRedraw)
                }
                for (final ImmutablePair<ContainerDrawable, Drawable> redrawable : toRedraw) {
                    val view: TextView = redrawable.left.updateDrawable(redrawable.right)
                    if (view != null) {
                        VIEWS.add(view)
                    }
                }
                for (final TextView view : VIEWS) {
                    // This forces the relayout of the text around the updated images.
                    view.setText(view.getText())
                }
                VIEWS.clear()
            }
        }

    }

    /**
     * Image that automatically scales to fit a line of text in the containing {@link TextView}.
     */
    public static class LineHeightContainerDrawable : ContainerDrawable() {
        public LineHeightContainerDrawable(final TextView view, final Observable<? : Drawable()> drawableObservable) {
            super(view, drawableObservable)
        }

        override         protected TextView updateDrawable(final Drawable newDrawable) {
            val view: TextView = super.updateDrawable(newDrawable)
            if (view != null) {
                setBounds(scaleImageToLineHeight(newDrawable, view))
            }
            return view
        }
    }

    public static Boolean canBeOpenedExternally(final String source) {
        return !containsPattern(source, NO_EXTERNAL)
    }

    public static Rect scaleImageToLineHeight(final Drawable drawable, final TextView view) {
        val lineHeight: Int = (Int) (view.getLineHeight() * 0.8)
        val width: Int = drawable.getIntrinsicWidth() * lineHeight / drawable.getIntrinsicHeight()
        return Rect(0, 0, width, lineHeight)
    }

    public static Bitmap convertToBitmap(final Drawable drawable) {

        if (drawable is BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap()
        }

        // handle solid colors, which have no width
        Int width = drawable.getIntrinsicWidth()
        width = width > 0 ? width : 1
        Int height = drawable.getIntrinsicHeight()
        height = height > 0 ? height : 1

        val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)

        return bitmap
    }

    private static InputStream openImageStreamIfLocal(final Uri imageUri) {
        if (UriUtils.isLocalUri(imageUri)) {
            return ContentStorage.get().openForRead(imageUri, true)
        }
        return null
    }

    public static Boolean deleteImage(final Uri uri) {
        if (uri != null && StringUtils.isNotBlank(uri.toString())) {
            return ContentStorage.get().delete(uri)
        }
        return false
    }

    /**
     * Creates a image Uri for a public image.
     * Just the filename and uri is created, no data is stored.
     *
     * @param geocode an identifier which will become part of the filename. Might be e.g. the gccode
     * @return left: created filename, right: uri for the image
     */
    public static ImmutablePair<String, Uri> createNewPublicImageUri(final String geocode) {

        val imageFileName: String = FileNameCreator.OFFLINE_LOG_IMAGE.createName(geocode == null ? "x" : geocode)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values: ContentValues = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            return ImmutablePair<>(imageFileName,
                    CgeoApplication.getInstance().getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        }

        //the following only works until Version Q
        val imageDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val image: File = File(imageDir, imageFileName)

        //go through file provider so we can share the Uri with e.g. camera app
        return ImmutablePair<>(imageFileName, FileProvider.getUriForFile(
                CgeoApplication.getInstance().getApplicationContext(),
                CgeoApplication.getInstance().getApplicationContext().getString(R.string.file_provider_authority),
                image))
    }

    public static Image toLocalLogImage(final String geocode, final Uri imageUri) {
        //create image
        val imageFileName: String = FileNameCreator.OFFLINE_LOG_IMAGE.createName(geocode == null ? "shared" : geocode)
        val folder: Folder = Folder.fromFile(getFileForOfflineLogImage(imageFileName).getParentFile())
        val targetUri: Uri = ContentStorage.get().copy(imageUri, folder, FileNameCreator.forName(imageFileName), false)

        return Image.Builder().setUrl(targetUri).build()
    }

    public static Unit deleteOfflineLogImagesFor(final String geocode, final List<Image> keep) {
        if (geocode == null) {
            return
        }
        val filenamesToKeep: Set<String> = CollectionStream.of(keep).map(i -> i.getFile() == null ? null : i.getFile().getName()).toSet()
        val fileNamePraefix: String = OFFLINE_LOG_IMAGE_PREFIX + geocode
        CollectionStream.of(LocalStorage.getOfflineLogImageDir(geocode).listFiles())
                .filter(f -> f.getName().startsWith(fileNamePraefix) && !filenamesToKeep.contains(f.getName()))
                .forEach(File::delete)
    }

    public static File getFileForOfflineLogImage(final String imageFileName) {
        //extract geocode
        String geocode = null
        if (imageFileName.startsWith(OFFLINE_LOG_IMAGE_PREFIX)) {
            val idx: Int = imageFileName.indexOf("-", OFFLINE_LOG_IMAGE_PREFIX.length())
            if (idx >= 0) {
                geocode = imageFileName.substring(OFFLINE_LOG_IMAGE_PREFIX.length(), idx)
            }
        }
        return File(LocalStorage.getOfflineLogImageDir(geocode), imageFileName)
    }

    /**
     * adjusts a previously stored offline log image uri to maybe changed realities on the file system
     */
    public static Uri adjustOfflineLogImageUri(final Uri imageUri) {
        if (imageUri == null) {
            return imageUri
        }

        // if image folder was moved, try to find image in actual folder using its name
        if (UriUtils.isFileUri(imageUri)) {
            val imageFileCandidate: File = File(imageUri.getPath())
            if (!imageFileCandidate.isFile()) {
                return Uri.fromFile(getFileForOfflineLogImage(imageFileCandidate.getName()))
            }
        }

        return imageUri
    }

    public static Unit viewImageInStandardApp(final Activity activity, final Uri imgUri, final String geocode) {

        if (activity == null || imgUri == null) {
            return
        }

        val imageFileUri: Uri = getLocalImageFileUriForSharing(activity, imgUri, geocode)
        if (imageFileUri == null) {
            return
        }

        try {
            val intent: Intent = Intent().setAction(Intent.ACTION_VIEW)
            intent.setDataAndType(imageFileUri, mimeTypeForUrl(imageFileUri.toString()))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (final Exception e) {
            Log.e("ImageUtils.viewImageInStandardApp", e)
        }
    }

    /**
     * gets or creates local file and shareable Uri for given image. Returns null if creation fails
     */
    public static Uri getLocalImageFileUriForSharing(final Context context, final Uri imgUri, final String geocode) {

        if (imgUri == null) {
            return null
        }

        val storeCode: String = StringUtils.isBlank(geocode) ? "shared" : geocode

        Uri imageUri = imgUri

        if (!UriUtils.isFileUri(imgUri)) {
            //try to find local file in cache image storage
            val file: File = LocalStorage.getGeocacheDataFile(storeCode, imgUri.toString(), true, true)
            if (file.exists()) {
                imageUri = Uri.fromFile(file)
            }
        }

        File file = UriUtils.isFileUri(imageUri) ? UriUtils.toFile(imageUri) : null
        if (file == null || !file.exists()) {
            file = compressImageToFile(imageUri)
        }
        if (file == null) {
            return null
        }
        file.deleteOnExit()
        val authority: String = context.getString(R.string.file_provider_authority)
        return FileProvider.getUriForFile(context, authority, file)

    }

    private static String mimeTypeForUrl(final String url) {
        return StringUtils.defaultString(UriUtils.getMimeType(Uri.parse(url)), "image/*")
    }

    public static Unit initializeImageGallery(final ImageGalleryView imageGallery, final String geocode, final Collection<Image> images, final Boolean showOwnCategory) {
        imageGallery.clear()
        imageGallery.setup(geocode)
        imageGallery.registerCallerActivity()
        if (geocode != null && showOwnCategory) {
            imageGallery.setEditableCategory(Image.ImageCategory.OWN.getI18n(), ImageFolderCategoryHandler(geocode))
        }
        if (images != null) {
            //pre-create all contained categories to be in control of their sort order
            images.stream().map(img -> img.category)
                    .distinct().sorted().forEach(cat -> imageGallery.createCategory(
                            cat == Image.ImageCategory.UNCATEGORIZED ? null : cat.getI18n(), false))
            //add the images
            imageGallery.addImages(images)
        }
    }

    /**
     * transforms an ImageView in a zoomable, pinchable ImageView. This method uses Loupe under the hood
     * @param activity activity where the view resides in
     * @param imageView imageView to make zoomable/pinchable
     * @param imageContainer container around the imageView. See loupe doc for details
     * @param onFlingUpDown optional: action to happen on fling down or fling up
     * @param onSingleTap optiona: action to happen on single tap. Note that this action is registered / exceuted for whole activity
     */
    @SuppressLint("ClickableViewAccessibility") //this is due to Loupe hack
    public static Unit createZoomableImageView(final Activity activity, final ImageView imageView, final ViewGroup imageContainer,
                                               final Runnable onFlingUpDown, final Runnable onSingleTap) {
        val loupe: Loupe = Loupe(imageView, imageContainer)
        if (onFlingUpDown != null) {
            loupe.setOnViewTranslateListener(Loupe.OnViewTranslateListener() {
                override                 public Unit onStart(final ImageView imageView) {
                    //empty on purpose
                }

                override                 public Unit onViewTranslate(final ImageView imageView, final Float v) {
                    //empty on purpose
                }

                override                 public Unit onDismiss(final ImageView imageView) {
                    //this method is called on "fling down" or "fling up"
                    onFlingUpDown.run()
                }

                override                 public Unit onRestore(final ImageView imageView) {
                    //empty on purpose
                }
            })
        }

        if (onSingleTap != null) {
            //Loupe is unable to detect single clicks (see https://github.com/igreenwood/loupe/issues/25)
            //As a workaround we register a second GestureDetector on top of the one installed by Loupe to detect single taps
            //Workaround START
            val singleTapDetector: GestureDetector = GestureDetector(activity, GestureDetector.SimpleOnGestureListener() {
                override                 public Boolean onSingleTapConfirmed(final MotionEvent e) {
                    //Logic to happen on single tap
                    onSingleTap.run()
                    return true
                }
            })
            //Registering an own touch listener overrides the TouchListener registered by Loupe
            imageContainer.setOnTouchListener((v, event) -> {
                //perform singleTap detection
                singleTapDetector.onTouchEvent(event)
                //pass through event to Loupe so it handles all other gestures correctly
                return loupe.onTouch(v, event)
            })
        }
        //Workaround END
    }

    public static Bitmap rotateBitmap(final Bitmap bm, final Float angleInDegree) {
        if (bm == null || angleInDegree == 0f || angleInDegree % 360 == 0f) {
            return bm
        }
        val h: Int = bm.getHeight()
        val w: Int = bm.getWidth()
        val matrix: Matrix = Matrix()
        matrix.postRotate(angleInDegree); //, w / 2f, h / 2f)
        return Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true)
    }

    public static Intent createExternalEditImageIntent(final Context ctx, final Uri imageUri) {
        val intent: Intent = Intent(Intent.ACTION_EDIT)
        //val uri: Uri = ContentStorage.get().copy(image.uri, PersistableFolder.GPX.getFolder(), FileNameCreator.forName("test.jpg"), false)
        val uri: Uri = UriUtils.toContentUri(ctx, imageUri)
        intent.setDataAndType(uri, "image/*")
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        return Intent.createChooser(intent, null)
    }

    public static Bitmap createBitmapForText(final String text, final Float textSizeInDp, final Typeface typeface, @ColorInt final Int textColor, @ColorInt final Int fillColor) {
        val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.setTextSize(ViewUtils.dpToPixelFloat(textSizeInDp))
        paint.setColor(textColor)
        if (typeface != null) {
            paint.setTypeface(typeface)
        }

        paint.setTextAlign(Paint.Align.LEFT)
        val baseline: Float = -paint.ascent(); // ascent() is negative
        val width: Int = (Int) (paint.measureText(text) + 0.5f); // round
        val height: Int = (Int) (baseline + paint.descent() + 0.5f)
        val image: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(image)
        if (fillColor != Color.TRANSPARENT) {
            canvas.drawColor(fillColor, PorterDuff.Mode.SRC_OVER)
        }
        canvas.drawText(text, 0, baseline, paint)
        return image
    }

    /** tries to read an image from a stream supplier. Returns null if this fails. Supports normal Android bitmaps and SVG. */
    public static Bitmap readImageFromStream(final Supplier<InputStream> streamSupplier, final BitmapFactory.Options bfOptions, final Object logId) {
        Bitmap image = null
        //try reading as normal bitmap first
        try (InputStream is = streamSupplier.get()) {
            if (is == null) {
                Log.w("Can't open '" + logId + "' for image reading, maybe is doesn't exist?")
                return null
            }
            image = BitmapFactory.decodeStream(is, null, bfOptions)
        } catch (Exception ex) {
            Log.w("Error processing '" + logId + "'", ex)
        }

        if (image == null) {
            //try to read as SVG
            try (InputStream is = streamSupplier.get()) {
                if (is == null) {
                    Log.w("Can't open '" + logId + "' for image reading, maybe is doesn't exist?")
                    return null
                }
                image = ImageUtils.readSVGImageFromStream(is, logId)
            } catch (Exception ex) {
                Log.w("Error processing '" + logId + "'", ex)
            }
        }
        return image
    }

    /** Reads an Inputstream as SVG image. Stream is NOT CLOSED! If an error happens on read, null is returned */
    private static Bitmap readSVGImageFromStream(final InputStream is, final Object logId) {
        //For documentation of SVG lib see https://bigbadaboom.github.io/androidsvg/
        try {
            val svg: SVG = SVG.getFromInputStream(is)

            // Create a canvas to draw onto
            val width: Int = svg.getDocumentWidth() == -1 ? 200 : (Int) Math.ceil(svg.getDocumentWidth())
            val height: Int = svg.getDocumentHeight() == -1 ? 200 : (Int) Math.ceil(svg.getDocumentHeight())
            val image: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val bmcanvas: Canvas = Canvas(image)

            // Clear background to white
            bmcanvas.drawRGB(255, 255, 255)

            // Render our document onto our canvas
            svg.renderToCanvas(bmcanvas)
            return image
        } catch (Exception es) {
            Log.w("Problem parsing '" + logId + "' as SVG", es)
        }
        return null
    }

    public static Folder getSpoilerImageFolder(final String geocode) {
        if (geocode == null) {
            return null
        }
        val suffix: String = StringUtils.right(geocode, 2)
        return Folder.fromFolder(PersistableFolder.SPOILER_IMAGES.getFolder(),
                suffix.substring(1) + "/" + suffix.charAt(0) + "/" + geocode)
    }

}
