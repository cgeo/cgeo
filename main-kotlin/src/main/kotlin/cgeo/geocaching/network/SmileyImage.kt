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

package cgeo.geocaching.network

import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.ImageUtils.LineHeightContainerDrawable

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.TextView

import io.reactivex.rxjava3.core.Observable

/**
 * Specialized image class for fetching and displaying smileys in the log book.
 */
class SmileyImage : HtmlImage() {

    public SmileyImage(final String geocode, final TextView view) {
        super(geocode, false, false, view, false)
    }

    override     protected BitmapDrawable scaleImage(final Bitmap bitmap) {
        val view: TextView = viewRef.get()
        if (bitmap == null || view == null) {
            return null
        }
        val drawable: BitmapDrawable = BitmapDrawable(view.getResources(), bitmap)
        drawable.setBounds(ImageUtils.scaleImageToLineHeight(drawable, view))
        return drawable
    }

    override     protected BitmapDrawable getContainerDrawable(final TextView view, final Observable<BitmapDrawable> drawable) {
        return LineHeightContainerDrawable(view, drawable)
    }

}
