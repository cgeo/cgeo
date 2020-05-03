package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Filter {@link Geocache}s if they have a user-defined {@link Waypoint}.
 *
 */
public class OwnWaypointFilter extends AbstractFilter implements IFilterFactory {

    public static final Creator<OwnWaypointFilter> CREATOR = new Parcelable.Creator<OwnWaypointFilter>() {

        @Override
        public OwnWaypointFilter createFromParcel(final Parcel in) {
            return new OwnWaypointFilter(in);
        }

        @Override
        public OwnWaypointFilter[] newArray(final int size) {
            return new OwnWaypointFilter[size];
        }
    };

    protected OwnWaypointFilter() {
        super(R.string.caches_filter_own_waypoint);
    }

    protected OwnWaypointFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        for (final Waypoint waypoint : cache.getWaypoints()) {
            if (waypoint.isUserDefined()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {
        return Collections.<IFilter> singletonList(this);
    }
}
