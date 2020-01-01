package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class MultiListingFilter extends AbstractFilter {

    private static final double MAX_DISTANCE_KILOMETERS = 0.02;

    public static final Creator<MultiListingFilter> CREATOR = new Parcelable.Creator<MultiListingFilter>() {

        @Override
        public MultiListingFilter createFromParcel(final Parcel in) {
            return new MultiListingFilter(in);
        }

        @Override
        public MultiListingFilter[] newArray(final int size) {
            return new MultiListingFilter[size];
        }
    };

    protected MultiListingFilter() {
        super(R.string.caches_filter_multi_listing);
    }

    public MultiListingFilter(final Parcel in) {
        super(in);
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
        return LevenshteinDistance.getDefaultInstance().apply(current.getName(), next.getName()) < 3;
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
        Collections.sort(sorted, (lhs, rhs) -> Float.compare(lhs.first, rhs.first));
        return sorted;
    }

}
