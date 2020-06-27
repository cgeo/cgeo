package cgeo.geocaching.utils;

import android.content.SharedPreferences;

import org.xmlpull.v1.XmlPullParserException;

public class SettingsUtils {

    public enum SettingsType {
        TYPE_STRING     ("string"),
        TYPE_BOOLEAN    ("boolean"),
        TYPE_INTEGER    ("integer"),
        TYPE_LONG       ("long"),
        TYPE_FLOAT      ("float"),
        TYPE_UNKNOWN    ("unknown");

        private final String id;

        SettingsType(final String id) {
            this.id = id;
        }

        public String getId() {
            return id;
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
}
