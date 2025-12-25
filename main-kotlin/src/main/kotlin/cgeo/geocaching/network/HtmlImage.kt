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

package cgeo.geocaching.network

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.DisplayUtils
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.ImageUtils.ContainerDrawable
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MetadataUtils
import cgeo.geocaching.utils.RxUtils.ObservableCache
import cgeo.geocaching.utils.UriUtils
import cgeo.geocaching.utils.html.HtmlUtils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Html
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.File
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.HashMap
import java.util.Map
import java.util.concurrent.TimeUnit

import com.drew.metadata.Metadata
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.disposables.CancellableDisposable
import io.reactivex.rxjava3.processors.PublishProcessor
import okhttp3.Response
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutableTriple

/**
 * All-purpose image getter that can also be used as a ImageGetter interface when displaying caches.
 */

class HtmlImage : Html.ImageGetter {

    //for testing purposes: setting this value will delay every image get by the given amount of milliseconds
    private static val TEST_DELAY_TIME_MS: Long = 0

    private static final String[] BLOCKED = {
            "gccounter.de",
            "gccounter.com",
            "cachercounter/?",
            "gccounter/imgcount.php",
            "flagcounter.com",
            "compteur-blog.net",
            "counter.digits.com",
            "andyhoppe",
            "besucherzaehler-homepage.de",
            "hitwebcounter.com",
            "kostenloser-counter.eu",
            "trendcounter.com",
            "hit-counter-download.com",
            "gcwetterau.de/counter"
    }
    public static val SHARED: String = "shared"

    public static val IMAGE_ERROR_DATA: ImageData = ImageData(null, null, null)

    private final String geocode
    /**
     * on error: return large error image, if {@code true}, otherwise empty 1x1 image
     */
    private final Boolean returnErrorImage
    private final Boolean onlySave
    private final Boolean userInitiatedRefresh
    private final Int maxWidth
    private final Int maxHeight
    private final Resources resources
    final WeakReference<TextView> viewRef
    private var loadMetadata: Boolean = false

    private val cache: Map<String, BitmapDrawable> = HashMap<>()

    private val observableCache: ObservableCache<String, ImageData> = ObservableCache<>(this::fetchDrawableUncached)

    // Background loading
    // .cache() is not yet available on Completable instances as of RxJava 2.0.0, so we have to go back
    // to the observable world to achieve the caching.
    private val loading: PublishProcessor<Completable> = PublishProcessor.create()
    private val waitForEnd: Completable = Completable.merge(loading).cache()
    private val disposable: CompositeDisposable = CompositeDisposable(waitForEnd.subscribe())

    public static class ImageData {
        public final BitmapDrawable bitmapDrawable
        public final Metadata metadata
        public final Uri localUri

        private ImageData(final BitmapDrawable bitmapDrawable, final Metadata metadata, final Uri localUri) {
            this.bitmapDrawable = bitmapDrawable
            this.metadata = metadata
            this.localUri = localUri
        }
    }

    private static class InternalImageData {
        public final Bitmap bitmap
        public BitmapDrawable bitmapDrawable
        public final Metadata metadata
        public final Uri localUri
        public final Boolean isFresh

        InternalImageData(final Bitmap bitmap, final BitmapDrawable bitmapDrawable, final Metadata metadata, final Uri localUri, final Boolean isFresh) {
            this.localUri = localUri
            this.bitmap = bitmap
            this.isFresh = isFresh
            this.metadata = metadata
            this.bitmapDrawable = bitmapDrawable
        }
    }


    /**
     * Create a HtmlImage object with different behaviors depending on <tt>onlySave</tt> and <tt>view</tt> values.
     * There are the three possible use cases:
     * <ul>
     * <li>If onlySave is true, {@link #getDrawable(String)} will return <tt>null</tt> immediately and will queue the
     * image retrieval and saving in the loading subject. Downloads will start in parallel when the blocking
     * {@link #waitForEndCompletable(DisposableHandler)} method is called, and they can be
     * cancelled through the given handler.</li>
     * <li>If <tt>onlySave</tt> is <tt>false</tt> and the instance is called through {@link #fetchDrawable(String)},
     * then an observable for the given URL will be returned. This observable will emit the local copy of the image if
     * it is present regardless of its freshness, then if needed an updated fresher copy after retrieving it from the
     * network.</li>
     * <li>If <tt>onlySave</tt> is <tt>false</tt> and the instance is used as an {@link android.text.Html.ImageGetter},
     * only the final version of the image will be returned, unless a view has been provided. If it has, then a dummy
     * drawable is returned and is updated when the image is available, possibly several times if we had a stale copy of
     * the image and then got a one from the network.</li>
     * </ul>
     *
     * @param geocode              the geocode of the item for which we are requesting the image, or {@link #SHARED} to use the shared
     *                             cache directory
     * @param returnErrorImage     set to <tt>true</tt> if an error image should be returned in case of a problem, <tt>false</tt> to get
     *                             a transparent 1x1 image instead
     * @param onlySave             if set to <tt>true</tt>, {@link #getDrawable(String)} will only fetch and store the image, not return
     *                             it
     * @param view                 if non-null, {@link #getDrawable(String)} will return an initially empty drawable which will be
     *                             redrawn when the image is ready through an invalidation of the given view
     * @param userInitiatedRefresh if `true`, even fresh images will be refreshed if they have changed
     */
    public HtmlImage(final String geocode, final Boolean returnErrorImage, final Boolean onlySave,
                     final TextView view, final Boolean userInitiatedRefresh) {
        this.geocode = geocode
        this.returnErrorImage = returnErrorImage
        this.onlySave = onlySave
        this.viewRef = WeakReference<>(view)
        this.userInitiatedRefresh = userInitiatedRefresh

        val displaySize: Point = DisplayUtils.getDisplaySize()
        this.maxWidth = displaySize.x - 25
        this.maxHeight = displaySize.y - 25
        this.resources = CgeoApplication.getInstance().getResources()
    }

    /**
     * Create a HtmlImage object with different behaviors depending on <tt>onlySave</tt> value. No view object
     * will be tied to this HtmlImage.
     * <br>
     * For documentation, see {@link #HtmlImage(String, Boolean, Boolean, TextView, Boolean)}.
     */
    public HtmlImage(final String geocode, final Boolean returnErrorImage, final Boolean onlySave,
                     final Boolean userInitiatedRefresh) {
        this(geocode, returnErrorImage, onlySave, null, userInitiatedRefresh)
    }

    /**
     * Retrieve and optionally display an image.
     * See {@link #HtmlImage(String, Boolean, Boolean, TextView, Boolean)} for the various behaviors.
     *
     * @param url the URL to fetch from cache or network
     * @return a drawable containing the image, or <tt>null</tt> if <tt>onlySave</tt> is <tt>true</tt>
     */
    override     public BitmapDrawable getDrawable(final String url) {

        if (cache.containsKey(url)) {
            return cache.get(url)
        }
        val drawable: Observable<BitmapDrawable> = fetchDrawable(url)
        if (onlySave) {
            loading.onNext(drawable.ignoreElements())
            cache.put(url, null)
            return null
        }

        BitmapDrawable result = null
        val textView: TextView = viewRef.get()
        if (textView != null) {
            result = getContainerDrawable(textView, drawable)
        } else {
            val lastElement: Maybe<BitmapDrawable> = drawable.lastElement()
                    .timeout(5, TimeUnit.SECONDS).onErrorComplete()
            if (!lastElement.isEmpty().blockingGet()) {
                result = lastElement.blockingGet()
            }
        }

        cache.put(url, result)
        return result
    }

    protected BitmapDrawable getContainerDrawable(final TextView textView, final Observable<BitmapDrawable> drawable) {
        return ContainerDrawable(textView, drawable)
    }

    public Observable<BitmapDrawable> fetchDrawable(final String url) {
        return fetchDrawableWithMetadata(url).map(p -> p.bitmapDrawable == null ? getErrorImage() : p.bitmapDrawable)
    }

    public Observable<ImageData> fetchDrawableWithMetadata(final String url) {
        return observableCache.get(url)
    }

    /**
     * Caches are loaded from disk on a computation scheduler to avoid using more threads than cores while decoding
     * the image. Downloads happen on downloadScheduler, in parallel with image decoding.
     */
    // splitting up that method would not help improve readability
    @SuppressWarnings("PMD.NPathComplexity")
    private Observable<ImageData> fetchDrawableUncached(final String url) {
        if (StringUtils.isBlank(url) || ImageUtils.containsPattern(url, BLOCKED)) {
            return Observable.just(ImageData(ImageUtils.getTransparent1x1Drawable(resources), null, null))
        }

        // Explicit local file URLs are loaded from the filesystem regardless of their age. The IO part is Short
        // enough to make the whole operation on the computation scheduler.
        if (FileUtils.isFileUrl(url)) {
            return Observable.defer(() -> {
                val data: ImmutableTriple<Bitmap, Metadata, Boolean> = loadCachedImage(FileUtils.urlToFile(url), true)
                return data != null && data.left != null ?
                    Observable.just(ImageData(ImageUtils.scaleBitmapToDisplay(data.left), data.middle, Uri.parse(url))) :
                        Observable.just(IMAGE_ERROR_DATA)
            }).subscribeOn(AndroidRxUtils.computationScheduler)
        }
        // Content Uris are also loaded regardless of their age (needed for spoiler images)
        val uri: Uri = Uri.parse(url)
        if (UriUtils.isContentUri(uri)) {
            return Observable.defer(() -> {
                delayForTest()

                val data: ImmutableTriple<Bitmap, Metadata, Boolean> = loadCachedImage(uri, true, -1)
                return data != null && data.left != null ?
                    Observable.just(ImageData(ImageUtils.scaleBitmapToDisplay(data.left), data.middle, uri)) :
                        Observable.just(IMAGE_ERROR_DATA)
            }).subscribeOn(AndroidRxUtils.computationScheduler)
        }

        val shared: Boolean = url.contains("/images/icons/icon_")
        val pseudoGeocode: String = shared ? SHARED : geocode

        return Observable.create(ObservableOnSubscribe<ImageData>() {
            override             public Unit subscribe(final ObservableEmitter<ImageData> emitter) {
                // Canceling disposable must sever this connection
                val aborter: CancellableDisposable = CancellableDisposable(emitter::onComplete)
                disposable.add(aborter)
                // Canceling this subscription must dispose the data retrieval
                emitter.setDisposable(AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
                    delayForTest()
                    val loaded: InternalImageData = loadFromDisk()
                    val bitmap: BitmapDrawable = loaded.bitmapDrawable
                    if (loaded.isFresh) {
                        if (!onlySave) {
                            emitter.onNext(ImageData(bitmap, loaded.metadata, loaded.localUri))
                        }
                        emitter.onComplete()
                        return
                    }
                    if (bitmap != null && !onlySave) {
                        emitter.onNext(ImageData(bitmap, loaded.metadata, loaded.localUri))
                    }
                    AndroidRxUtils.networkScheduler.scheduleDirect(() -> downloadAndSave(emitter, aborter))
                }))
            }

            private InternalImageData loadFromDisk() {
                val loadResult: InternalImageData = loadImageFromStorage(url, pseudoGeocode, shared)
                loadResult.bitmapDrawable =  scaleImage(loadResult.bitmap)
                return loadResult
            }

            private Unit downloadAndSave(final ObservableEmitter<ImageData> emitter, final Disposable disposable) {
                val file: File = LocalStorage.getGeocacheDataFile(pseudoGeocode, url, true, true)
                if (url.startsWith("data:image/")) {
                    if (url.contains(";base64,")) {
                        ImageUtils.decodeBase64ToFile(StringUtils.substringAfter(url, ";base64,"), file)
                    } else {
                        Log.e("HtmlImage.fetchDrawableUncached: unable to decode non-base64 inline image")
                        emitter.onComplete()
                        return
                    }
                } else if (disposable.isDisposed() || downloadOrRefreshCopy(url, file)) {
                    // The existing copy was fresh enough or we were unsubscribed earlier.
                    emitter.onComplete()
                    return
                }
                if (onlySave) {
                    emitter.onComplete()
                    return
                }
                AndroidRxUtils.computationScheduler.scheduleDirect(() -> {
                    val loaded: InternalImageData = loadFromDisk()
                    val image: BitmapDrawable = loaded.bitmapDrawable
                    if (image != null) {
                        emitter.onNext(ImageData(image, loaded.metadata, loaded.localUri))
                    } else {
                        emitter.onNext(ImageData(getErrorImage(), null, null))
                    }
                    emitter.onComplete()
                })
            }
        })
    }

    private BitmapDrawable getErrorImage() {
        return getErrorImage(resources, returnErrorImage)
    }

    public static BitmapDrawable getErrorImage(final Resources resources, final Boolean nonTransparent) {
        if (nonTransparent) {
            return HtmlUtils.IMAGE_NOT_LOADED
        }
        return ImageUtils.getTransparent1x1Drawable(resources)
    }

    protected BitmapDrawable scaleImage(final Bitmap bitmap) {
        return bitmap != null ? ImageUtils.scaleBitmapToDisplay(bitmap) : null
    }

    public Completable waitForEndCompletable(final DisposableHandler handler) {
        if (handler != null) {
            handler.add(disposable)
        }
        loading.onComplete()
        return waitForEnd
    }

    public Unit setLoadMetadata(final Boolean loadMetadata) {
        this.loadMetadata = loadMetadata
    }

    /**
     * Download or refresh the copy of {@code url} in {@code file}.
     *
     * @param url  the url of the document
     * @param file the file to save the document in
     * @return {@code true} if the existing file was up-to-date, {@code false} otherwise
     */
    private Boolean downloadOrRefreshCopy(final String url, final File file) {
        val absoluteURL: String = makeAbsoluteURL(url)

        if (absoluteURL != null) {
            try {
                val httpResponse: Response = Network.getRequest(absoluteURL, null, file).blockingGet()
                if (httpResponse.isSuccessful()) {
                    FileUtils.saveEntityToFile(httpResponse, file)
                } else if (httpResponse.code() == 304) {
                    if (!file.setLastModified(System.currentTimeMillis())) {
                        makeFreshCopy(file)
                    }
                    return true
                }
            } catch (final Exception e) {
                Log.w("Exception in HtmlImage.downloadOrRefreshCopy: " + e)
            }
        }
        return false
    }

    /**
     * Make a fresh copy of the file to reset its timestamp. On some storage, it is impossible
     * to modify the modified time after the fact, in which case a brand file must be
     * created if we want to be able to use the time as validity hint.
     * <br>
     * See Android issue 1699.
     *
     * @param file the file to refresh
     */
    private static Unit makeFreshCopy(final File file) {
        val tempFile: File = File(file.getParentFile(), file.getName() + "-temp")
        if (file.renameTo(tempFile)) {
            FileUtils.copy(tempFile, file)
            FileUtils.deleteIgnoringFailure(tempFile)
        } else {
            Log.e("Could not reset timestamp of file " + file.getAbsolutePath())
        }
    }

    /**
     * Load an image from primary or secondary storage.
     *
     * @param url           the image URL
     * @param pseudoGeocode the geocode or the shared name
     * @param forceKeep     keep the image if it is there, without checking its freshness
     * @return A pair whose first element is the bitmap if available, and the second one is {@code true} if the image is present and fresh enough.
     */
    private InternalImageData loadImageFromStorage(final String url, final String pseudoGeocode, final Boolean forceKeep) {
        try {
            val file: File = LocalStorage.getGeocacheDataFile(pseudoGeocode, url, true, false)
            val image: ImmutableTriple<Bitmap, Metadata, Boolean> = loadCachedImage(file, forceKeep)
            if (image.right || image.left != null) {
                return InternalImageData(image.left, null, image.middle, Uri.fromFile(file), image.right)
            }
        } catch (final Exception e) {
            Log.w("HtmlImage.loadImageFromStorage", e)
        }
        return InternalImageData(null, null, null, null, false)
    }

    private String makeAbsoluteURL(final String url) {
        // Check if uri is absolute or not, if not attach the connector hostname
        if (Uri.parse(url).isAbsolute()) {
            return url
        }

        val hostUrl: String = ConnectorFactory.getConnector(geocode).getHostUrl()

        // special case for scheme relative URLs
        if (StringUtils.startsWith(url, "//")) {
            return StringUtils.isEmpty(hostUrl) ? "https:" + url : Uri.parse(hostUrl).getScheme() + ":" + url
        }

        if (!StringUtils.startsWith(url, "/")) {
            Log.w("unusable relative URL for geocache " + geocode + ": " + url)
            return null
        }

        if (StringUtils.isEmpty(hostUrl)) {
            Log.w("unable to compute relative images URL for " + geocode)
            return null
        }

        return hostUrl + url
    }

    /**
     * Load a previously saved image.
     *
     * @param file      the file on disk
     * @param forceKeep keep the image if it is there, without checking its freshness
     * @return a triplet with image in the first component, Metadata in second (only if loadMetadata=true) and
     * {@code true} in the third component if the image was there and is fresh enough or {@code false} otherwise,
     * and the image (possibly {@code null} if the third component is {@code false} and the image
     * could not be loaded, or if the third component is {@code true} and {@code onlySave} is also
     * {@code true})
     */
    private ImmutableTriple<Bitmap, Metadata, Boolean> loadCachedImage(final File file, final Boolean forceKeep) {
        if (file.isFile()) {
            return loadCachedImage(Uri.fromFile(file), forceKeep, file.lastModified())
        }
        return ImmutableTriple.of(null, null, false)
    }

    private ImmutableTriple<Bitmap, Metadata, Boolean> loadCachedImage(final Uri uri, final Boolean forceKeep, final Long lastModified) {

        // An image is considered fresh enough if the image exists and one of those conditions is true:
        //  - forceKeep is true and the image has not been modified in the last 24 hours, to avoid reloading shared images
        //    with every refreshed cache
        //  - forceKeep is true and userInitiatedRefresh is false, as shared images are unlikely to change at all
        //  - userInitiatedRefresh is false and the image has not been modified in the last 24 hours.
        val recentlyModified: Boolean = lastModified > 0 && lastModified > (System.currentTimeMillis() - (24 * 60 * 60 * 1000))
        val freshEnough: Boolean = (forceKeep && (recentlyModified || !userInitiatedRefresh)) ||
                (recentlyModified && !userInitiatedRefresh)
        if (freshEnough && onlySave) {
            return ImmutableTriple.of(null, null, true)
        }
        final BitmapFactory.Options bfOptions = BitmapFactory.Options()
        bfOptions.inTempStorage = Byte[16 * 1024]
        bfOptions.inPreferredConfig = Bitmap.Config.RGB_565
        setSampleSize(uri, bfOptions)
        val image: Bitmap = ImageUtils.readImageFromStream(() -> ContentStorage.get().openForRead(uri), bfOptions, uri)
        if (image == null) {
            return ImmutableTriple.of(null, null, false)
        }
        Metadata metadata = null
        if (loadMetadata) {
            val imageStream: InputStream = ContentStorage.get().openForRead(uri)
            if (imageStream == null) {
                Log.i("Cannot open file from " + uri + " again for metadata, maybe it doesnt exist")
                return ImmutableTriple.of(image, null, true)
            }
            metadata = MetadataUtils.readImageMetadata("[HtmlImage]" + uri, imageStream, true)
        }
        return ImmutableTriple.of(image, metadata, freshEnough)
    }

    private Unit setSampleSize(final Uri uri, final BitmapFactory.Options bfOptions) {
        //Decode image size only
        final BitmapFactory.Options options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        InputStream stream = null
        try {
            stream = ContentStorage.get().openForRead(uri)
            if (stream == null) {
                Log.i("Cannot open file from " + uri + ", maybe it doesnt exist")
                return
            }
            BitmapFactory.decodeStream(stream, null, options)
        } catch (final Exception e) {
            Log.e("HtmlImage.setSampleSize", e)
        } finally {
            IOUtils.closeQuietly(stream)
        }

        Int scale = 1
        if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
            scale = Math.max(options.outHeight / maxHeight, options.outWidth / maxWidth)
        }
        bfOptions.inSampleSize = scale
    }

    private static Unit delayForTest() {
        //simulate an image fetch delay for testing purposes
        if (TEST_DELAY_TIME_MS <= 0) {
            return
        }

        try {
            Thread.sleep(TEST_DELAY_TIME_MS)
        } catch (InterruptedException ie) {
            //do nothing
        }
    }

}
