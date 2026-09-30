package cgeo.geocaching.utils;

import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.utils.functions.Action1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.DisposableObserver;

/** Helper class to retrieve image data and cache it in-memory. Handles local and remote image uris */
public class ImageLoader {

    private String htmlImageCode;

    private final Object imageCacheMutex = new Object();
    private final LeastRecentlyUsedMap<String, HtmlImage.ImageData> imageCache;
    private final Map<String, List<Action1<HtmlImage.ImageData>>> imageCacheListeners = new HashMap<>();
    private final CompositeDisposable imageCacheDisposable = new CompositeDisposable();

    public ImageLoader() {
        this(2);
    }

    public ImageLoader(final int cacheSize) {
        this.htmlImageCode = HtmlImage.SHARED;
        this.imageCache = new LeastRecentlyUsedMap.LruCache<>(cacheSize);
    }

    /** set the code BEFORE starting to use the imageloader */
    public void setCode(final String htmlImageCode) {
        if (Objects.equals(htmlImageCode, this.htmlImageCode)) {
            return;
        }
        this.clear();
        this.htmlImageCode = htmlImageCode == null ? HtmlImage.SHARED : htmlImageCode;
    }

    public void loadImage(final String imageUrl, final Action1<HtmlImage.ImageData> action) {
        loadImage(imageUrl, action, null);
    }

    public void loadImage(final String imageUrl, final Action1<HtmlImage.ImageData> action, final Runnable actionOnCacheMiss) {
        synchronized (imageCacheMutex) {
            if (imageCache.containsKey(imageUrl)) {
                action.call(imageCache.get(imageUrl));
                return;
            }
            if (actionOnCacheMiss != null) {
                actionOnCacheMiss.run();
            }
            if (imageCacheListeners.containsKey(imageUrl)) {
                imageCacheListeners.get(imageUrl).add(action);
                return;
            }

            imageCacheListeners.put(imageUrl, new ArrayList<>(Collections.singletonList(action)));
            final HtmlImage imgGetter = new HtmlImage(this.htmlImageCode, true, false, false);
            imgGetter.setLoadMetadata(true);

            final DisposableObserver<HtmlImage.ImageData> observer = new DisposableObserver<HtmlImage.ImageData>() {
                @Override
                public void onNext(final HtmlImage.ImageData imgData) {
                    synchronized (imageCacheMutex) {
                        imageCache.put(imageUrl, imgData);
                        if (imageCacheListeners.containsKey(imageUrl)) {
                            for (Action1<HtmlImage.ImageData> a : imageCacheListeners.get(imageUrl)) {
                                a.call(imgData);
                            }
                        }
                        imageCacheListeners.remove(imageUrl);
                    }
                }

                @Override
                public void onError(final Throwable e) {
                    Log.w("ImageLoader: could not load " + imageUrl, e);
                    synchronized (imageCacheMutex) {
                        // nothing will arrive for this url any more. Keeping the listeners would make
                        // every later request for the same url register and then wait forever.
                        imageCacheListeners.remove(imageUrl);
                    }
                    removeFromContainer();
                }

                @Override
                public void onComplete() {
                    removeFromContainer();
                }

                /** a terminated subscription has to leave the container, it would otherwise grow with every loaded image */
                private void removeFromContainer() {
                    imageCacheDisposable.delete(this);
                }
            };
            // add before subscribing: the observer may terminate on the main thread while we are still here
            imageCacheDisposable.add(observer);
            imgGetter.fetchDrawableWithMetadata(imageUrl).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
        }
    }

    public void clear() {
        synchronized (imageCacheMutex) {
            imageCache.clear();
            imageCacheListeners.clear();
            imageCacheDisposable.clear();
        }
    }


}
