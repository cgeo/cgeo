package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.Log;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** accepts requests to load geocaches online but allows configuring of a maximum update rate */
public class LiveMapGeocacheLoader {

    private static final String LOGPRAEFIX = "LiveMapGeocacheLoader:";

    private static final long MIN_AGE_BETWEEN_ONLINE_REQUESTS = 2000; // value is in milliseconds
    public static final long PROCESS_DELAY = 1000; // value is in milliseconds
    private static final long CACHE_EXPIRY = 10 * 60000; // value is in milliseconds

    private final Disposable actionDisposable;

    private long dirtyTime = -1;
    private Viewport viewport;
    private GeocacheFilter filter;

    public enum State { REQUESTED, RUNNING, STOPPED_OK, STOPPED_ERROR, STOPPED_PARTIAL_RESULT }

    private static final class Action implements Runnable {

        private final LiveMapGeocacheLoader loader;
        private final Consumer<State> onStateChange;
        private State lastStateSent = State.STOPPED_OK;
        private final Consumer<Set<Geocache>> onResult;

        private long lastRequest;
        private Set<Geocache> lastResult;
        private GeocacheFilter lastFilter;
        private Viewport lastViewport;
        private boolean lastResultIsPartial;

        Action(final LiveMapGeocacheLoader loader, final Consumer<State> onStateChange, final Consumer<Set<Geocache>> onResult) {
            this.onStateChange = onStateChange;
            this.onResult = onResult;
            this.loader = loader;
        }

        @Override
        public void run() {
            String logParams = "";
            String logResult = "";
            try {
                final long ageOfLastRequest = System.currentTimeMillis() - lastRequest;
                final long dirtyTime = this.loader.dirtyTime;
                final boolean isDirty = dirtyTime >= 0;
                final boolean isDirtyLongEnough = (isDirty && (System.currentTimeMillis() - dirtyTime) > PROCESS_DELAY);
                if (isDirty) {
                    setState(State.REQUESTED);
                }
                //quick abort
                if (!isDirtyLongEnough || ageOfLastRequest < MIN_AGE_BETWEEN_ONLINE_REQUESTS) {
                    return;
                }

                //get parameters
                final Viewport viewport;
                final GeocacheFilter filter;
                synchronized (loader) {
                    this.loader.dirtyTime = -1;
                    viewport = this.loader.viewport;
                    filter = this.loader.filter;
                }

                //do load
                logParams = "(vp=" + viewport + ",f=" + filter + ")";
                Log.iForce(LOGPRAEFIX + "START" + logParams);

                setState(State.RUNNING);
                final Set<Geocache> result;
                boolean resultIsError = false;
                boolean resultIsPartial = false;
                if (!Viewport.isValid(viewport)) {
                    // -> invalid viewport
                    logResult = "invalidViewport";
                    result = Collections.emptySet();
                } else if (ageOfLastRequest < CACHE_EXPIRY && lastViewport != null && lastViewport.includes(viewport) && GeocacheFilter.filtersSame(lastFilter, filter)) {
                    // -> serve from cache
                    result = lastResult;
                    resultIsPartial = lastResultIsPartial;
                    logResult = "cache hit";
                } else if (!Viewport.isValid(viewport)) {
                    logResult = "invalidViewport";
                    result = Collections.emptySet();
                } else {
                    // retrieving live caches
                    final SearchResult searchResult = ConnectorFactory.searchByViewport(viewport.resize(3.0), filter);
                    final boolean isPartial = searchResult.isPartialResult();
                    final int originalCount = searchResult.getCount();
                    result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                    final int foundInDbCount = result.size();
                    if (filter != null) {
                        filter.filterList(result);
                    }
                    final int filteredCount = result.size();
                    resultIsError = searchResult.getError() != StatusCode.NO_ERROR;
                    logResult = "o:" + originalCount + "/db:" + foundInDbCount + "/f:" + filteredCount + "/isPartial:" + isPartial + "/state:" + searchResult.getError();

                    lastRequest = System.currentTimeMillis();

                    //store in cache
                    final boolean keepInCache = !resultIsError || !result.isEmpty();
                    lastResult = keepInCache ? result : null;
                    lastViewport = keepInCache ? viewport : null;
                    lastFilter = keepInCache ? filter : null;
                    lastResultIsPartial = isPartial || (keepInCache && resultIsError);
                    resultIsPartial = lastResultIsPartial;
                    if (resultIsPartial) {
                        //hack for GC.com: reduce the caches' "lastViewport" to the smallest viewport containing all GC.com.caches
                        lastViewport = Viewport.containingGCliveCaches(result);
                    }
                }

                //cleanup and send result
                setState(resultIsError ? State.STOPPED_ERROR : (resultIsPartial ? State.STOPPED_PARTIAL_RESULT : State.STOPPED_OK));
                if (!resultIsError) {
                    onResult.accept(result);
                }
                Log.iForce(LOGPRAEFIX + "END  " + logParams + ":" + logResult);

                logParams = "";
                logResult = "";
            } catch (Exception e) {
                Log.e(LOGPRAEFIX + "Unexpected exception in runner" + logParams + ":" + logResult, e);
            }
        }

        private void setState(final State newState) {
            if (lastStateSent != newState) {
                onStateChange.accept(newState);
                lastStateSent = newState;
            }
        }
    }

    public LiveMapGeocacheLoader(final Consumer<State> onStateChanged, final Consumer<Set<Geocache>> onResult) {
        this.actionDisposable = Schedulers.newThread().schedulePeriodicallyDirect(new Action(this, onStateChanged, onResult), 0, 250, TimeUnit.MILLISECONDS);
    }

    public synchronized void requestUpdate(final Viewport viewport, final GeocacheFilter filter) {
        this.viewport = viewport;
        this.filter = filter;
        this.dirtyTime = this.dirtyTime <= -1 ? System.currentTimeMillis() : this.dirtyTime;
    }

    public void destroy() {
        this.actionDisposable.dispose();
    }
}
