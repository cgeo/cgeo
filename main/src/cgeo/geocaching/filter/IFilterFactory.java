package cgeo.geocaching.filter;

import androidx.annotation.NonNull;

import java.util.List;

interface IFilterFactory {
    @NonNull
    List<IFilter> getFilters();
}
