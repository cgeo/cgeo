package cgeo.geocaching.filter;

import org.eclipse.jdt.annotation.NonNull;

import java.util.List;

interface IFilterFactory {
    @NonNull
    List<? extends IFilter> getFilters();
}
