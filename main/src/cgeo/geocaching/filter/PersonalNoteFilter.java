package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Filter that accepts {@link Geocache}s with a non empty personal note stored locally.
 */
public class PersonalNoteFilter extends AbstractFilter implements IFilterFactory {

    protected PersonalNoteFilter() {
        super(CgeoApplication.getInstance().getString(R.string.caches_filter_personal_note));
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return StringUtils.isNotBlank(cache.getPersonalNote());
    }

    @Override
    @NonNull
    public List<PersonalNoteFilter> getFilters() {
        return Collections.singletonList(this);
    }

}
