package cgeo.geocaching.loaders;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;

import android.app.Activity;

import androidx.annotation.NonNull;

public class KeywordGeocacheListLoader extends LiveFilterGeocacheListLoader {

    @NonNull public final String keyword;

    public KeywordGeocacheListLoader(final Activity activity, @NonNull final String keyword) {
        super(activity);
        this.keyword = keyword;
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.NAME;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final NameGeocacheFilter nameFilter = (NameGeocacheFilter) GeocacheFilterType.NAME.create();
        nameFilter.getStringFilter().setTextValue(keyword);
        return nameFilter;
    }

}
