package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.location.IConversion;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.android.gms.maps.GoogleMap;

public class GoogleCachesList {

    protected static final double CIRCLE_RADIUS = 528.0 * IConversion.FEET_TO_KILOMETER * 1000.0;
    public static final float ZINDEX_GEOCACHE = 4;
    public static final float ZINDEX_WAYPOINT = 3;
    public static final float ZINDEX_CIRCLE = 2;

    private Collection<MapObjectOptions> options;

    private final GoogleMapObjectsQueue mapObjects;

    public GoogleCachesList(final GoogleMap googleMap) {
        mapObjects = new GoogleMapObjectsQueue(googleMap);
    }


    private static Set<MapObjectOptions> diff(final Collection<MapObjectOptions> one, final Collection<MapObjectOptions> two) {
        final Set<MapObjectOptions> set = new HashSet<>(one);
        set.removeAll(two);
        return set;
    }


    public void redraw(@NonNull final Collection<? extends MapObjectOptionsFactory> itemsPre, final boolean showCircles) {
        final Collection<MapObjectOptions> options = updateMapObjectOptions(itemsPre, showCircles);
        updateMapObjects(options);
    }

    private void updateMapObjects(@NonNull final Collection<MapObjectOptions> options) {
        if (this.options == options) {
            return; // rare, can happen, be prepared if happens
        }
        if (this.options == null) {
            this.options = options;
            mapObjects.requestAdd(this.options);
        } else {
            final Collection<MapObjectOptions> toRemove = diff(this.options, options);
            final Collection<MapObjectOptions> toAdd = toRemove.size() == this.options.size() ? options : diff(options, this.options);
//            Log.i("From original " + this.options.size()  + " items will be " + toAdd.size() + " added and " + toRemove.size() + " removed to match new count " + options.size());
            this.options = options;

            mapObjects.requestRemove(toRemove);
            mapObjects.requestAdd(toAdd);
        }
    }

    private Collection<MapObjectOptions> updateMapObjectOptions(final Collection<? extends MapObjectOptionsFactory> items, final boolean showCircles) {
        final Collection<MapObjectOptions> options = new ArrayList<>(items.size());
        for (final MapObjectOptionsFactory factory : items) {
            Collections.addAll(options, factory.getMapObjectOptions(showCircles));
        }
        return options;
    }

}
