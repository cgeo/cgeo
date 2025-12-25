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
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.TemplateTextPreference
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.SettingsUtils

import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.PreferenceCategory

import java.util.UUID.randomUUID

class PreferenceLoggingFragment : BasePreferenceFragment() {
    PreferenceCategory logTemplatesCategory

    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_logging, rootKey)
        logTemplatesCategory = findPreference(getString(R.string.preference_category_logging_logtemplates))
    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings_title_logging)

        // Update "Signature" preview
        SettingsUtils.setPrefSummary(this, R.string.pref_signature, Settings.getSignature())
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_signature)), (preference, newValue) -> {
            SettingsUtils.setPrefSummary(this, R.string.pref_signature, Settings.getSignature())
            return true
        })

        // Init "Log Templates"
        recreateLogTemplatePreferences()

        // Update "Log Image Default Caption Prefix"
        SettingsUtils.setPrefSummary(this, R.string.pref_log_image_default_prefix, Settings.getLogImageCaptionDefaultPraefix())
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_log_image_default_prefix)), (preference, newValue) -> {
            SettingsUtils.setPrefSummary(this, R.string.pref_log_image_default_prefix, Settings.getLogImageCaptionDefaultPraefixFor(String.valueOf(newValue)))
            return true
        })
    }

    private Unit recreateLogTemplatePreferences() {
        logTemplatesCategory.removeAll()
        logTemplatesCategory.setVisible(true)
        for (Settings.PrefLogTemplate template : Settings.getLogTemplates()) {
            logTemplatesCategory.addPreference(createLogTemplatePreference(template))
        }
        logTemplatesCategory.addPreference(createLogTemplatePreferenceAddNew())
    }

    private Preference createLogTemplatePreferenceAddNew() {
        val preference: Preference = Preference(requireContext())
        preference.setTitle(R.string.init_log_template_addnew)
        preference.setLayoutResource(R.layout.preference_button)
        preference.setIconSpaceReserved(false)
        preference.setOnPreferenceClickListener(pref -> {
            createLogTemplatePreference(Settings.PrefLogTemplate(randomUUID().toString(), "", "")).launchEditTemplateDialog()
            return true
        })
        return preference
    }

    private TemplateTextPreference createLogTemplatePreference(final Settings.PrefLogTemplate template) {

        val preference: TemplateTextPreference = TemplateTextPreference(requireContext())
        preference.setKey(template.getKey())
        preference.setTitle(template.getTitle())
        preference.setSummary(template.getText())
        preference.setIconSpaceReserved(false)
        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            recreateLogTemplatePreferences()
            return true
        })
        return preference
    }
}
