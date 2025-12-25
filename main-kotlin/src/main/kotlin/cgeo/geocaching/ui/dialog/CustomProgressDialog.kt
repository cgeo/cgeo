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

package cgeo.geocaching.ui.dialog

import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.Log

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.view.View

import java.lang.reflect.Field

/**
 * Modified progress dialog class which allows hiding the absolute numbers.
 */
class CustomProgressDialog : ProgressDialog() {

    public CustomProgressDialog(final Context context) {
        super(context)
        // @todo super(context, Settings.isLightSkin() ? R.style.cgeoProgressdialogTheme_light : R.style.cgeoProgressdialogTheme)
    }

    override     protected Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        try {
            // Field is private, make it accessible through reflection before hiding it.
            val field: Field = getClass().getSuperclass().getDeclaredField("mProgressNumber")
            field.setAccessible(true)
            ViewUtils.setVisibility((View) field.get(this), View.GONE)
        } catch (final Exception e) { // no multi-catch below SDK 19
            Log.e("Failed to find the progressDialog field 'mProgressNumber'", e)
        }
    }
}
