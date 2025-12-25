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
import cgeo.geocaching.ui.dialog.Dialogs

import android.content.Context
import android.util.AttributeSet
import android.view.View

import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

// https://www.joehxblog.com/how-to-add-a-Long-click-to-an-androidx-preference/
abstract class AbstractClickablePreference : Preference() {

    private final SettingsActivity activity

    private final View.OnLongClickListener longClickListener = v -> {
        if (!isAuthorized()) {
            return false
        }

        final AlertDialog.Builder builder = Dialogs.newBuilder(v.getContext())
        builder.setMessage(R.string.auth_forget_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.auth_forget_title)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                    revokeAuthorization()
                    setSummary(R.string.auth_unconnected_tap_here)
                    setIcon(R.drawable.attribute_firstaid)
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel())
        builder.create().show()

        return true
    }

    AbstractClickablePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        activity = (SettingsActivity) context
    }

    AbstractClickablePreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        activity = (SettingsActivity) context
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)

        setOnPreferenceClickListener(getOnPreferenceClickListener(activity))

        val itemView: View = holder.itemView
        itemView.setOnLongClickListener(this.longClickListener)
    }

    protected abstract OnPreferenceClickListener getOnPreferenceClickListener(SettingsActivity settingsActivity)

    protected Boolean isAuthorized() {
        return false
    }

    protected Unit revokeAuthorization() {
    }
}
