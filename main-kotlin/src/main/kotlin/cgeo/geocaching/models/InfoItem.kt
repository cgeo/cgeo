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

package cgeo.geocaching.models

import cgeo.geocaching.Intents
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.CheckableItemSelectActivity

import android.app.Activity
import android.content.Intent

import androidx.annotation.StringRes

import java.util.ArrayList

abstract class InfoItem {

    protected final Int id
    @StringRes protected final Int titleResId

    public InfoItem(final Int id, final @StringRes Int titleResId) {
        this.id = id
        this.titleResId = titleResId
    }

    public Int getId() {
        return id
    }

    public @StringRes Int getTitleResId() {
        return titleResId
    }

    public static InfoItem getById(final Int id, final ArrayList<InfoItem> items) {
        for (InfoItem item : items) {
            if (item.id == id) {
                return item
            }
        }
        return null
    }

    public static Unit startActivity(final Activity caller, final String className, final String fieldName, final @StringRes Int title, @StringRes final Int prefKey, final Int defaultSource) {
        // make sure preference items are read once, so that default values are recognized
        val unused: ArrayList<Integer> = Settings.getInfoItems(prefKey, defaultSource)
        // now run selector
        val intent: Intent = Intent(caller, CheckableItemSelectActivity.class)
        intent.putExtra(Intents.EXTRA_TITLE, title)
        intent.putExtra(Intents.EXTRA_ID, prefKey)
        intent.putExtra(Intents.EXTRA_CLASS, className)
        intent.putExtra(Intents.EXTRA_FIELD, fieldName)
        caller.startActivity(intent)
    }

}
