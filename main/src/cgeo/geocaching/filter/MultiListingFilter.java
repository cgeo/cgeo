package cgeo.geocaching.filter;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class MultiListingFilter extends AbstractFilter {

    private static final double MAX_DISTANCE_KILOMETERS = 0.02;

    protected MultiListingFilter() {
        super(R.string.caches_filter_multi_listing);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        // not used in this filter
        return false;
    }

    @Override
    public void filter(@NonNull final List<Geocache> list) {
        final List<Pair<Float, Geocache>> sorted = getDistanceSortedCaches(list);

        final HashSet<Geocache> filtered = new HashSet<>();
        for (int i = 0; i < sorted.size(); i++) {
            final Geocache current = sorted.get(i).second;
            for (int j = i + 1; j < sorted.size(); j++) {
                final Geocache next = sorted.get(j).second;
                if (current.getCoords().distanceTo(next) < MAX_DISTANCE_KILOMETERS && haveSimilarNames(current, next)) {
                    if (ConnectorFactory.getConnector(current) != ConnectorFactory.getConnector(next) && current.isFound() != next.isFound()) {
                        filtered.add(current);
                        filtered.add(next);
                    }
                } else {
                    break;
                }
            }
        }
        list.retainAll(filtered);
    }

    private static boolean haveSimilarNames(final Geocache current, final Geocache next) {
        return StringUtils.getLevenshteinDistance(current.getName(), next.getName()) < 3;
    }

    private static List<Pair<Float, Geocache>> getDistanceSortedCaches(final List<Geocache> list) {
        final GeoData geo = Sensors.getInstance().currentGeo();
        final Geopoint currentPos = new Geopoint(geo);
        final List<Pair<Float, Geocache>> sorted = new ArrayList<>();
        for (final Geocache cache : list) {
            final Geopoint coords = cache.getCoords();
            if (coords != null) {
                final float distance = currentPos.distanceTo(coords);
                sorted.add(new Pair<>(distance, cache));
            }
        }
        Collections.sort(sorted, new Comparator<Pair<Float, Geocache>>() {

            @Override
            public int compare(final Pair<Float, Geocache> lhs, final Pair<Float, Geocache> rhs) {
                return Float.compare(lhs.first, rhs.first);
            }
        });
        return sorted;
    }

}
