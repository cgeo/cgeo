package cgeo.geocaching.sorting;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

/**
 * sort caches by state (normal, disabled, archived)
 */
class StateComparator extends AbstractCacheComparator {
    private final String stateActive = CgeoApplication.getInstance().getString(R.string.cache_status_active);
    private final String stateDisabled = CgeoApplication.getInstance().getString(R.string.cache_status_disabled);
    private final String stateArchived = CgeoApplication.getInstance().getString(R.string.cache_status_archived);

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return getState(cache1) - getState(cache2);
    }

    private static int getState(final Geocache cache) {
        if (cache.isDisabled()) {
            return 1;
        }
        if (cache.isArchived()) {
            return 2;
        }
        return 0;
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        switch (getState(cache)) {
            case 1:
                return stateDisabled;
            case 2:
                return stateArchived;
            default:
                return stateActive;
        }
    }

}
