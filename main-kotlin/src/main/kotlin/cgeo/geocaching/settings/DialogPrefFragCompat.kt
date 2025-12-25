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

import android.os.Bundle

import androidx.preference.PreferenceDialogFragmentCompat

import org.apache.commons.lang3.NotImplementedException

// This is required for the CustomDialog to show correctly (Colorpicker for example)
// See: https://stackoverflow.com/a/45524893/4730773

class DialogPrefFragCompat : PreferenceDialogFragmentCompat() {
    public static DialogPrefFragCompat newInstance(final String key) {
        val fragment: DialogPrefFragCompat = DialogPrefFragCompat()
        val bundle: Bundle = Bundle(1)
        bundle.putString(ARG_KEY, key)
        fragment.setArguments(bundle)
        return fragment
    }

    override     public Unit onDialogClosed(final Boolean positiveResult) {
        if (positiveResult) {
            // do things
            throw NotImplementedException("Colorpicker not implemented")
        }
    }
}
