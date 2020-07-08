package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import java.util.Comparator;

public interface CacheComparator extends Comparator<Geocache> {

    /**
     * returns {@code true} if this is a comparator that is used by the app automatically and was not selected by the
     * user.
     */
    boolean isAutoManaged();

    String getSortableSection(@NonNull Geocache cache);

}
