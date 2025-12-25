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
import android.content.res.TypedArray
import android.util.AttributeSet

import androidx.annotation.Nullable
import androidx.preference.Preference

/**
 * Base class for preferences which evaluate their XML attributes for further processing.
 */
abstract class AbstractAttributeBasedPreference : Preference() {

    public AbstractAttributeBasedPreference(final Context context) {
        super(context)
    }

    public AbstractAttributeBasedPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        processAttributes(context, attrs, 0)
    }

    public AbstractAttributeBasedPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        processAttributes(context, attrs, defStyle)
    }

    private Unit processAttributes(final Context context, final AttributeSet attrs, final Int defStyle) {
        if (attrs == null) {
            return
        }
        val types: TypedArray = context.obtainStyledAttributes(attrs, getAttributeNames(),
                defStyle, 0)

        processAttributeValues(types)

        types.recycle()
    }

    /**
     * Evaluate the attributes which where requested in {@link AbstractAttributeBasedPreference#getAttributeNames()}.
     */
    protected abstract Unit processAttributeValues(TypedArray values)

    /**
     * @return the names of the attributes you want to read in your preference implementation
     */
    protected abstract Int[] getAttributeNames()

}
