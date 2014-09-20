package cgeo.geocaching.loaders;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.connector.trackable.GeokretyConnector;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.utils.Log;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class GeokretyInventoryLoader extends AbstractInventoryLoader {
    private final GeokretyConnector connector;
    private final List<TrackableLog> trackables = new ArrayList<>();

    public GeokretyInventoryLoader(final Context context, final TrackableConnector connector) {
        super(context);
        this.connector = (GeokretyConnector) connector;
    }

    @Override
    public List<TrackableLog> loadInBackground() {
        try {
                for (final Trackable trackable: connector.loadInventory()) {
                    trackables.add(new TrackableLog(
                            trackable.getTrackingcode(),
                            trackable.getName(),
                            GeokretyConnector.getId(trackable.getGeocode()),
                            0,
                            trackable.getBrand()
                    ));
                }
            return trackables;
        } catch (final RuntimeException e) {
            Log.e("GeokretyInventoryLoader.loadInBackground", e);
        }
        return trackables;
    }
}
