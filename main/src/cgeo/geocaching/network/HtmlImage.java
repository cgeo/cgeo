package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.ImageUtils.ContainerDrawable;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;
import cgeo.geocaching.utils.RxUtils.ObservableCache;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.Html;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;

/**
 * All-purpose image getter that can also be used as a ImageGetter interface when displaying caches.
 */

public class HtmlImage implements Html.ImageGetter {

    private static final String[] BLOCKED = new String[] {
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
    };
    public static final String SHARED = "shared";

    final private String geocode;
    /**
     * on error: return large error image, if {@code true}, otherwise empty 1x1 image
     */
    final private boolean returnErrorImage;
    final private int listId;
    final private boolean onlySave;
    final private int maxWidth;
    final private int maxHeight;
    final private Resources resources;
    protected final TextView view;

    final private ObservableCache<String, BitmapDrawable> observableCache = new ObservableCache<>(new Func1<String, Observable<BitmapDrawable>>() {
        @Override
        public Observable<BitmapDrawable> call(final String url) {
            return fetchDrawableUncached(url);
        }
    });

    // Background loading
    final private PublishSubject<Observable<String>> loading = PublishSubject.create();
    final private Observable<String> waitForEnd = Observable.merge(loading).publish().refCount();
    final private CompositeSubscription subscription = new CompositeSubscription(waitForEnd.subscribe());

    /**
     * Create a new HtmlImage object with different behaviours depending on <tt>onlySave</tt> and <tt>view</tt> values.
     * There are the three possible use cases:
     * <ul>
     *  <li>If onlySave is true, getDrawable() will return null immediately and will queue the image retrieval
     *      and saving in the loading subject. Downloads will start in parallel when the blocking
     *      waitForBackgroundLoading() method is called, and they can be cancelled through the given handler.</li>
     *  <li>If onlySave is false and the instance is called through fetchDrawable(), then an observable for the
     *      given URL will be returned. This observable will emit the local copy of the image if it is present</li>
     *      regardless of its freshness, then if needed an updated fresher copy after retrieving it from the network.
     *  <li>If onlySave is false and the instance is used as an ImageGetter, only the final version of the
     *      image will be returned, unless a view has been provided. If it has, then a dummy drawable is returned
     *      and is updated when the image is available, possibly several times if we had a stale copy of the image
     *      and then got a new one from the network.</li>
     * </ul>
     *
     * @param geocode the geocode of the item for which we are requesting the image
     * @param returnErrorImage set to <tt>true</tt> if an error image should be returned in case of a problem,
     *                         <tt>false</tt> to get a transparent 1x1 image instead
     * @param listId the list this cache belongs to, used to determine if an older image for the offline case can be used or not
     * @param onlySave if set to <tt>true</tt>, {@link #getDrawable(String)} will only fetch and store the image, not return it
     * @param view if non-null, {@link #getDrawable(String)} will return an initially empty drawable which will be redrawn when
     *             the image is ready through an invalidation of the given view
     */
    public HtmlImage(final String geocode, final boolean returnErrorImage, final int listId, final boolean onlySave, final TextView view) {
        this.geocode = geocode;
        this.returnErrorImage = returnErrorImage;
        this.listId = listId;
        this.onlySave = onlySave;
        this.view = view;

        final Point displaySize = Compatibility.getDisplaySize();
        this.maxWidth = displaySize.x - 25;
        this.maxHeight = displaySize.y - 25;
        this.resources = CgeoApplication.getInstance().getResources();
    }

    /**
     * Create a new HtmlImage object with different behaviours depending on <tt>onlySave</tt> value. No view object
     * will be tied to this HtmlImage.
     *
     * For documentation, see {@link #HtmlImage(String, boolean, int, boolean, TextView)}.
     */
    public HtmlImage(final String geocode, final boolean returnErrorImage, final int listId, final boolean onlySave) {
        this(geocode, returnErrorImage, listId, onlySave, null);
    }

    /**
     * Retrieve and optionally display an image.
     * See {@link #HtmlImage(String, boolean, int, boolean, TextView)} for the various behaviours.
     *
     * @param url
     *            the URL to fetch from cache or network
     * @return a drawable containing the image, or <tt>null</tt> if <tt>onlySave</tt> is <tt>true</tt>
     */
    @Nullable
    @Override
    public BitmapDrawable getDrawable(final String url) {
        final Observable<BitmapDrawable> drawable = fetchDrawable(url);
        if (onlySave) {
            loading.onNext(drawable.map(new Func1<BitmapDrawable, String>() {
                @Override
                public String call(final BitmapDrawable bitmapDrawable) {
                    return url;
                }
            }));
            return null;
        }
        return view == null ? drawable.toBlocking().lastOrDefault(null) : getContainerDrawable(drawable);
    }

    protected BitmapDrawable getContainerDrawable(final Observable<BitmapDrawable> drawable) {
        return new ContainerDrawable(view, drawable);
    }

    public Observable<BitmapDrawable> fetchDrawable(final String url) {
        return observableCache.get(url);
    }

    // Caches are loaded from disk on a computation scheduler to avoid using more threads than cores while decoding
    // the image. Downloads happen on downloadScheduler, in parallel with image decoding.
    private Observable<BitmapDrawable> fetchDrawableUncached(final String url) {
        if (StringUtils.isBlank(url) || ImageUtils.containsPattern(url, BLOCKED)) {
            return Observable.just(ImageUtils.getTransparent1x1Drawable(resources));
        }

        // Explicit local file URLs are loaded from the filesystem regardless of their age. The IO part is short
        // enough to make the whole operation on the computation scheduler.
        if (FileUtils.isFileUrl(url)) {
            return Observable.defer(new Func0<Observable<BitmapDrawable>>() {
                @Override
                public Observable<BitmapDrawable> call() {
                    final Bitmap bitmap = loadCachedImage(FileUtils.urlToFile(url), true).left;
                    return bitmap != null ? Observable.just(ImageUtils.scaleBitmapToFitDisplay(bitmap)) : Observable.<BitmapDrawable>empty();
                }
            }).subscribeOn(RxUtils.computationScheduler);
        }

        final boolean shared = url.contains("/images/icons/icon_");
        final String pseudoGeocode = shared ? SHARED : geocode;

        return Observable.create(new OnSubscribe<BitmapDrawable>() {
            @Override
            public void call(final Subscriber<? super BitmapDrawable> subscriber) {
                subscription.add(subscriber);
                subscriber.add(RxUtils.computationScheduler.createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        final ImmutablePair<BitmapDrawable, Boolean> loaded = loadFromDisk();
                        final BitmapDrawable bitmap = loaded.left;
                        if (loaded.right) {
                            subscriber.onNext(bitmap);
                            subscriber.onCompleted();
                            return;
                        }
                        if (bitmap != null && !onlySave) {
                            subscriber.onNext(bitmap);
                        }
                        RxUtils.networkScheduler.createWorker().schedule(new Action0() {
                            @Override public void call() {
                                downloadAndSave(subscriber);
                            }
                        });
                    }
                }));
            }

            private ImmutablePair<BitmapDrawable, Boolean> loadFromDisk() {
                final ImmutablePair<Bitmap, Boolean> loadResult = loadImageFromStorage(url, pseudoGeocode, shared);
                return scaleImage(loadResult);
            }

            private void downloadAndSave(final Subscriber<? super BitmapDrawable> subscriber) {
                final File file = LocalStorage.getStorageFile(pseudoGeocode, url, true, true);
                if (url.startsWith("data:image/")) {
                    if (url.contains(";base64,")) {
                        ImageUtils.decodeBase64ToFile(StringUtils.substringAfter(url, ";base64,"), file);
                    } else {
                        Log.e("HtmlImage.getDrawable: unable to decode non-base64 inline image");
                        subscriber.onCompleted();
                        return;
                    }
                } else if (subscriber.isUnsubscribed() || downloadOrRefreshCopy(url, file)) {
                        // The existing copy was fresh enough or we were unsubscribed earlier.
                        subscriber.onCompleted();
                        return;
                }
                if (onlySave) {
                    subscriber.onCompleted();
                } else {
                    RxUtils.computationScheduler.createWorker().schedule(new Action0() {
                        @Override
                        public void call() {
                            final ImmutablePair<BitmapDrawable, Boolean> loaded = loadFromDisk();
                            final BitmapDrawable image = loaded.left;
                            if (image != null) {
                                subscriber.onNext(image);
                            } else {
                                subscriber.onNext(returnErrorImage ?
                                        new BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.image_not_loaded)) :
                                        ImageUtils.getTransparent1x1Drawable(resources));
                            }
                            subscriber.onCompleted();
                        }
                    });
                }
            }
        });
    }

    @SuppressWarnings("static-method")
    protected ImmutablePair<BitmapDrawable, Boolean> scaleImage(final ImmutablePair<Bitmap, Boolean> loadResult) {
        final Bitmap bitmap = loadResult.left;
        return ImmutablePair.of(bitmap != null ? ImageUtils.scaleBitmapToFitDisplay(bitmap) : null, loadResult.right);
    }

    public Observable<String> waitForEndObservable(@Nullable final CancellableHandler handler) {
        if (handler != null) {
            handler.unsubscribeIfCancelled(subscription);
        }
        loading.onCompleted();
        return waitForEnd;
    }

    /**
     * Download or refresh the copy of <code>url</code> in <code>file</code>.
     *
     * @param url the url of the document
     * @param file the file to save the document in
     * @return <code>true</code> if the existing file was up-to-date, <code>false</code> otherwise
     */
    private boolean downloadOrRefreshCopy(final String url, final File file) {
        final String absoluteURL = makeAbsoluteURL(url);

        if (absoluteURL != null) {
            try {
                final HttpResponse httpResponse = Network.getRequest(absoluteURL, null, file);
                if (httpResponse != null) {
                    final int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        LocalStorage.saveEntityToFile(httpResponse, file);
                    } else if (statusCode == 304) {
                        if (!file.setLastModified(System.currentTimeMillis())) {
                            makeFreshCopy(file);
                        }
                        return true;
                    }
                }
            } catch (final Exception e) {
                Log.e("HtmlImage.downloadOrRefreshCopy", e);
            }
        }
        return false;
    }

    /**
     * Make a fresh copy of the file to reset its timestamp. On some storage, it is impossible
     * to modify the modified time after the fact, in which case a brand new file must be
     * created if we want to be able to use the time as validity hint.
     *
     * See Android issue 1699.
     *
     * @param file the file to refresh
     */
    private static void makeFreshCopy(final File file) {
        final File tempFile = new File(file.getParentFile(), file.getName() + "-temp");
        if (file.renameTo(tempFile)) {
            LocalStorage.copy(tempFile, file);
            FileUtils.deleteIgnoringFailure(tempFile);
        }
        else {
            Log.e("Could not reset timestamp of file " + file.getAbsolutePath());
        }
    }

    /**
     * Load an image from primary or secondary storage.
     *
     * @param url the image URL
     * @param pseudoGeocode the geocode or the shared name
     * @param forceKeep keep the image if it is there, without checking its freshness
     * @return A pair whose first element is the bitmap if available, and the second one is <code>true</code> if the image is present and fresh enough.
     */
    @NonNull
    private ImmutablePair<Bitmap, Boolean> loadImageFromStorage(final String url, final String pseudoGeocode, final boolean forceKeep) {
        try {
            final File file = LocalStorage.getStorageFile(pseudoGeocode, url, true, false);
            final ImmutablePair<Bitmap, Boolean> image = loadCachedImage(file, forceKeep);
            if (image.right || image.left != null) {
                return image;
            }
            final File fileSec = LocalStorage.getStorageSecFile(pseudoGeocode, url, true);
            return loadCachedImage(fileSec, forceKeep);
        } catch (final Exception e) {
            Log.w("HtmlImage.loadImageFromStorage", e);
        }
        return ImmutablePair.of((Bitmap) null, false);
    }

    @Nullable
    private String makeAbsoluteURL(final String url) {
        // Check if uri is absolute or not, if not attach the connector hostname
        // FIXME: that should also include the scheme
        if (Uri.parse(url).isAbsolute()) {
            return url;
        }

        final String host = ConnectorFactory.getConnector(geocode).getHost();
        if (StringUtils.isNotEmpty(host)) {
            final StringBuilder builder = new StringBuilder("http://");
            builder.append(host);
            if (!StringUtils.startsWith(url, "/")) {
                // FIXME: explain why the result URL would be valid if the path does not start with
                // a '/', or signal an error.
                builder.append('/');
            }
            builder.append(url);
            return builder.toString();
        }

        return null;
    }

    /**
     * Load a previously saved image.
     *
     * @param file the file on disk
     * @param forceKeep keep the image if it is there, without checking its freshness
     * @return a pair with <code>true</code> in the second component if the image was there and is fresh enough or <code>false</code> otherwise,
     *         and the image (possibly <code>null</code> if the second component is <code>false</code> and the image
     *         could not be loaded, or if the second component is <code>true</code> and <code>onlySave</code> is also
     *         <code>true</code>)
     */
    @NonNull
    private ImmutablePair<Bitmap, Boolean> loadCachedImage(final File file, final boolean forceKeep) {
        if (file.exists()) {
            final boolean freshEnough = listId >= StoredList.STANDARD_LIST_ID || file.lastModified() > (new Date().getTime() - (24 * 60 * 60 * 1000)) || forceKeep;
            if (freshEnough && onlySave) {
                return ImmutablePair.of((Bitmap) null, true);
            }
            final BitmapFactory.Options bfOptions = new BitmapFactory.Options();
            bfOptions.inTempStorage = new byte[16 * 1024];
            bfOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            setSampleSize(file, bfOptions);
            final Bitmap image = BitmapFactory.decodeFile(file.getPath(), bfOptions);
            if (image == null) {
                Log.e("Cannot decode bitmap from " + file.getPath());
                return ImmutablePair.of((Bitmap) null, false);
            }
            return ImmutablePair.of(image, freshEnough);
        }
        return ImmutablePair.of((Bitmap) null, false);
    }

    private void setSampleSize(final File file, final BitmapFactory.Options bfOptions) {
        //Decode image size only
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(file));
            BitmapFactory.decodeStream(stream, null, options);
        } catch (final FileNotFoundException e) {
            Log.e("HtmlImage.setSampleSize", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        int scale = 1;
        if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
            scale = Math.max(options.outHeight / maxHeight, options.outWidth / maxWidth);
        }
        bfOptions.inSampleSize = scale;
    }

}
