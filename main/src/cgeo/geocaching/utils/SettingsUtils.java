package cgeo.geocaching.utils;

import cgeo.geocaching.R;

import android.content.SharedPreferences;

import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

public class SettingsUtils {

    public enum SettingsType {
        TYPE_STRING     ("string", ""),
        TYPE_BOOLEAN    ("boolean", "false"),
        TYPE_INTEGER    ("integer", "0"),
        TYPE_INTEGER_COMPATIBILITY ("int", "0"),     // for reading compatibility with early backups
        TYPE_LONG       ("long", "0"),
        TYPE_FLOAT      ("float", "0.0"),
        TYPE_UNKNOWN    ("unknown", "");

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

}
