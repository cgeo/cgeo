package cgeo.geocaching.loaders;

import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.sorting.GeocacheSort;

import android.app.Activity;

import androidx.annotation.NonNull;

import javax.annotation.Nullable;

public class OwnerGeocacheListLoader extends LiveFilterGeocacheListLoader {

    @NonNull public final String username;
    @Nullable public final GeocacheFilterContext filterContext;

    public OwnerGeocacheListLoader(final Activity activity, final GeocacheSort sort, @NonNull final String username, @Nullable final GeocacheFilterContext filterContext) {
        super(activity, sort);
        this.username = username;
        this.filterContext = filterContext;
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.OWNER;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final OwnerGeocacheFilter ownerFilter = GeocacheFilterType.OWNER.create();
        ownerFilter.getStringFilter().setTextValue(username);
        return ownerFilter;
    }

    @Override
    @NonNull
    protected GeocacheFilter getBaseFilter() {
        if (filterContext != null) {
            return filterContext.get();
        }
        return super.getBaseFilter();
    }
}
