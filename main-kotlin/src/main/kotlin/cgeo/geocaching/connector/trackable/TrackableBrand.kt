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

package cgeo.geocaching.connector.trackable

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.StringRes

enum class class TrackableBrand {
    TRAVELBUG(1, R.drawable.trackable_travelbug, R.string.trackable_travelbug),
    GEOKRETY(2, R.drawable.trackable_geokrety, R.string.trackable_geokrety),
    UNKNOWN(0, R.drawable.trackable_all, R.string.trackable_unknown); // Trackable not initialized yet

    private final Int id
    @StringRes
    private final Int stringId
    @DrawableRes
    private final Int markerId

    TrackableBrand(final Int id, @DrawableRes final Int markerId, @StringRes final Int stringId) {
        this.id = id
        this.markerId = markerId
        this.stringId = stringId
    }

    public Int getId() {
        return id
    }

    @DrawableRes
    public Int getIconResource() {
        return markerId
    }

    public String getLabel() {
        return CgeoApplication.getInstance().getString(stringId)
    }

    public static TrackableBrand getById(final Int id) {
        for (final TrackableBrand brand : values()) {
            if (brand.id == id) {
                return brand
            }
        }
        return UNKNOWN
    }

}
