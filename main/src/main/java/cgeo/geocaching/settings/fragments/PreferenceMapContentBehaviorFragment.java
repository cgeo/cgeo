package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.ButtonPreference;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.PreferenceUtils;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.preference.Preference;

import org.apache.commons.lang3.StringUtils;

public class PreferenceMapContentBehaviorFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_map_content_behavior, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        activity.setTitle(R.string.settings_title_map_content_behavior);

        updateNotificationAudioInfo();

        // Clear icon cache when modifying settings that influence icon appearance
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_dtMarkerOnCacheIcon)), (preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_bigSmileysOnMap)), (preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_visitedWaypointsSemiTransparent)), (preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_autozoom_consider_lastcenter)), (preference, newValue) -> {
            setAutozoomSummary(preference, (Boolean) newValue);
            return true;
        });
        setAutozoomSummary(findPreference(getString(R.string.pref_autozoom_consider_lastcenter)), Settings.getBoolean(R.string.pref_autozoom_consider_lastcenter, false));
    }

    public void updateNotificationAudioInfo() {
        setButton(true);
        setButton(false);
    }

    private void setAutozoomSummary(final Preference pref, final boolean value) {
        pref.setSummary(value ? R.string.init_summary_autozoom_consider_lastcenter_on : R.string.init_summary_autozoom_consider_lastcenter_off);
    }

    private void setButton(final boolean first) {
        final @StringRes int keyId = first ? R.string.pref_persistableuri_proximity_notification_far : R.string.pref_persistableuri_proximity_notification_close;
        final ButtonPreference bp = findPreference(getString(keyId));
        assert bp != null;
        final String current = Settings.getString(keyId, "");

        bp.setSummary(StringUtils.isNotBlank(current) ? Uri.parse(current).getLastPathSegment() : getString(R.string.proximitynotification_internal));
        if (StringUtils.isNotBlank(current)) {
            bp.hideButton(false);
            bp.setCallback(() -> {
                Settings.putString(keyId, "");
                bp.setSummary(R.string.proximitynotification_internal);
                bp.hideButton(true);
            });
        } else {
            bp.setCallback(null);
            bp.hideButton(true);
        }
        bp.setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) requireActivity()).startProximityNotificationSelector(first);
            return false;
        });
    }

}

