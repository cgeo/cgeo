package cgeo.geocaching.loaders;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.sorting.GeocacheSort;

import android.app.Activity;

import androidx.annotation.NonNull;

public class KeywordGeocacheListLoader extends LiveFilterGeocacheListLoader {

    @NonNull public final String keyword;

    public KeywordGeocacheListLoader(final Activity activity, final GeocacheSort sort, @NonNull final String keyword) {
        super(activity, sort);
        this.keyword = keyword;
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.NAME;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final NameGeocacheFilter nameFilter = GeocacheFilterType.NAME.create();
        nameFilter.getStringFilter().setTextValue(keyword);
        return nameFilter;
    }

}
