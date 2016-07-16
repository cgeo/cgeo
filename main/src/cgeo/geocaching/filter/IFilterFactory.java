package cgeo.geocaching.filter;

import android.support.annotation.NonNull;

import java.util.List;

interface IFilterFactory {
    @NonNull
    List<? extends IFilter> getFilters();
}
