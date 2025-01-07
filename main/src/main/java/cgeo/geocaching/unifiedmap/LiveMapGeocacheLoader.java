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

    public static final long PROCESS_DELAY = 3000; // value is in milliseconds
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
            try {
                //quick abort if nothing is requested
                if (this.loader.dirtyTime < 0) {
                    return;
                }

                final Viewport viewport;
                final GeocacheFilter filter;
                synchronized (loader) {
                    //check whether current request was already served with last request
                    viewport = this.loader.viewport;
                    filter = this.loader.filter;
                    logParams = "(vp=" + viewport + ",f=" + filter + ")";

                    //quick exit on invalid viewport
                    if (!Viewport.isValid(viewport)) {
                        Log.iForce(LOGPRAEFIX + "INVALID VIEWPORT " + logParams);
                        setState(State.STOPPED_OK, "");
                        this.loader.dirtyTime = -1;
                        return;
                    }
                    //quick exit on cache hit
                    if (lastViewport != null && lastViewport.includes(viewport) && GeocacheFilter.filtersSame(lastFilter, filter) && (System.currentTimeMillis() - lastRequest) < CACHE_EXPIRY) {
                        Log.iForce(LOGPRAEFIX + "CACHE HIT " + logParams);
                        setState(State.STOPPED_OK, "");
                        this.loader.dirtyTime = -1;
                        return;
                    }

                    //if request is real check if its time to really request
                    final long requestAge = System.currentTimeMillis() - this.loader.dirtyTime;
                    if (requestAge < PROCESS_DELAY) {
                        setState(State.REQUESTED, "");
                        return;
                    }

                    //if we come here we HAVE to do an online request. Reset dirty state
                    this.loader.dirtyTime = -1;
                }

                Log.iForce(LOGPRAEFIX + "START" + logParams);
                setState(State.RUNNING, null);

                // DO ONLINE REQUEST retrieving live caches for x times the requested size (to fill up cache)
                final Viewport retrievalViewport = viewport.resize(3.0);
                final SearchResult searchResult = ConnectorFactory.searchByViewport(retrievalViewport, filter);

                //check for partial results
                final List<String> partialConnectors = searchResult.getPartialConnectors();
                final boolean resultIsPartial = !partialConnectors.isEmpty();
                final String partialConnectorString = TextUtils.join(partialConnectors, s -> s, ", ").toString();
                //check for error results
                final Map<String, StatusCode> connectorErrors = searchResult.getConnectorErrors();
                final boolean resultIsError = !connectorErrors.isEmpty();
                final String errorConnectorString = TextUtils.join(connectorErrors.entrySet(), s -> s.getKey() + ":" + s.getValue(), ", ").toString();

                //Postpprocess result and do logging
                final int originalCount = searchResult.getCount();
                final Set<Geocache> result = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
                final int foundInDbCount = result.size();
                if (filter != null) {
                    filter.filterList(result);
                }
                final int filteredCount = result.size();
                final String logResult = "o:" + originalCount + "/db:" + foundInDbCount + "/f:" + filteredCount + "/partial:" + partialConnectorString + "/error:" + errorConnectorString;

                //store in cache
                final boolean keepInCache = !resultIsError;
                lastRequest = System.currentTimeMillis();
                lastFilter = keepInCache ? filter : null;
                lastViewport = keepInCache ? retrievalViewport : null;
                if (keepInCache && resultIsPartial) {
                    //reduce the cached viewport to one which is NOT partial any more
                    //hack for GC.com: reduce the caches' "lastViewport" to the smallest viewport containing all GC.com.caches
                    lastViewport = Viewport.containingGCliveCaches(result);
                }

                final boolean isPartialForUser = !resultIsError && resultIsPartial && (lastViewport == null || !lastViewport.includes(viewport));

                //cleanup and send result
                if (resultIsError) {
                    setState(State.STOPPED_ERROR, errorConnectorString);
                } else if (isPartialForUser) {
                    setState(State.STOPPED_PARTIAL_RESULT, partialConnectorString);
                } else {
                    setState(State.STOPPED_OK, null);
                }
                if (!resultIsError) {
                    onResult.accept(result);
                }
                Log.iForce(LOGPRAEFIX + "END  " + logParams + ":" + logResult);

            } catch (Exception e) {
                Log.e(LOGPRAEFIX + "UNEXPECTED ERROR" + logParams, e);
                setState(State.STOPPED_ERROR, "Exception:" + e.getMessage());
            }
            //if we have just done an online request, ensure that there's a grace period for the next
            synchronized (loader) {
                if (this.loader.dirtyTime > 0) {
                    this.loader.dirtyTime = System.currentTimeMillis();
                }
            }
        }

        private void setState(final State newState, final String msg) {
            if (lastStateSent != newState) {
                Log.iForce(LOGPRAEFIX + "set state to " + newState + "[" + msg + "]");
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
