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

import cgeo.geocaching.models.Geocache

import android.app.Activity

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List

/**
 * Represents an exporter to export a {@link List} of {@link Geocache} to various formats.
 */
interface Export {
    /**
     * Export a {@link List} of {@link Geocache} to various formats.
     *
     * @param caches   The {@link List} of {@link Geocache} to be exported
     * @param activity optional: Some exporters might have an UI which requires an {@link Activity}
     */
    Unit export(List<Geocache> caches, Activity activity)

    /**
     * Get the localized name of this exporter.
     *
     * @return localized name
     */
    String getName()
}
