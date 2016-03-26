package cgeo.geocaching.settings;

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
import cgeo.geocaching.R;

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

        ListView listView = (ListView)parent;
        listView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                ListAdapter listAdapter = listView.getAdapter();
                Object obj = listAdapter.getItem(position);
                if (obj instanceof View.OnLongClickListener) {
                    View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
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

    protected void revokeAuthorization() {}

    @Override
    public boolean onLongClick(View v) {
        if (!isAuthorized()) {
            return false;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setMessage("You'll be disconnected from this connector. Your currently saved credentials will be lost.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Forget authorization?")
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
