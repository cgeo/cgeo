package cgeo.geocaching.ui;

import cgeo.geocaching.network.HtmlImage;

import android.graphics.drawable.BitmapDrawable;

import io.reactivex.rxjava3.core.Observable;

public class DirectionImage {

    private static final HtmlImage HTML_IMAGE = new HtmlImage(HtmlImage.SHARED, false, false, false);

    private DirectionImage() {
        // utility class
    }

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
