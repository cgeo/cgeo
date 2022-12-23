package cgeo.geocaching.loaders;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;

import android.app.Activity;

import androidx.annotation.NonNull;

public class OwnerGeocacheListLoader extends LiveFilterGeocacheListLoader {

    @NonNull public final String username;

    public OwnerGeocacheListLoader(final Activity activity, @NonNull final String username) {
        super(activity);
        this.username = username;
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.OWNER;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final OwnerGeocacheFilter ownerFilter = (OwnerGeocacheFilter) GeocacheFilterType.OWNER.create();
        ownerFilter.getStringFilter().setTextValue(username);
        return ownerFilter;
    }
}
