package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.ButtonPreference;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.StringRes;

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
        findPreference(getString(R.string.pref_dtMarkerOnCacheIcon)).setOnPreferenceChangeListener((preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });
        findPreference(getString(R.string.pref_bigSmileysOnMap)).setOnPreferenceChangeListener((preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });
        findPreference(getString(R.string.pref_visitedWaypointsSemiTransparent)).setOnPreferenceChangeListener((preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });
    }

    public void updateNotificationAudioInfo() {
        setButton(true);
        setButton(false);
    }

    private void setButton(final boolean first) {
        final @StringRes int keyId = first ? R.string.pref_persistableuri_proximity_notification_far : R.string.pref_persistableuri_proximity_notification_close;
        final ButtonPreference bp = findPreference(getString(keyId));
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
            ((SettingsActivity) getActivity()).startProximityNotificationSelector(first);
            return false;
        });
    }

}

