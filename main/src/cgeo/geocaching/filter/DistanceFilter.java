package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;

import java.util.ArrayList;
import java.util.List;

class DistanceFilter extends AbstractFilter {
    private final GeoData geo;
    private final int minDistance;
    private final int maxDistance;

    public DistanceFilter(String name, final int minDistance, final int maxDistance) {
        super(name);
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        geo = CgeoApplication.getInstance().currentGeo();
    }

    @Override
    public boolean accepts(final Geocache cache) {
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

        private static final int[] KILOMETERS = { 0, 2, 5, 10, 20, 50 };

        @Override
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
