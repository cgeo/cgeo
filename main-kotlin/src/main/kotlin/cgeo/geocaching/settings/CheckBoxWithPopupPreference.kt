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

import cgeo.geocaching.R
import cgeo.geocaching.ui.UrlPopup

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet

import androidx.annotation.NonNull
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceViewHolder

class CheckBoxWithPopupPreference : CheckBoxPreference() {

    // strings for the popup dialog
    private String title
    private String text
    private String url
    private String urlButton

    public CheckBoxWithPopupPreference(final Context context) {
        super(context)
    }

    public CheckBoxWithPopupPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        processAttributes(context, attrs, 0)
    }

    public CheckBoxWithPopupPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        processAttributes(context, attrs, defStyle)
    }

    private Unit processAttributes(final Context context, final AttributeSet attrs, final Int defStyle) {
        if (attrs == null) {
            return; // coward's retreat
        }

        // Array need to be ordered. See: http://stackoverflow.com/a/19092511/944936
        val types: TypedArray = context.obtainStyledAttributes(
                attrs,
                Int[]{R.attr.text, R.attr.popupTitle, R.attr.url, R.attr.urlButton},
                defStyle, 0)

        text = types.getString(0)
        title = types.getString(1)
        url = types.getString(2)
        urlButton = types.getString(3)

        types.recycle()
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)

        // show dialog when checkbox enabled
        setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(Boolean) newValue) {
                return true
            }
            UrlPopup(preference.getContext()).show(title, text, url, urlButton)
            return true
        })
    }

}
