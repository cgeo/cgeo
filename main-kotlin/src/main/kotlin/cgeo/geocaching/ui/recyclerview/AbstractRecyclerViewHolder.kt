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

package cgeo.geocaching.ui.recyclerview

import android.view.View

import androidx.recyclerview.widget.RecyclerView

/**
 * Abstract super class for all view holders. It is responsible for the invocation of the view injection code.
 */
abstract class AbstractRecyclerViewHolder : RecyclerView().ViewHolder {

    protected AbstractRecyclerViewHolder(final View view) {
        super(view)
    }

}
