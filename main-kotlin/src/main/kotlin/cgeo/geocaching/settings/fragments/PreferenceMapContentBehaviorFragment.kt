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
import cgeo.geocaching.settings.ButtonPreference
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.utils.MapMarkerUtils
import cgeo.geocaching.utils.PreferenceUtils

import android.net.Uri
import android.os.Bundle

import androidx.annotation.StringRes
import androidx.preference.Preference

import org.apache.commons.lang3.StringUtils

class PreferenceMapContentBehaviorFragment : BasePreferenceFragment() {

    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_map_content_behavior, rootKey)
    }

    override     public Unit onResume() {
        super.onResume()
        val activity: SettingsActivity = (SettingsActivity) getActivity()
        assert activity != null
        activity.setTitle(R.string.settings_title_map_content_behavior)

        updateNotificationAudioInfo()

        // Clear icon cache when modifying settings that influence icon appearance
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_dtMarkerOnCacheIcon)), (preference, newValue) -> {
            MapMarkerUtils.clearCachedItems()
            return true
        })
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_bigSmileysOnMap)), (preference, newValue) -> {
            MapMarkerUtils.clearCachedItems()
            return true
        })
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_visitedWaypointsSemiTransparent)), (preference, newValue) -> {
            MapMarkerUtils.clearCachedItems()
            return true
        })
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_autozoom_consider_lastcenter)), (preference, newValue) -> {
            setAutozoomSummary(preference, (Boolean) newValue)
            return true
        })
        setAutozoomSummary(findPreference(getString(R.string.pref_autozoom_consider_lastcenter)), Settings.getBoolean(R.string.pref_autozoom_consider_lastcenter, false))
    }

    public Unit updateNotificationAudioInfo() {
        setButton(true)
        setButton(false)
    }

    private Unit setAutozoomSummary(final Preference pref, final Boolean value) {
        pref.setSummary(value ? R.string.init_summary_autozoom_consider_lastcenter_on : R.string.init_summary_autozoom_consider_lastcenter_off)
    }

    private Unit setButton(final Boolean first) {
        final @StringRes Int keyId = first ? R.string.pref_persistableuri_proximity_notification_far : R.string.pref_persistableuri_proximity_notification_close
        val bp: ButtonPreference = findPreference(getString(keyId))
        assert bp != null
        val current: String = Settings.getString(keyId, "")

        bp.setSummary(StringUtils.isNotBlank(current) ? Uri.parse(current).getLastPathSegment() : getString(R.string.proximitynotification_internal))
        if (StringUtils.isNotBlank(current)) {
            bp.hideButton(false)
            bp.setCallback(() -> {
                Settings.putString(keyId, "")
                bp.setSummary(R.string.proximitynotification_internal)
                bp.hideButton(true)
            })
        } else {
            bp.setCallback(null)
            bp.hideButton(true)
        }
        bp.setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) requireActivity()).startProximityNotificationSelector(first)
            return false
        })
    }

}

