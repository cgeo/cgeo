package cgeo.geocaching.settings;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.files.SimpleDirChooser;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogTemplate;

import org.apache.commons.lang3.StringUtils;
import org.openintents.intents.FileManagerIntents;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html"> Android Design: Settings</a> for design
 * guidelines and the <a href="http://developer.android.com/guide/topics/ui/settings.html">Settings API Guide</a> for
 * more information on developing a Settings UI.
 *
 * @author koem (initial author)
 */
public class NewSettingsActivity extends PreferenceActivity {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = true;

    private EditText signatureText;

    /**
     * Enum for dir choosers. This is how we can retrieve information about the
     * directory and preference key in onActivityResult() easily just by knowing
     * the result code.
     */
    private enum DirChooserType {
        GPX_IMPORT_DIR(1, Settings.KEY_GPX_IMPORT_DIR, Environment.getExternalStorageDirectory().getPath() + "/gpx"),
        GPX_EXPORT_DIR(2, Settings.KEY_GPX_EXPORT_DIR, Environment.getExternalStorageDirectory().getPath() + "/gpx"),
        THEMES_DIR(3, Settings.KEY_RENDER_THEME_BASE_FOLDER, "");
        public final int requestCode;
        public final String key;
        public final String defaultValue;

        private DirChooserType(int requestCode, String key, String defaultValue) {
            this.requestCode = requestCode;
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public DirChooserType getByKey(int requestCode) {
            for (DirChooserType dct : values()) {
                if (dct.requestCode == requestCode) {
                    return dct;
                }
            }
            return null;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    private void initPreferences() {
        initMapSourcePreference();

        bindSummarysToValues(Settings.KEY_USERNAME, Settings.KEY_PASSWORD,
                Settings.KEY_GCVOTE_PASSWORD, Settings.KEY_SIGNATURE,
                Settings.KEY_MAP_SOURCE, Settings.KEY_RENDER_THEME_BASE_FOLDER,
                Settings.KEY_GPX_EXPORT_DIR, Settings.KEY_GPX_IMPORT_DIR);

        initDirChoosers();
    }

    // workaround, because OnContextItemSelected nor onMenuItemSelected is never called
    OnMenuItemClickListener TEMPLATE_CLICK = new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            LogTemplate template = LogTemplateProvider.getTemplate(item.getItemId());
            if (template != null) {
                insertSignatureTemplate(template);
                return true;
            }
            return false;
        }
    };

    // workaround, because OnContextItemSelected nor onMenuItemSelected is never called
    void setSignatureTextView(EditText view) {
        this.signatureText = view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        // context menu for signature templates
        if (v.getId() == R.id.signature_templates) {
            menu.setHeaderTitle(R.string.init_signature_template_button);
            ArrayList<LogTemplate> templates = LogTemplateProvider.getTemplates();
            for (int i = 0; i < templates.size(); ++i) {
                menu.add(0, templates.get(i).getItemId(), 0, templates.get(i).getResourceId());
                menu.getItem(i).setOnMenuItemClickListener(TEMPLATE_CLICK);
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    private void insertSignatureTemplate(final LogTemplate template) {
        String insertText = "[" + template.getTemplateString() + "]";
        ActivityMixin.insertAtPosition(signatureText, insertText, true);
    }

    /**
     * fill the choice list for map sources
     */
    private void initMapSourcePreference() {
        ListPreference pref = (ListPreference) findPreference(Settings.KEY_MAP_SOURCE);

        List<MapSource> mapSources = MapProviderFactory.getMapSources();
        CharSequence[] entries = new CharSequence[mapSources.size()];
        CharSequence[] values = new CharSequence[mapSources.size()];
        for (int i = 0; i < mapSources.size(); ++i) {
            entries[i] = mapSources.get(i).getName();
            values[i] = String.valueOf(mapSources.get(i).getNumericalId());
        }
        pref.setEntries(entries);
        pref.setEntryValues(values);
    }

    /**
     * fire up a dir chooser on click on the preference
     *
     * @see onActivityResult() for processing of the selected directory
     *
     * @param key
     *            key of the preference
     * @param defaultValue
     *            default directory - in case the preference has never been
     *            set yet
     */
    private void initDirChoosers() {
        for (final DirChooserType dct : DirChooserType.values()) {
            final String dir = Settings.getString(dct.key, dct.defaultValue);

            findPreference(dct.key).setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            startDirChooser(dct, dir);
                            return false;
                        }
                    });
        }
    }

    private void startDirChooser(DirChooserType dct, String startDirectory) {
        try {
            final Intent dirChooser = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
            if (StringUtils.isNotBlank(startDirectory)) {
                dirChooser.setData(Uri.fromFile(new File(startDirectory)));
            }
            dirChooser.putExtra(FileManagerIntents.EXTRA_TITLE,
                    getString(R.string.simple_dir_chooser_title));
            dirChooser.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
                    getString(android.R.string.ok));
            startActivityForResult(dirChooser, dct.requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            // OI file manager not available
            final Intent dirChooser = new Intent(this, SimpleDirChooser.class);
            dirChooser.putExtra(Intents.EXTRA_START_DIR, startDirectory);
            startActivityForResult(dirChooser, dct.requestCode);
        }
    }

    private void setChosenDirectory(DirChooserType dct, Intent data) {
        final String directory = new File(data.getData().getPath()).getAbsolutePath();
        if (StringUtils.isNotBlank(directory)) {
            Preference p = findPreference(dct.key);
            if (p == null) {
                return;
            }
            Settings.setString(dct.key, directory);
            p.setSummary(directory);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        for (DirChooserType dct : DirChooserType.values()) {
            if (requestCode == dct.requestCode) {
                setChosenDirectory(dct, data);
                return;
            }
        }

        switch (requestCode) {
        //            case SELECT_MAPFILE_REQUEST:
        //                if (data.hasExtra(Intents.EXTRA_MAP_FILE)) {
        //                    final String mapFile = data.getStringExtra(Intents.EXTRA_MAP_FILE);
        //                    Settings.setMapFile(mapFile);
        //                    if (!Settings.isValidMapFile(Settings.getMapFile())) {
        //                        showToast(res.getString(R.string.warn_invalid_mapfile));
        //                    }
        //                }
        //                updateMapSourceMenu();
        //                initMapDirectoryEdittext(true);
        //                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.preferences);

        initPreferences();
    }

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /** {@inheritDoc} */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            // loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener bindSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof EditPasswordPreference) {
                if (StringUtils.isBlank((String) value)) {
                    preference.setSummary("");
                } else {
                    preference.setSummary("\u2022 \u2022 \u2022 \u2022 \u2022 \u2022 \u2022 \u2022 \u2022 \u2022");
                }
            } else if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #bindSummaryToValueListener
     */
    private static void bindSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceChangeListener(bindSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        bindSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private void bindSummarysToValues(String... keys) {
        for (String key : keys) {
            bindSummaryToValue(findPreference(key));
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindSummaryToValue(findPreference("example_text"));
            bindSummaryToValue(findPreference("example_list"));
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // addPreferencesFromResource(R.xml.pref_notification);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // addPreferencesFromResource(R.xml.pref_data_sync);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindSummaryToValue(findPreference("sync_frequency"));
        }
    }
}
