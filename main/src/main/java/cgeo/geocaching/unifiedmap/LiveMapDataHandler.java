package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.Log;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LiveMapDataHandler {

    private final Action action;
    private final Disposable actionDisposable;

    private boolean dirty;
    private final Parameters params = new Parameters();

    private static class Parameters {
        private Viewport viewport;
        private GeocacheFilter filter;
        private boolean enabled;
        private boolean liveEnabled;

        Parameters copy() {
            final Parameters p = new Parameters();
            p.viewport = viewport;
            p.filter = filter;
            p.liveEnabled = liveEnabled;
            p.enabled = enabled;
            return p;
        }
    }

    private static final class Action implements Runnable {

        private final LiveMapDataHandler handler;
        private final UnifiedMapViewModel model;
        private final LiveMapGeocacheLoader liveLoader;

        private GeocacheFilter lastFilter;
        private Viewport lastViewport;
        private boolean lastLiveEnabled = true;

        Action(final LiveMapDataHandler handler, final UnifiedMapViewModel model) {
            this.handler = handler;
            this.model = model;
            this.liveLoader = new LiveMapGeocacheLoader(model.liveLoadStatus::postValue, this::updateLiveLoad);
        }

        private void updateLiveLoad(final Set<Geocache> result) {
            final GeocacheFilter filter = lastFilter;
            if (filter != null) {
                filter.filterList(result);
            }
            model.caches.write(caches -> {
                caches.removeAll(result);
                caches.addAll(result);
            });
        }

        @Override
        public void run() {
            try {
                //fast abort if there's nothing to do
                if (!handler.dirty) {
                    return;
                }

                //get parameters to work on and reset them
                final Parameters params;
                synchronized (this.handler) {
                    handler.dirty = false;
                    params = handler.params.copy();
                }

                if (!params.enabled) {
                    return;
                }

                final boolean filterChanged = !GeocacheFilter.filtersSame(params.filter, lastFilter);
                final boolean lastViewportContainsCurrent = lastViewport != null && lastViewport.includes(params.viewport);

                //database refresh
                if (filterChanged || !lastViewportContainsCurrent) {
                    //get a bit more than just the current viewport. This prevents immediate necessity for a db call on next (small) map move
                    final Viewport refreshedViewport = params.viewport.resize(1.5);
                    final Set<Geocache> dbCaches = MapUtils.getGeocachesFromDatabase(refreshedViewport, params.filter);
                    model.caches.write(caches -> caches.addAll(dbCaches));
                    lastViewport = refreshedViewport;
                }
                //live refresh
                if (params.liveEnabled) {
                    liveLoader.requestUpdate(params.viewport, params.filter, !lastLiveEnabled);
                } else {
                    liveLoader.cancelRequest();
                }
                lastFilter = params.filter;
                lastLiveEnabled = params.liveEnabled;
            } catch (final Exception e) {
                Log.w("LiveMapDataHandler.run", e);
            }
        }

    }

    public LiveMapDataHandler(final UnifiedMapViewModel model) {
        this.action = new Action(this, model);
        this.actionDisposable = Schedulers.newThread().schedulePeriodicallyDirect(action, 0, 250, TimeUnit.MILLISECONDS);
    }

    public synchronized void setViewport(final Viewport viewport) {
        if (Objects.equals(viewport, this.params.viewport)) {
            return;
        }
        this.params.viewport = viewport;
        this.dirty = true;
    }

    public synchronized void setFilter(final GeocacheFilter filter) {
        if (GeocacheFilter.filtersSame(this.params.filter, filter)) {
            return;
        }
        this.params.filter = filter;
        this.dirty = true;
    }

    public synchronized void setLiveEnabled(final boolean enabled) {
        this.params.liveEnabled = enabled;
        this.dirty = true;
    }

    public synchronized void setEnabled(final boolean enabled) {
        this.params.enabled = enabled;
        this.dirty = true;
    }

    public synchronized  boolean isEnabled() {
        return this.params.enabled;
    }

    public void destroy() {
        this.actionDisposable.dispose();
        this.action.liveLoader.destroy();
    }
}
