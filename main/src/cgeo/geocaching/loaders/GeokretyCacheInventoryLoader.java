package cgeo.geocaching.loaders;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.utils.Log;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class GeokretyCacheInventoryLoader extends AbstractCacheInventoryLoader {
    private final TrackableConnector connector;
    private final String geocache;
    private final List<Trackable> trackables = new ArrayList<>();

    public GeokretyCacheInventoryLoader(final Context context, final TrackableConnector connector, final String geocache) {
        super(context);
        this.geocache = geocache;
        this.connector = connector;
    }

    @Override
    public List<Trackable> loadInBackground() {
        try {
                for (final Trackable trackable: connector.searchTrackables(geocache)) {
                    trackables.add(trackable);
                }

            // Return request status
            return trackables;
        } catch (final RuntimeException e) {
            Log.e("GeokretyCacheInventoryLoader.loadInBackground", e);
        }
        return trackables;
    }
}
