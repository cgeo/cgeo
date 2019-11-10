package cgeo.geocaching.list;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

public enum ListMarker { // markerId must not change as values stored in the database depend on it
    NO_MARKER(0, R.string.caches_listmarker_none, R.drawable.dot_transparent),
    MARKER1(1, R.string.color_green, R.drawable.dot_traditional),
    MARKER2(2, R.string.color_orange, R.drawable.dot_multi),
    MARKER3(3, R.string.color_blue, R.drawable.dot_mystery),
    MARKER4(4, R.string.color_red, R.drawable.dot_event),
    MARKER5(5, R.string.color_turquoise, R.drawable.dot_virtual),
    MARKER6(6, R.string.color_black, R.drawable.dot_black);

    public final int markerId;
    public final int resLabel;
    public final int resDrawable;

    public static final byte MAX_BITS_PER_MARKER = 3;
    public static final int BITMASK = (1 << MAX_BITS_PER_MARKER) - 1;

    ListMarker(final int markerId, final int resLabel, final int resDrawable) {
        this.markerId = markerId;
        this.resLabel = resLabel;
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
        return NO_MARKER.resDrawable;
    }
}
