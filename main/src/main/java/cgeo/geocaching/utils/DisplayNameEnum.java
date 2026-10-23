package cgeo.geocaching.utils;

/** Implemented by enums which can be selected in a list preference, see {@link PreferenceUtils#configEnumListPreference} */
public interface DisplayNameEnum {

    /** user-visible, localized text of this enum value */
    String getDisplayName();
}
