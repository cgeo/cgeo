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

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

/**
 * Behaves exactly the same as Preference, only needed to have a separate class to compare against
 */

class PreferenceTextAlwaysShow : Preference() {

    public PreferenceTextAlwaysShow(final Context context) {
        this(context, null)
    }

    public PreferenceTextAlwaysShow(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0)
    }

    public PreferenceTextAlwaysShow(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)
        holder.setDividerAllowedAbove(false)
    }
}
