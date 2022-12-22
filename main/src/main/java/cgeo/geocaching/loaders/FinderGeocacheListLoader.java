package cgeo.geocaching.loaders;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;

import android.app.Activity;

import androidx.annotation.NonNull;

public class FinderGeocacheListLoader extends LiveFilterGeocacheListLoader {

    @NonNull public final String username;

    public FinderGeocacheListLoader(final Activity activity, @NonNull final String username) {
        super(activity);
        this.username = username;
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.LOG_ENTRY;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final LogEntryGeocacheFilter foundByFilter = (LogEntryGeocacheFilter) GeocacheFilterType.LOG_ENTRY.create();
        foundByFilter.setFoundByUser(username);
        return foundByFilter;
    }
}
