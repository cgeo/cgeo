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

import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog

import android.content.Context
import android.util.AttributeSet

import androidx.annotation.NonNull
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class TextPreference : Preference() {

    public TextPreference(final Context context) {
        super(context)
    }

    public TextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public TextPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)
        val pref: Preference = findPreferenceInHierarchy(getKey())

        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                launchTextDialog(pref.getTitle().toString())
                return false
            })
        }
    }

    public Unit launchTextDialog(final String title) {

        SimpleDialog.ofContext(getContext()).setTitle(TextParam.text(title))
                .input(SimpleDialog.InputOptions().setInitialValue(getPersistedString(null)), s -> {
                    persistString(s)
                    callChangeListener(s)
                })
    }

}
