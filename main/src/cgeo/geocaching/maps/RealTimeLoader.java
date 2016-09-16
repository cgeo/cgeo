package cgeo.geocaching.maps;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class RealTimeLoader {

    private OnBadTokensListener onBadTokensListener;
    private OnBusyLoaderListener onBusyLoaderListener;
    final private AtomicBoolean busy = new AtomicBoolean(false);

    public interface OnBadTokensListener {
        void onBadTokens();
    }

    public interface OnBusyLoaderListener {
        void onBusy();
        void onNotBusy();
    }

    /**
     * Set a callback to be called every time an attempt to get the map tokens from geocaching.com fails.
     * If the geocaching.com connector is not activated, the callback will not be invoked.
     *
     * @param onBadTokensListener the listener
     */
    public void setOnBadTokensListener(final OnBadTokensListener onBadTokensListener) {
        this.onBadTokensListener = onBadTokensListener;
    }

    /**
     * Set a callback to be called when the loader service is busy or no longer busy.
     *
     * @param onBusyLoaderListener the listener
     */
    public void setOnBusyLoaderListener(final OnBusyLoaderListener onBusyLoaderListener) {
        this.onBusyLoaderListener = onBusyLoaderListener;
    }

    private Single<SearchResult> downloadByViewport(final Viewport viewport, final MapTokens mapTokens) {
        if (Settings.isGCConnectorActive() && !mapTokens.valid() && onBadTokensListener != null) {
            onBadTokensListener.onBadTokens();
        }
        return ConnectorFactory.searchByViewport(viewport, mapTokens);
    }

    private SearchResult loadByViewport(final Viewport viewport) {
        return DataStore.loadCachedInViewport(viewport, CacheType.ALL);
    }

    private Single<Collection<Geocache>> byViewport(final Viewport viewport, final MapTokens mapTokens) {
        final Single<SearchResult> loaded = Single.fromCallable(new Callable<SearchResult>() {
            @Override
            public SearchResult call() throws Exception {
                return loadByViewport(viewport);
            }
        }).subscribeOn(Schedulers.io());
        final Single<SearchResult> downloaded = downloadByViewport(viewport, mapTokens);
        return loaded.zipWith(downloaded, new BiFunction<SearchResult, SearchResult, Collection<Geocache>>() {
            @Override
            public Collection<Geocache> apply(final SearchResult searchResult, final SearchResult searchResult2) {
                searchResult.addSearchResult(searchResult2);
                return searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            }
        }).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(final Disposable disposable) {
                if (!busy.getAndSet(true) && onBusyLoaderListener != null) {
                    onBusyLoaderListener.onBusy();
                }
            }
        }).doOnEvent(new BiConsumer<Collection<Geocache>, Throwable>() {
            @Override
            public void accept(final Collection<Geocache> geocaches, final Throwable throwable) throws Exception {
                if (busy.getAndSet(false) && onBusyLoaderListener != null) {
                    onBusyLoaderListener.onNotBusy();
                }
            }
        });
    }

    /**
     * Return the latest geocaches present in the viewport given by the observable. If the previous retrieval
     * has not terminated, it gets canceled through RxJava unsubscription mechanisms and the retrieval of the
     * caches in the new viewport is started.
     *
     * @param viewports an observable with the consecutive viewports to consider, the latest always being the interesting one
     * @return the latest interesting viewport so far and a collection of geocaches in this viewport
     */
    // FIXME: it looks like at least the gc.com retrieval methods do apply some filter already using the global settings
    public Observable<ImmutablePair<Viewport, Collection<Geocache>>> loadCaches(final Observable<Viewport> viewports) {
        final Observable<Observable<ImmutablePair<Viewport, Collection<Geocache>>>> observableObservable =
                viewports.observeOn(Schedulers.computation(), false, 1).zipWith(MapTokens.retrieveMapTokens(), new BiFunction<Viewport, MapTokens, ImmutablePair<Viewport, MapTokens>>() {
                    @Override
                    public ImmutablePair<Viewport, MapTokens> apply(final Viewport viewport, final MapTokens mapTokens) {
                        return ImmutablePair.of(viewport, mapTokens);
                    }
                }).map(new Function<ImmutablePair<Viewport, MapTokens>, Observable<ImmutablePair<Viewport, Collection<Geocache>>>>() {
                    @Override
                    public Observable<ImmutablePair<Viewport, Collection<Geocache>>> apply(final ImmutablePair<Viewport, MapTokens> viewportMapTokensImmutablePair) {
                        final Viewport viewport = viewportMapTokensImmutablePair.left;
                        return byViewport(viewport, viewportMapTokensImmutablePair.right).toObservable().map(new Function<Collection<Geocache>, ImmutablePair<Viewport, Collection<Geocache>>>() {
                            @Override
                            public ImmutablePair<Viewport, Collection<Geocache>> apply(final Collection<Geocache> geocaches) {
                                return ImmutablePair.of(viewport, geocaches);
                            }
                        });
                    }
                });
        // Only keep the latest result
        return Observable.switchOnNext(observableObservable);
    }

}
