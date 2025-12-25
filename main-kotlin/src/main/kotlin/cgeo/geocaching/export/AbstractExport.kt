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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R

import android.content.res.Resources

import androidx.annotation.NonNull

abstract class AbstractExport : Export {
    private final String name
    private final String progressTitle

    protected AbstractExport(final Int name) {
        val resources: Resources = CgeoApplication.getInstance().getResources()
        this.name = resources.getString(name)
        progressTitle = resources.getString(R.string.export_progress, this.name)
    }

    override     public String getName() {
        return name
    }

    override     public String toString() {
        // used in the array adapter of the dialog showing the exports
        return getName()
    }

    protected String getProgressTitle() {
        return progressTitle
    }
}
