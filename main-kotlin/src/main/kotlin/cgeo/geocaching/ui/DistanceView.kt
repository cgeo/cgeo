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

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units

import android.content.Context
import android.util.AttributeSet

import androidx.appcompat.widget.AppCompatTextView

class DistanceView : AppCompatTextView() {

    private var cacheCoords: Geopoint = null
    private var distance: Float = null

    public DistanceView(final Context context) {
        super(context)
    }

    public DistanceView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public DistanceView(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
    }

    public Unit setCacheData(final Geopoint cacheCoords, final Float distance) {
        this.cacheCoords = cacheCoords
        this.distance = distance
    }

    public Unit update(final Geopoint coords) {
        if (cacheCoords == null) {
            setText(distance == null ? "?" : "~" + Units.getDistanceFromKilometers(distance))
        } else {
            setText(coords == null ? "?" : Units.getDistanceFromKilometers(coords.distanceTo(cacheCoords)))
        }
    }

    public Unit setTypeface(final Int typeface) {
        setTypeface(getTypeface(), typeface)
    }
}
