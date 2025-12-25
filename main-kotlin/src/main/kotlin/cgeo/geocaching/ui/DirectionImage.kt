// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import cgeo.geocaching.network.HtmlImage

import android.graphics.drawable.BitmapDrawable

import io.reactivex.rxjava3.core.Observable

class DirectionImage {

    private static val HTML_IMAGE: HtmlImage = HtmlImage(HtmlImage.SHARED, false, false, false)

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
        return HTML_IMAGE.fetchDrawable("https://www.geocaching.com/images/icons/compass/" + directionCode + ".gif")
    }

}
