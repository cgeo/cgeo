package cgeo.geocaching.settings;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectMapfileActivity;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.files.SimpleDirChooser;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogTemplate;

import org.apache.commons.lang3.StringUtils;
import org.openintents.intents.FileManagerIntents;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static final String INTENT_GOTO = "GOTO";
    private static final int INTENT_GOTO_SERVICES = 1;
    private static final String FAKEKEY_MAIN_SCREEN = "fakekey_main_screen";
    private static final String FAKEKEY_SERVICES_SCREEN = "fakekey_services_screen";

    private EditText signatureText;

    /**
     * Enum for dir choosers. This is how we can retrieve information about the
     * directory and preference key in onActivityResult() easily just by knowing
     * the result code.
     */
    private enum DirChooserType {
        GPX_IMPORT_DIR(1, Settings.KEY_GPX_IMPORT_DIR,
                Environment.getExternalStorageDirectory().getPath() + "/gpx"),
        GPX_EXPORT_DIR(2, Settings.KEY_GPX_EXPORT_DIR,
                Environment.getExternalStorageDirectory().getPath() + "/gpx"),
        THEMES_DIR(3, Settings.KEY_RENDER_THEME_BASE_FOLDER, "");
        public final int requestCode;
        public final String key;
        public final String defaultValue;

        private DirChooserType(int requestCode, String key, String defaultValue) {
            this.requestCode = requestCode;
            this.key = key;
            this.defaultValue = defaultValue;
        }
    }

    private final static int DIR_CHOOSER_MAPS_DIRECTORY_REQUEST = 4;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Settings.isLightSkin()) {
            setTheme(R.style.settings_light);
        } else {
            setTheme(R.style.settings);
        }

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        initPreferences();

        Intent intent = getIntent();
        int gotoPage = intent.getIntExtra(INTENT_GOTO, 0);
        if (gotoPage == INTENT_GOTO_SERVICES) {
            // start with services screen
            PreferenceScreen main = (PreferenceScreen) findPreference(FAKEKEY_MAIN_SCREEN);
            int index = findPreference(FAKEKEY_SERVICES_SCREEN).getOrder();
            main.onItemClick(null, null, index, 0);
        }
    }

    private void initPreferences() {
        initMapSourcePreference();
        initDirChoosers();
        initDefaultNavigationPreferences();
        initBackupButtons();
        initDbLocationPreference();
        initDebugPreference();
        initBasicMemberPreferences();
        initSend2CgeoPreferences();

        Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getAll();
        for (String key : prefs.keySet()) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference || pref instanceof EditPasswordPreference) {
                bindSummaryToStringValue(pref);
            } else if (pref instanceof NumberPickerPreference) {
                bindSummaryToIntValue(pref);
            }
        }
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
    @SuppressWarnings("deprecation")
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
     * fill the choice list for default navigation tools
     */
    @SuppressWarnings("deprecation")
    private void initDefaultNavigationPreferences() {

        final List<NavigationAppsEnum> apps = NavigationAppFactory.getInstalledDefaultNavigationApps();

        CharSequence[] entries = new CharSequence[apps.size()];
        CharSequence[] values = new CharSequence[apps.size()];
        for (int i = 0; i < apps.size(); ++i) {
            entries[i] = apps.get(i).toString();
            values[i] = String.valueOf(apps.get(i).id);
        }

        ListPreference pref = (ListPreference) findPreference(Settings.KEY_DEFAULT_NAVIGATION_TOOL);
        pref.setEntries(entries);
        pref.setEntryValues(values);
        pref = (ListPreference) findPreference(Settings.KEY_DEFAULT_NAVIGATION_TOOL_2);
        pref.setEntries(entries);
        pref.setEntryValues(values);
    }

    /**
     * fire up a dir chooser on click on the preference
     *
     * @see #onActivityResult() for processing of the selected directory
     *
     * @param key
     *            key of the preference
     * @param defaultValue
     *            default directory - in case the preference has never been
     *            set yet
     */
    @SuppressWarnings("deprecation")
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

        findPreference(Settings.KEY_MAP_DIRECTORY).setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent(NewSettingsActivity.this,
                                SelectMapfileActivity.class);
                        startActivityForResult(i, DIR_CHOOSER_MAPS_DIRECTORY_REQUEST);
                        return false;
                    }
                });
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

    @SuppressWarnings("deprecation")
    private void setChosenDirectory(DirChooserType dct, Intent data) {
        final String directory = new File(data.getData().getPath()).getAbsolutePath();
        if (StringUtils.isNotBlank(directory)) {
            Preference p = findPreference(dct.key);
            if (p == null) {
                return;
            }
            Settings.putString(dct.key, directory);
            p.setSummary(directory);
        }
    }

    @SuppressWarnings("deprecation")
    public void initBackupButtons() {
        Preference backup = findPreference(Settings.FAKEKEY_PREFERENCE_BACKUP);
        backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final Context context = preference.getContext();
                // avoid overwriting an existing backup with an empty database
                // (can happen directly after reinstalling the app)
                if (cgData.getAllCachesCount() == 0) {
                    ActivityMixin.helpDialog(NewSettingsActivity.this,
                            context.getString(R.string.init_backup),
                            context.getString(R.string.init_backup_unnecessary));
                    return false;
                }

                final ProgressDialog dialog = ProgressDialog.show(context,
                        context.getString(R.string.init_backup),
                        context.getString(R.string.init_backup_running), true, false);
                new Thread() {
                    @Override
                    public void run() {
                        final String backupFileName = cgData.backupDatabase();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                                ActivityMixin.helpDialog(NewSettingsActivity.this,
                                        context.getString(R.string.init_backup_backup),
                                        backupFileName != null
                                                ? context.getString(R.string.init_backup_success)
                                                        + "\n" + backupFileName
                                                : context.getString(R.string.init_backup_failed));
                                VALUE_CHANGE_LISTENER.onPreferenceChange(findPreference(
                                        Settings.FAKEKEY_PREFERENCE_BACKUP_INFO), "");
                            }
                        });
                    }
                }.start();
                return true;
            }
        });

        Preference restore = findPreference(Settings.FAKEKEY_PREFERENCE_RESTORE);
        restore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                ((cgeoapplication) NewSettingsActivity.this.getApplication())
                        .restoreDatabase(NewSettingsActivity.this);
                return true;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void initDbLocationPreference() {
        Preference p = findPreference(Settings.KEY_DB_ON_SDCARD);
        p.setPersistent(false);
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean oldValue = Settings.isDbOnSDCard();
                ((cgeoapplication) NewSettingsActivity.this.getApplication())
                        .moveDatabase(NewSettingsActivity.this);
                return oldValue != Settings.isDbOnSDCard();
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void initDebugPreference() {
        Preference p = findPreference(Settings.KEY_DEBUG);
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.setDebug((Boolean) newValue);
                return true;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void initBasicMemberPreferences() {
        findPreference(Settings.KEY_LOAD_DIRECTION_IMG).setEnabled(
                !Settings.isPremiumMember());
        findPreference(Settings.KEY_SHOW_CAPTCHA).setEnabled(
                !Settings.isPremiumMember());
    }

    private static void initSend2CgeoPreferences() {
        Settings.putString(Settings.KEY_WEBDEVICE_NAME, Settings.getWebDeviceName());
    }

    public static void startWithServicesPage(Context fromActivity) {
        final Intent intent = new Intent(fromActivity, NewSettingsActivity.class);
        intent.putExtra(INTENT_GOTO, INTENT_GOTO_SERVICES);
        fromActivity.startActivity(intent);
    }

    @SuppressWarnings("deprecation")
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
            case DIR_CHOOSER_MAPS_DIRECTORY_REQUEST:
                if (data.hasExtra(Intents.EXTRA_MAP_FILE)) {
                    final String mapFile = data.getStringExtra(Intents.EXTRA_MAP_FILE);
                    Settings.setMapFile(mapFile);
                    if (!Settings.isValidMapFile(Settings.getMapFile())) {
                        ActivityMixin.showToast(this, R.string.warn_invalid_mapfile);
                    }
                }
                initMapSourcePreference();
                findPreference(Settings.KEY_MAP_DIRECTORY).setSummary(
                        Settings.getMapFileDirectory());
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener VALUE_CHANGE_LISTENER = new Preference.OnPreferenceChangeListener() {
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
            } else if (Settings.FAKEKEY_PREFERENCE_BACKUP_INFO.equals(preference.getKey())) {
                File lastBackupFile = cgData.getRestoreFile();
                String text;
                if (lastBackupFile != null) {
                    text = preference.getContext().getString(R.string.init_backup_last) + " "
                            + Formatter.formatTime(lastBackupFile.lastModified())
                            + ", " + Formatter.formatDate(lastBackupFile.lastModified());
                } else {
                    text = preference.getContext().getString(R.string.init_backup_last_no);
                }
                preference.setSummary(text);
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
     * @see #VALUE_CHANGE_LISTENER
     */
    private static void bindSummaryToValue(Preference preference, Object value) {
        // Set the listener to watch for value changes.
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);

        // Trigger the listener immediately with the preference's
        // current value.
        VALUE_CHANGE_LISTENER.onPreferenceChange(preference, value);
    }

    /**
     * auto-care for the summary of the preference of string type with this key
     *
     * @param key
     */
    @SuppressWarnings("deprecation")
    private void bindSummaryToStringValue(Preference pref) {
        if (pref == null) {
            return;
        }

        String value = PreferenceManager
                .getDefaultSharedPreferences(pref.getContext())
                .getString(pref.getKey(), "");

        bindSummaryToValue(pref, value);
    }

    /**
     * auto-care for the summary of the preference of int type with this key
     *
     * @param key
     */
    @SuppressWarnings("deprecation")
    private void bindSummaryToIntValue(Preference pref) {
        if (pref == null) {
            return;
        }

        int value = PreferenceManager
                .getDefaultSharedPreferences(pref.getContext())
                .getInt(pref.getKey(), 0);

        bindSummaryToValue(pref, value);
    }
}
