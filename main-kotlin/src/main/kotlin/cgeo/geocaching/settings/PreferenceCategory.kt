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

package cgeo.geocaching.settings

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.preference.PreferenceViewHolder

class PreferenceCategory : androidx().preference.PreferenceCategory {

    public PreferenceCategory(final Context ctx, final AttributeSet attrs, final Int defStyle) {
        super(ctx, attrs, defStyle)
    }

    public PreferenceCategory(final Context ctx, final AttributeSet attrs) {
        super(ctx, attrs)
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)
        val summary: TextView = (TextView) holder.findViewById(android.R.id.summary)
        if (summary != null) {
            // Enable multiple line support
            summary.setSingleLine(false)
            summary.setMaxLines(10)
        }
    }

}
