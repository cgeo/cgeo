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

package cgeo.geocaching.log

import cgeo.geocaching.databinding.LogsItemBinding
import cgeo.geocaching.ui.AbstractViewHolder

import android.view.View

class LogViewHolder : AbstractViewHolder() {
    private Int position
    protected final LogsItemBinding binding

    public LogViewHolder(final View rowView) {
        super(rowView)
        binding = LogsItemBinding.bind(rowView)
    }

    /**
     * Read the position of the cursor pointed to by this holder. <br/>
     * This must be called by the UI thread.
     *
     * @return the cursor position
     */
    public Int getPosition() {
        return position
    }

    /**
     * Set the position of the cursor pointed to by this holder. <br/>
     * This must be called by the UI thread.
     *
     * @param position the cursor position
     */
    public Unit setPosition(final Int position) {
        this.position = position
    }
}
