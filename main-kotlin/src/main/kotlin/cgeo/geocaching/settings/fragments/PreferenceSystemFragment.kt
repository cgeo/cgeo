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

package cgeo.geocaching.settings.fragments

import cgeo.geocaching.DBInspectionActivity
import cgeo.geocaching.R
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.settings.ViewSettingsActivity
import cgeo.geocaching.storage.extension.OneTimeDialogs
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.ui.dialog.SimpleDialogExamples
import cgeo.geocaching.utils.BranchDetectionHelper
import cgeo.geocaching.utils.DebugUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.SettingsUtils.initPublicFolders
import cgeo.geocaching.utils.SettingsUtils.setPrefClick

import android.content.Intent
import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.PreferenceCategory

class PreferenceSystemFragment : BasePreferenceFragment() {
    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_system, rootKey)
    }

    override     public Unit onResume() {
        super.onResume()
        val activity: SettingsActivity = (SettingsActivity) getActivity()
        assert activity != null
        activity.setTitle(R.string.settings_title_system)

        setPrefClick(this, R.string.pref_fakekey_memory_dump, () -> DebugUtils.createMemoryDump(activity))
        setPrefClick(this, R.string.pref_fakekey_reset_otm, () -> SimpleDialog.of(getActivity()).setMessage(TextParam.id(R.string.init_reset_otm_confirm)).confirm(() -> {
            OneTimeDialogs.resetAll()
            ViewUtils.showShortToast(activity, R.string.init_reset_otm_done)
        }))
        setPrefClick(this, R.string.pref_fakekey_generate_logcat, () -> DebugUtils.createLogcat(activity))
        setPrefClick(this, R.string.pref_fakekey_view_settings, () -> startActivity(Intent(activity, ViewSettingsActivity.class)))
        setPrefClick(this, R.string.pref_fakekey_view_database, () -> startActivity(Intent(activity, DBInspectionActivity.class)))
        setPrefClick(this, R.string.pref_fakekey_gui_testscreen, () -> SimpleDialogExamples.createTestDialog(activity))

        if (BranchDetectionHelper.isDeveloperBuild()) {
            Preference testDir = findPreference(getString(R.string.pref_persistablefolder_testdir))
            if (testDir == null) {
                testDir = Preference(getActivity())
                testDir.setKey(getString(R.string.pref_persistablefolder_testdir))
                testDir.setTitle("Directory for Unit Tests. This setting is only needed for development and only visible in developer builds")
                testDir.setIconSpaceReserved(false)

                val localFileSystem: PreferenceCategory = findPreference(getString(R.string.pref_fakekey_local_filesystem))
                localFileSystem.addPreference(testDir)
            }
        }

        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_debug)), (pref, newValue) -> {
            Log.setDebug((Boolean) newValue)
            return true
        })

        initPublicFolders(this, activity.getCsah())
    }
}
