package cgeo.geocaching.ui;

import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.network.HtmlImage;

import rx.Observable;

import android.graphics.drawable.BitmapDrawable;

public class DirectionImage {

    static final private HtmlImage HTML_IMAGE = new HtmlImage(HtmlImage.SHARED, false, StoredList.STANDARD_LIST_ID, false);

    /**
     * Retrieve the direction image corresponding to the direction code.
     *
     * @param directionCode one of the eight cardinal points
     * @return an observable containing zero or more drawables (the last one being the freshest image)
     */
    public static Observable<BitmapDrawable> fetchDrawable(final String directionCode) {
        return HTML_IMAGE.fetchDrawable("https://www.geocaching.com/images/icons/compass/" + directionCode + ".gif");
    }

}
