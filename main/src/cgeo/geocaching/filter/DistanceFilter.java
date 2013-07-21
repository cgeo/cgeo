package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;

import java.util.ArrayList;
import java.util.List;

class DistanceFilter extends AbstractFilter {
    private final IGeoData geo;
    private final int minDistance;
    private final int maxDistance;

    public DistanceFilter(String name, final int minDistance, final int maxDistance) {
        super(name);
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        geo = cgeoapplication.getInstance().currentGeo();
    }

    @Override
    public boolean accepts(final Geocache cache) {
        final Geopoint currentPos = new Geopoint(geo.getLocation());
        final float distance = currentPos.distanceTo(cache.getCoords());

        return (distance >= minDistance) && (distance <= maxDistance);
    }

    public static class Factory implements IFilterFactory {

        private static final int[] KILOMETERS = { 0, 2, 5, 10, 20, 50 };

        @Override
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<IFilter>(KILOMETERS.length);
            for (int i = 0; i < KILOMETERS.length; i++) {
                final int minRange = KILOMETERS[i];
                final int maxRange;
                if (i < KILOMETERS.length - 1) {
                    maxRange = KILOMETERS[i + 1];
                }
                else {
                    maxRange = Integer.MAX_VALUE;
                }
                final String range = maxRange == Integer.MAX_VALUE ? "> " + String.valueOf(minRange) : String.valueOf(minRange) + " - " + String.valueOf(maxRange);
                final String name = cgeoapplication.getInstance().getResources().getQuantityString(R.plurals.tts_kilometers, maxRange, range);
                filters.add(new DistanceFilter(name, minRange, maxRange));
            }
            return filters;
        }

    }
}
