package cgeo.geocaching.utils;

import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.utils.functions.Action1;

import android.graphics.drawable.BitmapDrawable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.drew.metadata.Metadata;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Helper class for retrieving of image data and keeping it in-memory
 */
public class ImageDataMemoryCache {

    private String htmlImageCode;

    private final Object imageCacheMutex = new Object();
    private final LeastRecentlyUsedMap<String, Pair<BitmapDrawable, Metadata>> imageCache;
    private final Map<String, List<Action1<Pair<BitmapDrawable, Metadata>>>> imageCacheListeners = new HashMap<>();
    private final CompositeDisposable imageCacheDisposable = new CompositeDisposable();

    public ImageDataMemoryCache(final int cacheSize) {
        this.htmlImageCode = HtmlImage.SHARED;
        this.imageCache = new LeastRecentlyUsedMap.LruCache<>(cacheSize);
    }

    public void setCode(final String htmlImageCode) {
        if (Objects.equals(htmlImageCode, this.htmlImageCode)) {
            return;
        }
        this.clear();
        this.htmlImageCode = htmlImageCode == null ? HtmlImage.SHARED : htmlImageCode;
    }

    public void loadImage(final String imageUrl, final Action1<Pair<BitmapDrawable, Metadata>> action) {
        loadImage(imageUrl, action, null);
    }

    public void loadImage(final String imageUrl, final Action1<Pair<BitmapDrawable, Metadata>> action, final Runnable actionOnCacheMiss) {
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
            //TODO: continue editing here
            final Disposable disposable = imgGetter.fetchDrawableWithMetadata(imageUrl).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(img -> {
                        synchronized (imageCacheMutex) {
                            final Pair<BitmapDrawable, Metadata> imgData = Pair.create(img.left, img.right);
                            imageCache.put(imageUrl, imgData);
                            if (imageCacheListeners.containsKey(imageUrl)) {
                                for (Action1<Pair<BitmapDrawable, Metadata>> a : imageCacheListeners.get(imageUrl)) {
                                    a.call(imgData);
                                }
                            }
                            imageCacheListeners.remove(imageUrl);
                        }
                    });
            imageCacheDisposable.add(disposable);
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
