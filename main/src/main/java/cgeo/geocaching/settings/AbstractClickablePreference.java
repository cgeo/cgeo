package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

// https://www.joehxblog.com/how-to-add-a-long-click-to-an-androidx-preference/
abstract class AbstractClickablePreference extends Preference {

    private final SettingsActivity activity;

    private final View.OnLongClickListener longClickListener = v -> {
        if (!isAuthorized()) {
            return false;
        }

        final AlertDialog.Builder builder = Dialogs.newBuilder(v.getContext());
        builder.setMessage(R.string.auth_forget_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.auth_forget_title)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                    revokeAuthorization();
                    setSummary(R.string.auth_unconnected);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel());
        builder.create().show();

        return true;
    };

    AbstractClickablePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        activity = (SettingsActivity) context;
    }

    AbstractClickablePreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        activity = (SettingsActivity) context;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        setOnPreferenceClickListener(getOnPreferenceClickListener(activity));

        final View itemView = holder.itemView;
        itemView.setOnLongClickListener(this.longClickListener);
    }

    protected abstract OnPreferenceClickListener getOnPreferenceClickListener(SettingsActivity settingsActivity);

    protected boolean isAuthorized() {
        return false;
    }

    protected void revokeAuthorization() {
    }
}
