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

import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder

import android.view.View

/**
 * Abstract super class for all view holders. It is responsible for the invocation of the view injection code and for
 * the tagging of views.
 * <br>
 * TODO: Use {@link AbstractRecyclerViewHolder} and the recycler view instead.
 */
abstract class AbstractViewHolder {

    protected AbstractViewHolder(final View view) {
        view.setTag(this)
    }

}
