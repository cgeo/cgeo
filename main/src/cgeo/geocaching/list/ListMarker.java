package cgeo.geocaching.list;

import cgeo.geocaching.R;

public enum ListMarker { // markerId must not change as values stored in the database depend on it
    NO_MARKER(0,0),
    MARKER1(1, R.drawable.dot_traditional),
    MARKER2(2, R.drawable.dot_multi),
    MARKER3(3, R.drawable.dot_mystery),
    MARKER4(4, R.drawable.dot_event);

    ListMarker(final int markerId, final int resDrawable) {
        this.markerId = markerId;
        this.resDrawable = resDrawable;
    }

    public final int markerId;
    public final int resDrawable;

}
