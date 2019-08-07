package cgeo.geocaching.list;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

public enum ListMarker { // markerId must not change as values stored in the database depend on it
    NO_MARKER(0, 0),
    MARKER1(1, R.drawable.dot_traditional),
    MARKER2(2, R.drawable.dot_multi),
    MARKER3(3, R.drawable.dot_mystery),
    MARKER4(4, R.drawable.dot_event),
    MARKER5(5, R.drawable.dot_virtual);

    public final int markerId;
    public final int resDrawable;

    public static final byte MAX_BITS_PER_MARKER = 3;
    public static final int BITMASK = (1 << MAX_BITS_PER_MARKER) - 1;

    ListMarker(final int markerId, final int resDrawable) {
        this.markerId = markerId;
        this.resDrawable = resDrawable;
    }

    public static int getResDrawable(final int markerId) {
        for (final ListMarker temp : ListMarker.values()) {
            if (temp.markerId == markerId) {
                if (markerId == 0) {
                    Log.e("markerId 0 should never have been assigned");
                }
                return temp.resDrawable;
            }
        }
        Log.e("markerId " + markerId + " not found");
        return 0;
    }
}
