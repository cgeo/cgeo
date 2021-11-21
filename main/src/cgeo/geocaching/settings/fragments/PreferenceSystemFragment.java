package cgeo.geocaching.settings.fragments;

import static cgeo.geocaching.utils.SettingsUtils.setPrefClick;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.settings.ViewSettingsActivity;
import cgeo.geocaching.utils.DebugUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferenceSystemFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_system, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        activity.setTitle(R.string.settings_title_system);

        setPrefClick(this, R.string.pref_memory_dump, () -> DebugUtils.createMemoryDump(activity));
        setPrefClick(this, R.string.pref_generate_logcat, () -> DebugUtils.createLogcat(activity));
        setPrefClick(this, R.string.pref_generate_infos_downloadmanager, () -> DebugUtils.dumpDownloadmanagerInfos(activity));
        setPrefClick(this, R.string.pref_view_settings, () -> startActivity(new Intent(activity, ViewSettingsActivity.class)));
    }
}
