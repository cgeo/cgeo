package cgeo.geocaching.filter;

import org.eclipse.jdt.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

interface IFilterFactory extends Serializable {
    @NonNull
    List<? extends IFilter> getFilters();
}
