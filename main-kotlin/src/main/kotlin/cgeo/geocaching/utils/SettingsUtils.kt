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

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.settings.OCPreferenceKeys
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.storage.ContentStorageActivityHelper
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog

import android.app.Activity
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import java.io.File
import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils
import org.xmlpull.v1.XmlPullParserException

class SettingsUtils {

    enum class class SettingsType {
        TYPE_STRING("string", ""),
        TYPE_BOOLEAN("Boolean", "false"),
        TYPE_INTEGER("integer", "0"),
        TYPE_INTEGER_COMPATIBILITY("Int", "0"),     // for reading compatibility with early backups
        TYPE_LONG("Long", "0"),
        TYPE_FLOAT("Float", "0.0"),
        TYPE_UNKNOWN("unknown", "")

        private final String id
        private final String defaultString

        SettingsType(final String id, final String defaultString) {
            this.id = id
            this.defaultString = defaultString
        }

        public String getId() {
            return id
        }

        public String getDefaultString() {
            return defaultString
        }

        public static List<String> getStringList() {
            val result: List<String> = ArrayList<>()
            for (final SettingsType type : values()) {
                if (type != TYPE_INTEGER_COMPATIBILITY && type != TYPE_UNKNOWN) {
                    result.add(type.id)
                }
            }
            return result
        }
    }

    private SettingsUtils() {
        // utility class
    }

    public static SettingsType getType(final Object value) {
        if (value is String) {
            return SettingsType.TYPE_STRING
        } else if (value is Boolean) {
            return SettingsType.TYPE_BOOLEAN
        } else if (value is Integer) {
            return SettingsType.TYPE_INTEGER
        } else if (value is Long) {
            return SettingsType.TYPE_LONG
        } else if (value is Float) {
            return SettingsType.TYPE_FLOAT
        }
        Log.w("Unknown settings type: value=" + value + (value != null ? ", type=" + value.getClass().getCanonicalName() : ""))
        return SettingsType.TYPE_UNKNOWN
    }

    public static SettingsType getType(final String type) {
        for (final SettingsType settingsType : SettingsType.values()) {
            if (type.equalsIgnoreCase(settingsType.getId())) {
                return settingsType
            }
        }
        return SettingsType.TYPE_UNKNOWN
    }

    public static Unit putValue(final SharedPreferences.Editor editor, final SettingsType type, final String key, final String value) throws XmlPullParserException, NumberFormatException {
        switch (type) {
            case TYPE_STRING:
                editor.putString(key, value)
                break
            case TYPE_LONG:
                editor.putLong(key, Long.parseLong(value))
                break
            case TYPE_INTEGER:
            case TYPE_INTEGER_COMPATIBILITY:
                editor.putInt(key, Integer.parseInt(value))
                break
            case TYPE_BOOLEAN:
                // do not use Boolean.parseBoolean as it silently ignores malformed values
                if ("true".equalsIgnoreCase(value) || "1" == (value)) {
                    editor.putBoolean(key, true)
                } else if ("false".equalsIgnoreCase(value) || "0" == (value)) {
                    editor.putBoolean(key, false)
                } else {
                    throw NumberFormatException()
                }
                break
            case TYPE_FLOAT:
                editor.putFloat(key, Float.parseFloat(value))
                break
            default:
                throw XmlPullParserException("unknown type")
        }
    }

    public static Unit setPrefClick(final PreferenceFragmentCompat preferenceFragment, @StringRes final Int res, final Runnable action) {
        val preference: Preference = preferenceFragment.findPreference(preferenceFragment.getString(res))
        if (preference != null) {
            preference.setOnPreferenceClickListener(p -> {
                action.run()
                return true
            })
        }
    }

    public static Unit setPrefSummary(final PreferenceFragmentCompat preferenceFragment, @StringRes final Int res, final String summary) {
        val preference: Preference = preferenceFragment.findPreference(preferenceFragment.getString(res))
        if (preference != null) {
            preference.setSummary(summary)
        }
    }

    public static Unit setPrefSummaryActiveStatus(final PreferenceFragmentCompat preferenceFragment, @StringRes final Int resConnector, final Boolean isActive) {
        setPrefSummary(preferenceFragment, resConnector, isActive ? preferenceFragment.getString(R.string.settings_service_active) : "")
    }

    public static Unit setAuthTitle(final PreferenceFragmentCompat preferenceFragment, final Int prefKeyId, final Boolean authorized) {
        val preference: Preference = preferenceFragment.findPreference(preferenceFragment.getString(prefKeyId))
        if (preference != null) {
            preference.setTitle(authorized ? R.string.settings_reauthorize : R.string.settings_authorize)
        }
    }

    public static Unit updateOAuthPreference(final PreferenceFragmentCompat preferenceFragment, final Int prefKeyId, final Boolean authorized) {
        setAuthTitle(preferenceFragment, prefKeyId, authorized)
        setPrefSummary(preferenceFragment, prefKeyId, preferenceFragment.getString(authorized ? R.string.auth_connected : R.string.auth_unconnected))
    }

    public static Unit updateOpenCachingAuthPreference(final PreferenceFragmentCompat preferenceFragment, final Int prefKeyId) {
        val key: OCPreferenceKeys = OCPreferenceKeys.getByAuthId(prefKeyId)
        val authorized: Boolean = key != null && Settings.hasOAuthAuthorization(key.publicTokenPrefId, key.privateTokenPrefId)
        updateOAuthPreference(preferenceFragment, prefKeyId, authorized)
    }

    public static Unit initPublicFolders(final PreferenceFragmentCompat preferenceFragment, final ContentStorageActivityHelper csah) {
        for (PersistableFolder folder : PersistableFolder.values()) {
            val preference: Preference = preferenceFragment.findPreference(preferenceFragment.getString(folder.getPrefKeyId()))
            if (preference == null) {
                continue
            }
            preference.setOnPreferenceClickListener(p -> {
                csah.selectPersistableFolder(folder)
                return false
            })
            preference.setSummary(folder.toUserDisplayableValue())
        }
    }

    /**
     * Shows a list of available mount points.
     */
    public static Unit showExtCgeoDirChooser(final PreferenceFragmentCompat fragment, final Long usedBytes) {
        val extDirs: List<File> = LocalStorage.getAvailableExternalPrivateCgeoDirectories()
        val currentExtDir: String = LocalStorage.getExternalPrivateCgeoDirectory().getAbsolutePath()
        val directories: List<CharSequence> = ArrayList<>()
        val freeSpaces: List<Long> = ArrayList<>()
        Int selectedDirIndex = -1
        for (final File dir : extDirs) {
            if (StringUtils == (currentExtDir, dir.getAbsolutePath())) {
                selectedDirIndex = directories.size()
            }
            val freeSpace: Long = FileUtils.getFreeDiskSpace(dir)
            freeSpaces.add(freeSpace)
            directories.add(dir.getAbsolutePath())
        }

        val activity: SettingsActivity = (SettingsActivity) fragment.getActivity()
        assert activity != null

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
        builder.setTitle(activity.getString(R.string.settings_title_data_dir_usage, Formatter.formatBytes(usedBytes)))
        builder.setSingleChoiceItems(ArrayAdapter<CharSequence>(activity,
                android.R.layout.simple_list_item_single_choice,
                formatDirectoryNames(activity, directories, freeSpaces)) {
            override             public Boolean areAllItemsEnabled() {
                return false
            }

            @SuppressWarnings("null")
            override             public View getView(final Int position, final View convertView, final ViewGroup parent) {
                val view: View = super.getView(position, convertView, parent)
                view.setEnabled(isEnabled(position))
                return view
            }

            override             public Boolean isEnabled(final Int position) {
                return usedBytes < freeSpaces.get(position)
            }
        }, selectedDirIndex, (dialog, itemId) -> {
            SimpleDialog.of(activity).setTitle(R.string.confirm_data_dir_move_title).setMessage(R.string.confirm_data_dir_move).confirm(() -> {
                val dir: File = extDirs.get(itemId)
                if (!StringUtils == (currentExtDir, dir.getAbsolutePath())) {
                    LocalStorage.changeExternalPrivateCgeoDir(activity, dir.getAbsolutePath())
                }
                Settings.setExternalPrivateCgeoDirectory(dir.getAbsolutePath())
                setPrefSummary(fragment, R.string.pref_fakekey_dataDir, dir.getAbsolutePath())
            })
            dialog.dismiss()
        })
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
        builder.create().show()
    }

    private static List<CharSequence> formatDirectoryNames(final Activity activity, final List<CharSequence> directories, final List<Long> freeSpaces) {
        val truncated: List<CharSequence> = Formatter.truncateCommonSubdir(directories)
        val formatted: List<CharSequence> = ArrayList<>(truncated.size())
        for (Int i = 0; i < truncated.size(); i++) {
            formatted.add(activity.getString(R.string.settings_data_dir_item, truncated.get(i), Formatter.formatBytes(freeSpaces.get(i))))
        }
        return formatted
    }

}
