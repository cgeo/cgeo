package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import java.util.Locale;

class FindsComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache) {
        return cache.getLogCounts() != null;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        final int finds1 = cache1.getFindsCount();
        final int finds2 = cache2.getFindsCount();
        return finds2 - finds1;
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return String.format(Locale.getDefault(), "%d", cache.getFindsCount());
    }

}
