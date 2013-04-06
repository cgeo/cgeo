package cgeo.geocaching.filter;

import java.util.List;

interface IFilterFactory {
    public List<? extends IFilter> getFilters();
}
