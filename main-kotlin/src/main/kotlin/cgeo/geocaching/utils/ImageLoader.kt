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

import cgeo.geocaching.network.HtmlImage
import cgeo.geocaching.utils.functions.Action1

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.Objects

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

/** Helper class to retrieve image data and cache it in-memory. Handles local and remote image uris */
class ImageLoader {

    private String htmlImageCode

    private val imageCacheMutex: Object = Object()
    private final LeastRecentlyUsedMap<String, HtmlImage.ImageData> imageCache
    private final Map<String, List<Action1<HtmlImage.ImageData>>> imageCacheListeners = HashMap<>()
    private val imageCacheDisposable: CompositeDisposable = CompositeDisposable()

    public ImageLoader() {
        this(2)
    }

    public ImageLoader(final Int cacheSize) {
        this.htmlImageCode = HtmlImage.SHARED
        this.imageCache = LeastRecentlyUsedMap.LruCache<>(cacheSize)
    }

    /** set the code BEFORE starting to use the imageloader */
    public Unit setCode(final String htmlImageCode) {
        if (Objects == (htmlImageCode, this.htmlImageCode)) {
            return
        }
        this.clear()
        this.htmlImageCode = htmlImageCode == null ? HtmlImage.SHARED : htmlImageCode
    }

    public Unit loadImage(final String imageUrl, final Action1<HtmlImage.ImageData> action) {
        loadImage(imageUrl, action, null)
    }

    public Unit loadImage(final String imageUrl, final Action1<HtmlImage.ImageData> action, final Runnable actionOnCacheMiss) {
        synchronized (imageCacheMutex) {
            if (imageCache.containsKey(imageUrl)) {
                action.call(imageCache.get(imageUrl))
                return
            }
            if (actionOnCacheMiss != null) {
                actionOnCacheMiss.run()
            }
            if (imageCacheListeners.containsKey(imageUrl)) {
                imageCacheListeners.get(imageUrl).add(action)
                return
            }

            imageCacheListeners.put(imageUrl, ArrayList<>(Collections.singletonList(action)))
            val imgGetter: HtmlImage = HtmlImage(this.htmlImageCode, true, false, false)
            imgGetter.setLoadMetadata(true)

            val disposable: Disposable = imgGetter.fetchDrawableWithMetadata(imageUrl).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(imgData -> {
                        synchronized (imageCacheMutex) {
                            imageCache.put(imageUrl, imgData)
                            if (imageCacheListeners.containsKey(imageUrl)) {
                                for (Action1<HtmlImage.ImageData> a : imageCacheListeners.get(imageUrl)) {
                                    a.call(imgData)
                                }
                            }
                            imageCacheListeners.remove(imageUrl)
                        }
                    })
            imageCacheDisposable.add(disposable)
        }
    }

    public Unit clear() {
        synchronized (imageCacheMutex) {
            imageCache.clear()
            imageCacheListeners.clear()
            imageCacheDisposable.clear()
        }
    }


}
