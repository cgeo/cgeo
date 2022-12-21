package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.AndroidRxUtils;

import androidx.annotation.Nullable;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.map.layer.Layer;

public class SinglePointOverlay extends AbstractCachesOverlay {

    private final Geopoint coords;
    private final WaypointType type;
    private final String geocode;

    public SinglePointOverlay(final NewMap map, final Geopoint coords, final WaypointType type, final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers, @Nullable final String geocode) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers);

        this.coords = coords;
        this.type = type;
        this.geocode = geocode;

        AndroidRxUtils.computationScheduler.scheduleDirect(this::fill);
    }

    @Override
    public int getVisibleCachesCount() {
        // TODO: Check whether the waypoint is within the viewport
        return 1;
    }

    private void fill() {
        try {
            showProgress();

            clearLayers();

            // construct waypoint
            final Waypoint waypoint = new Waypoint("", type, false);
            waypoint.setCoords(coords);
            if (StringUtils.isNotBlank(geocode)) {
                waypoint.setGeocode(geocode);
            }

            addItem(waypoint, false);

            addLayers();

            repaint();
        } finally {
            hideProgress();
        }
    }
}
