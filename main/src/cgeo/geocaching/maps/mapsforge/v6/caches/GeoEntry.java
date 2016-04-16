package cgeo.geocaching.maps.mapsforge.v6.caches;

import android.support.annotation.NonNull;

/**
 * Created by rsudev on 13.12.15.
 */
public class GeoEntry {

    public final String geocode;
    public final int overlayId;

    public GeoEntry(@NonNull final String geocode, final int overlayId) {
        this.geocode = geocode;
        this.overlayId = overlayId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GeoEntry geoEntry = (GeoEntry) o;

        return geocode.equals(geoEntry.geocode);

    }

    @Override
    public int hashCode() {
        return geocode.hashCode();
    }
}
