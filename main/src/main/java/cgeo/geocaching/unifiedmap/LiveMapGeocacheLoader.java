package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
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
        private final BiConsumer<State, String> onStateChange;
        private State lastStateSent = State.STOPPED_OK;
        private final Consumer<Set<Geocache>> onResult;

        private long lastRequest;
        private Set<Geocache> lastResult;
        private GeocacheFilter lastFilter;
        private Viewport lastViewport;

        Action(final LiveMapGeocacheLoader loader, final BiConsumer<State, String> onStateChange, final Consumer<Set<Geocache>> onResult) {
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
                    setState(State.REQUESTED, null);
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

                setState(State.RUNNING, null);
                final Set<Geocache> result;
                boolean resultIsError = false;
                boolean resultIsPartial = false;
                String partialConnectorString = "";
                String errorConnectorString = "";
                if (!Viewport.isValid(viewport)) {
                    // -> invalid viewport
                    logResult = "invalidViewport";
                    result = Collections.emptySet();
                } else if (ageOfLastRequest < CACHE_EXPIRY && lastViewport != null && lastViewport.includes(viewport) && GeocacheFilter.filtersSame(lastFilter, filter)) {
                    // -> serve from cache
                    result = lastResult;
                    logResult = "cache hit";
                } else if (!Viewport.isValid(viewport)) {
                    logResult = "invalidViewport";
                    result = Collections.emptySet();
                } else {
                    // retrieving live caches
                    final Viewport retrievalViewport = viewport.resize(3.0);
                    final SearchResult searchResult = ConnectorFactory.searchByViewport(viewport.resize(3.0), filter);

                    final List<String> partialConnectors = searchResult.getPartialConnectors();
                    final boolean isPartial = !partialConnectors.isEmpty();
                    partialConnectorString = TextUtils.join(partialConnectors, s -> s, ", ").toString();

                    final Map<String, StatusCode> connectorErrors = searchResult.getConnectorErrors();
                    resultIsError = !connectorErrors.isEmpty();
                    errorConnectorString = TextUtils.join(connectorErrors.entrySet(),
                            s -> s.getKey() + ":" + s.getValue(), ", ").toString();

                    final int originalCount = searchResult.getCount();
                    result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                    final int foundInDbCount = result.size();
                    if (filter != null) {
                        filter.filterList(result);
                    }
                    final int filteredCount = result.size();
                    logResult = "o:" + originalCount + "/db:" + foundInDbCount + "/f:" + filteredCount + "/partial:" + partialConnectorString + "/error:" + errorConnectorString;

                    lastRequest = System.currentTimeMillis();

                    //store in cache
                    final boolean keepInCache = !resultIsError;
                    lastResult = keepInCache ? result : null;
                    lastFilter = keepInCache ? filter : null;
                    lastViewport = keepInCache ? retrievalViewport : null;
                    if (keepInCache && isPartial) {
                        //reduce the cached viewport to one which is NOT partial any more
                        //hack for GC.com: reduce the caches' "lastViewport" to the smallest viewport containing all GC.com.caches
                        lastViewport = Viewport.containingGCliveCaches(result);
                    }
                    resultIsPartial = !resultIsError && isPartial && (lastViewport == null || !lastViewport.includes(viewport));
                }

                //cleanup and send result
                if (resultIsError) {
                    setState(State.STOPPED_ERROR, errorConnectorString);
                } else if (resultIsPartial) {
                    setState(State.STOPPED_PARTIAL_RESULT, partialConnectorString);
                } else {
                    setState(State.STOPPED_OK, null);
                }
                if (!resultIsError) {
                    onResult.accept(result);
                }
                Log.iForce(LOGPRAEFIX + "END  " + logParams + ":" + logResult);

                logParams = "";
                logResult = "";
            } catch (Exception e) {
                Log.e(LOGPRAEFIX + "Unexpected exception in runner" + logParams + ":" + logResult, e);
                setState(State.STOPPED_ERROR, "Exception:" + e.getMessage());
            }
        }

        private void setState(final State newState, final String msg) {
            if (lastStateSent != newState) {
                onStateChange.accept(newState, msg);
                lastStateSent = newState;
            }
        }
    }

    public LiveMapGeocacheLoader(final BiConsumer<State, String> onStateChanged, final Consumer<Set<Geocache>> onResult) {
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
