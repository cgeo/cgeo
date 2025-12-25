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

package cgeo.geocaching.export

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache

import android.app.Activity

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List

/**
 * Batch upload modified coords
 */
class BatchUploadModifiedCoordinates : AbstractExport() {
    private var modifiedOnly: Boolean = true

    public BatchUploadModifiedCoordinates(final Boolean modifiedOnly) {
        super(R.string.export_modifiedcoords)
        this.modifiedOnly = modifiedOnly
    }

    override     public Unit export(final List<Geocache> cachesList, final Activity activity) {
        final Geocache[] caches = cachesList.toArray(Geocache[0])
        BatchUploadModifiedCoordinatesTask(activity, getProgressTitle(), modifiedOnly).execute(caches)
    }

}
