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

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.SettingsUtils

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle

import androidx.preference.Preference

import java.util.concurrent.atomic.AtomicLong

import io.reactivex.rxjava3.schedulers.Schedulers

class PreferenceOfflinedataFragment : BasePreferenceFragment() {
    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_offlinedata, rootKey)

        PreferenceUtils.setOnPreferenceClickListener(findPreference(getString(R.string.pref_fakekey_preference_maintenance_directories)), preference -> {
            // disable the button, as the cleanup runs in background and should not be invoked a second time
            preference.setEnabled(false)

            val waitDialog: ProgressDialog = ProgressDialog(getActivity())
            waitDialog.setTitle(getString(R.string.init_maintenance_start))
            waitDialog.setMessage(getString(R.string.init_maintenance_ongoing))
            waitDialog.setCancelable(false)
            waitDialog.show()

            AndroidRxUtils.andThenOnUi(Schedulers.io(), DataStore::removeObsoleteGeocacheDataDirectories, () -> {
                val activity: Activity = getActivity()
                if (activity != null) {
                    ActivityMixin.showShortToast(activity, R.string.init_maintenance_finished)
                }
                waitDialog.dismiss()
            })
            return true
        })

        val isDbOnSdCard: Preference = findPreference(getString(R.string.pref_dbonsdcard))
        assert isDbOnSdCard != null
        isDbOnSdCard.setPersistent(false)
        isDbOnSdCard.setOnPreferenceClickListener(preference -> {
            val oldValue: Boolean = Settings.isDbOnSDCard()
            DataStore.moveDatabase(getActivity())
            return oldValue != Settings.isDbOnSDCard()
        })

        val dataDirPreference: Preference = findPreference(getString(R.string.pref_fakekey_dataDir))
        assert dataDirPreference != null
        dataDirPreference.setSummary(Settings.getExternalPrivateCgeoDirectory())
        if (LocalStorage.getAvailableExternalPrivateCgeoDirectories().size() < 2) {
            dataDirPreference.setEnabled(false)
        } else {
            val usedBytes: AtomicLong = AtomicLong()
            dataDirPreference.setOnPreferenceClickListener(preference -> {
                val progress: ProgressDialog = ProgressDialog.show(getActivity(), getString(R.string.calculate_dataDir_title), getString(R.string.calculate_dataDir), true, false)
                AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
                    // calculate disk usage
                    usedBytes.set(FileUtils.getSize(LocalStorage.getExternalPrivateCgeoDirectory()))
                }, () -> {
                    progress.dismiss()
                    SettingsUtils.showExtCgeoDirChooser(this, usedBytes.get())
                })
                return true
            })
        }
    }

    override     public Unit onResume() {
        super.onResume()
        val activity: SettingsActivity = (SettingsActivity) getActivity()
        assert activity != null
        activity.setTitle(R.string.settings_title_offlinedata)
        SettingsUtils.initPublicFolders(this, activity.getCsah())
    }
}
