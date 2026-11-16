package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.log.LogTemplatesActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.PreferenceUtils;
import cgeo.geocaching.utils.SettingsUtils;

import android.os.Bundle;

import androidx.preference.Preference;

import java.util.List;
import java.util.stream.Collectors;

public class PreferenceLoggingFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_logging, rootKey);
        final Preference manageLogTemplates = findPreference(getString(R.string.pref_fakekey_logging_logtemplates_manage));
        if (manageLogTemplates != null) {
            manageLogTemplates.setOnPreferenceClickListener(pref -> {
                LogTemplatesActivity.startActivity(requireContext());
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.settings_title_logging);

        // Update "Signature" preview
        SettingsUtils.setPrefSummary(this, R.string.pref_signature, Settings.getSignature());
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_signature)), (preference, newValue) -> {
            SettingsUtils.setPrefSummary(this, R.string.pref_signature, Settings.getSignature());
            return true;
        });

        // Update "Log Templates" summary
        final List<Settings.PrefLogTemplate> templates = Settings.getLogTemplates();
        SettingsUtils.setPrefSummary(this, R.string.pref_fakekey_logging_logtemplates_manage, templates.isEmpty()
                ? getString(R.string.init_log_templates_none)
                : templates.stream().map(Settings.PrefLogTemplate::getTitle).collect(Collectors.joining(", ")));

        // Update "Log Image Default Caption Prefix"
        SettingsUtils.setPrefSummary(this, R.string.pref_log_image_default_prefix, Settings.getLogImageCaptionDefaultPraefix());
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_log_image_default_prefix)), (preference, newValue) -> {
            SettingsUtils.setPrefSummary(this, R.string.pref_log_image_default_prefix, Settings.getLogImageCaptionDefaultPraefixFor(String.valueOf(newValue)));
            return true;
        });
    }
}
