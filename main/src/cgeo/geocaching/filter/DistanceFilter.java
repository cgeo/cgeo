package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;

import org.eclipse.jdt.annotation.NonNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

class DistanceFilter extends AbstractFilter {
    private static final long serialVersionUID = -4110173222670364694L;
    private transient GeoData geo;
    private final int minDistance;
    private final int maxDistance;

    public DistanceFilter(@NonNull final String name, final int minDistance, final int maxDistance) {
        super(name);
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        geo = Sensors.getInstance().currentGeo();
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        geo = Sensors.getInstance().currentGeo();
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        final Geopoint currentPos = new Geopoint(geo);
        final Geopoint coords = cache.getCoords();
        if (coords == null) {
            // If a cache has no coordinates, consider it to be out of range. It will
            // happen with archived caches.
            return false;
        }
        final float distance = currentPos.distanceTo(coords);
        return distance >= minDistance && distance <= maxDistance;
    }

    public static class Factory implements IFilterFactory {

        private static final long serialVersionUID = 1461003608933602211L;
        private static final int[] KILOMETERS = { 0, 2, 5, 10, 20, 50 };

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<>(KILOMETERS.length);
            for (int i = 0; i < KILOMETERS.length; i++) {
                final int minRange = KILOMETERS[i];
                final int maxRange;
                if (i < KILOMETERS.length - 1) {
                    maxRange = KILOMETERS[i + 1];
                }
                else {
                    maxRange = Integer.MAX_VALUE;
                }
                final String range = maxRange == Integer.MAX_VALUE ? "> " + minRange : minRange + " - " + maxRange;
                final String name = CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.tts_kilometers, maxRange, range);
                filters.add(new DistanceFilter(name, minRange, maxRange));
            }
            return filters;
        }

    }
}
