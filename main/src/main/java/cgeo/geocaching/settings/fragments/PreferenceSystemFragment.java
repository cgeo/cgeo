package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.DBInspectionActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.settings.ViewSettingsActivity;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.BranchDetectionHelper;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.utils.SettingsUtils.initPublicFolders;
import static cgeo.geocaching.utils.SettingsUtils.setPrefClick;

import android.content.Intent;
import android.os.Bundle;

public class PreferenceSystemFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_system, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        activity.setTitle(R.string.settings_title_system);

        setPrefClick(this, R.string.pref_fakekey_memory_dump, () -> DebugUtils.createMemoryDump(activity));
        setPrefClick(this, R.string.pref_fakekey_reset_otm, () -> SimpleDialog.of(getActivity()).setMessage(TextParam.id(R.string.init_reset_otm_confirm)).confirm(() -> {
            OneTimeDialogs.resetAll();
            ViewUtils.showShortToast(activity, R.string.init_reset_otm_done);
        }));
        setPrefClick(this, R.string.pref_fakekey_generate_logcat, () -> DebugUtils.createLogcat(activity));
        setPrefClick(this, R.string.pref_fakekey_view_settings, () -> startActivity(new Intent(activity, ViewSettingsActivity.class)));
        setPrefClick(this, R.string.pref_fakekey_view_database, () -> startActivity(new Intent(activity, DBInspectionActivity.class)));

        findPreference(getString(R.string.pref_persistablefolder_testdir)).setVisible(BranchDetectionHelper.isDeveloperBuild());

        findPreference(getString(R.string.pref_debug)).setOnPreferenceChangeListener((pref, newValue) -> {
            Log.setDebug((Boolean) newValue);
            return true;
        });

        initPublicFolders(this, activity.getCsah());
    }
}
