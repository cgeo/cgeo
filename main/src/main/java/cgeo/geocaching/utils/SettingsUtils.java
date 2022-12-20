package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.OCPreferenceKeys;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParserException;

public class SettingsUtils {

    public enum SettingsType {
        TYPE_STRING("string", ""),
        TYPE_BOOLEAN("boolean", "false"),
        TYPE_INTEGER("integer", "0"),
        TYPE_INTEGER_COMPATIBILITY("int", "0"),     // for reading compatibility with early backups
        TYPE_LONG("long", "0"),
        TYPE_FLOAT("float", "0.0"),
        TYPE_UNKNOWN("unknown", "");

        private final String id;
        private final String defaultString;

        SettingsType(final String id, final String defaultString) {
            this.id = id;
            this.defaultString = defaultString;
        }

        public String getId() {
            return id;
        }

        public String getDefaultString() {
            return defaultString;
        }

        public static List<String> getStringList() {
            final List<String> result = new ArrayList<>();
            for (final SettingsType type : values()) {
                if (type != TYPE_INTEGER_COMPATIBILITY && type != TYPE_UNKNOWN) {
                    result.add(type.id);
                }
            }
            return result;
        }
    }

    private SettingsUtils() {
        // utility class
    }

    public static SettingsType getType(final Object value) {
        if (value instanceof String) {
            return SettingsType.TYPE_STRING;
        } else if (value instanceof Boolean) {
            return SettingsType.TYPE_BOOLEAN;
        } else if (value instanceof Integer) {
            return SettingsType.TYPE_INTEGER;
        } else if (value instanceof Long) {
            return SettingsType.TYPE_LONG;
        } else if (value instanceof Float) {
            return SettingsType.TYPE_FLOAT;
        }
        return SettingsType.TYPE_UNKNOWN;
    }

    public static SettingsType getType(final String type) {
        for (final SettingsType settingsType : SettingsType.values()) {
            if (type.equalsIgnoreCase(settingsType.getId())) {
                return settingsType;
            }
        }
        return SettingsType.TYPE_UNKNOWN;
    }

    public static void putValue(final SharedPreferences.Editor editor, final SettingsType type, final String key, final String value) throws XmlPullParserException, NumberFormatException {
        switch (type) {
            case TYPE_STRING:
                editor.putString(key, value);
                break;
            case TYPE_LONG:
                editor.putLong(key, Long.parseLong(value));
                break;
            case TYPE_INTEGER:
            case TYPE_INTEGER_COMPATIBILITY:
                editor.putInt(key, Integer.parseInt(value));
                break;
            case TYPE_BOOLEAN:
                // do not use Boolean.parseBoolean as it silently ignores malformed values
                if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
                    editor.putBoolean(key, true);
                } else if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
                    editor.putBoolean(key, false);
                } else {
                    throw new NumberFormatException();
                }
                break;
            case TYPE_FLOAT:
                editor.putFloat(key, Float.parseFloat(value));
                break;
            default:
                throw new XmlPullParserException("unknown type");
        }
    }

    public static void setPrefClick(final PreferenceFragmentCompat preferenceFragment, @StringRes final int res, final Runnable action) {
        final Preference preference = preferenceFragment.findPreference(preferenceFragment.getString(res));
        if (preference != null) {
            preference.setOnPreferenceClickListener(p -> {
                action.run();
                return true;
            });
        }
    }

    public static void setPrefSummary(final PreferenceFragmentCompat preferenceFragment, @StringRes final int res, final String summary) {
        final Preference preference = preferenceFragment.findPreference(preferenceFragment.getString(res));
        if (preference != null) {
            preference.setSummary(summary);
        }
    }

    public static void setPrefSummaryActiveStatus(final PreferenceFragmentCompat preferenceFragment, @StringRes final int resConnector, final boolean isActive) {
        setPrefSummary(preferenceFragment, resConnector, isActive ? preferenceFragment.getString(R.string.settings_service_active) : "");
    }

    public static void setAuthTitle(final PreferenceFragmentCompat preferenceFragment, final int prefKeyId, final boolean authorized) {
        final Preference preference = preferenceFragment.findPreference(preferenceFragment.getString(prefKeyId));
        if (preference != null) {
            preference.setTitle(authorized ? R.string.settings_reauthorize : R.string.settings_authorize);
        }
    }

    public static void updateOAuthPreference(final PreferenceFragmentCompat preferenceFragment, final int prefKeyId, final boolean authorized) {
        setAuthTitle(preferenceFragment, prefKeyId, authorized);
        setPrefSummary(preferenceFragment, prefKeyId, preferenceFragment.getString(authorized ? R.string.auth_connected : R.string.auth_unconnected));
    }

    public static void updateOpenCachingAuthPreference(final PreferenceFragmentCompat preferenceFragment, final int prefKeyId) {
        final OCPreferenceKeys key = OCPreferenceKeys.getByAuthId(prefKeyId);
        final boolean authorized = key != null && Settings.hasOAuthAuthorization(key.publicTokenPrefId, key.privateTokenPrefId);
        updateOAuthPreference(preferenceFragment, prefKeyId, authorized);
    }

    public static void initPublicFolders(final PreferenceFragmentCompat preferenceFragment, final ContentStorageActivityHelper csah) {
        for (PersistableFolder folder : PersistableFolder.values()) {
            final Preference preference = preferenceFragment.findPreference(preferenceFragment.getString(folder.getPrefKeyId()));
            if (preference == null) {
                continue;
            }
            preference.setOnPreferenceClickListener(p -> {
                csah.selectPersistableFolder(folder);
                return false;
            });
            preference.setSummary(folder.toUserDisplayableValue());
        }
    }

    /**
     * Shows a list of available mount points.
     */
    public static void showExtCgeoDirChooser(final PreferenceFragmentCompat fragment, final long usedBytes) {
        final List<File> extDirs = LocalStorage.getAvailableExternalPrivateCgeoDirectories();
        final String currentExtDir = LocalStorage.getExternalPrivateCgeoDirectory().getAbsolutePath();
        final List<CharSequence> directories = new ArrayList<>();
        final List<Long> freeSpaces = new ArrayList<>();
        int selectedDirIndex = -1;
        for (final File dir : extDirs) {
            if (StringUtils.equals(currentExtDir, dir.getAbsolutePath())) {
                selectedDirIndex = directories.size();
            }
            final long freeSpace = FileUtils.getFreeDiskSpace(dir);
            freeSpaces.add(freeSpace);
            directories.add(dir.getAbsolutePath());
        }

        final SettingsActivity activity = (SettingsActivity) fragment.getActivity();
        assert activity != null;

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(activity.getString(R.string.settings_title_data_dir_usage, Formatter.formatBytes(usedBytes)));
        builder.setSingleChoiceItems(new ArrayAdapter<CharSequence>(activity,
                android.R.layout.simple_list_item_single_choice,
                formatDirectoryNames(activity, directories, freeSpaces)) {
            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @SuppressWarnings("null")
            @NonNull
            @Override
            public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                view.setEnabled(isEnabled(position));
                return view;
            }

            @Override
            public boolean isEnabled(final int position) {
                return usedBytes < freeSpaces.get(position);
            }
        }, selectedDirIndex, (dialog, itemId) -> {
            SimpleDialog.of(activity).setTitle(R.string.confirm_data_dir_move_title).setMessage(R.string.confirm_data_dir_move).confirm((dialog1, which) -> {
                final File dir = extDirs.get(itemId);
                if (!StringUtils.equals(currentExtDir, dir.getAbsolutePath())) {
                    LocalStorage.changeExternalPrivateCgeoDir(activity, dir.getAbsolutePath());
                }
                Settings.setExternalPrivateCgeoDirectory(dir.getAbsolutePath());
                setPrefSummary(fragment, R.string.pref_fakekey_dataDir, dir.getAbsolutePath());
            });
            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    private static List<CharSequence> formatDirectoryNames(final Activity activity, final List<CharSequence> directories, final List<Long> freeSpaces) {
        final List<CharSequence> truncated = Formatter.truncateCommonSubdir(directories);
        final List<CharSequence> formatted = new ArrayList<>(truncated.size());
        for (int i = 0; i < truncated.size(); i++) {
            formatted.add(activity.getString(R.string.settings_data_dir_item, truncated.get(i), Formatter.formatBytes(freeSpaces.get(i))));
        }
        return formatted;
    }

}
