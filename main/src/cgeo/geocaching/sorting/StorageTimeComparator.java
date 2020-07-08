package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.CalendarUtils;

import androidx.annotation.NonNull;

class StorageTimeComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Long.compare(cache1.getUpdated(), cache2.getUpdated());
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return CalendarUtils.yearMonth(cache.getUpdated());
    }
}
