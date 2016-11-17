package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

abstract class AbstractClickablePreference extends Preference implements View.OnLongClickListener {

    private final SettingsActivity activity;

    AbstractClickablePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        activity = (SettingsActivity) context;
    }

    AbstractClickablePreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        activity = (SettingsActivity) context;
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        setOnPreferenceClickListener(getOnPreferenceClickListener(activity));

        final ListView listView = (ListView) parent;
        listView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                final ListView listView = (ListView) parent;
                final ListAdapter listAdapter = listView.getAdapter();
                final Object obj = listAdapter.getItem(position);
                if (obj instanceof View.OnLongClickListener) {
                    final View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
                    return longListener.onLongClick(view);
                }
                return false;
            }
        });

        return super.onCreateView(parent);
    }

    protected abstract OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity settingsActivity);

    protected boolean isAuthorized() {
        return false;
    }

    protected void revokeAuthorization() {
    }

    @Override
    public boolean onLongClick(final View v) {
        if (!isAuthorized()) {
            return false;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setMessage(R.string.auth_forget_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.auth_forget_title)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        revokeAuthorization();
                        setSummary(R.string.auth_unconnected);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        builder.create().show();

        return true;
    }
}
