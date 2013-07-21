package cgeo.geocaching.filter;

import java.util.List;

interface IFilterFactory {
    List<? extends IFilter> getFilters();
}
