package cgeo.geocaching.storage.extension;

import cgeo.geocaching.R;
import cgeo.geocaching.storage.DataStore;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import org.apache.commons.lang3.StringUtils;

public class OneTimeDialogs extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_ONE_TIME_DIALOGS;

    private OneTimeDialogs(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    /**
     * list of all one time dialogs - new dialogs must be defined here
     */
    public enum DialogType {
        // names must not be changed, as there are database entries depending on it
        // title and text must be set when using the Dialogs.basicOneTimeMessage() function

        DATABASE_CONFIRM_OVERWRITE(null, null, DefaultBehavior.SHOW_ALWAYS, 0, 0),
        MAP_QUICK_SETTINGS(R.string.settings_information, R.string.quick_settings_info, DefaultBehavior.SHOW_ONLY_AFTER_UPGRADE, 0, R.drawable.ic_info_blue),
        MISSING_UNICODE_CHARACTERS(R.string.select_icon, R.string.onetime_missing_unicode_info, DefaultBehavior.SHOW_ALWAYS, 0, R.drawable.ic_info_blue),
        MAP_THEME_FIX_SLOWNESS(R.string.onetime_mapthemefixslow_title, R.string.onetime_mapthemefixslow_message, DefaultBehavior.SHOW_ALWAYS, R.string.faq_url_settings_themes, R.drawable.ic_info_blue),
        MAP_AUTOROTATION_DISABLE(R.string.map_autorotation, R.string.map_autorotation_disable, DefaultBehavior.SHOW_ALWAYS, 0, 0),
        MAP_LIVE_DISABLED(R.string.map_live_disabled, R.string.map_live_disabled_hint, DefaultBehavior.SHOW_ALWAYS, 0, R.drawable.ic_info_blue),
        ROUTE_OPTIMIZATION(R.string.route_optimization, R.string.route_optimization_info, DefaultBehavior.SHOW_ALWAYS, 0, 0),
        NOTIFICATION_PERMISSION(R.string.changed_permissions_title, R.string.changed_permissions_info, DefaultBehavior.SHOW_ONLY_AFTER_UPGRADE, 0, R.drawable.ic_info_blue),
        GOTO_DEPRECATION_NOTICE(R.string.goto_targets_deprecation_title, R.string.goto_targets_deprecation_notice, DefaultBehavior.SHOW_ALWAYS, 0, R.drawable.ic_info_blue),
        WHERIGO_PLAYER_SHORTCUTS(R.string.wherigo_otm_shortcuts_title, R.string.wherigo_otm_shortcuts_message, DefaultBehavior.SHOW_ALWAYS, 0, R.drawable.ic_info_blue),
        DELETE_CACHES_USER_DATA_WARNING(R.string.command_delete_caches_progress, R.string.caches_warning_delete_all_caches, DefaultBehavior.SHOW_ALWAYS, 0, 0),
        REMOVE_CACHES_FROM_LIST_WARNING(R.string.command_remove_caches_from_list_progress, R.string.caches_warning_remove_caches_from_single_list, DefaultBehavior.SHOW_ALWAYS, 0, 0);


        public final Integer messageTitle;
        public final Integer messageText;
        public final DefaultBehavior defaultBehavior;
        public final int moreInfoURLResId;
        public final int iconResId;

        DialogType(final Integer messageTitle, final Integer messageText, final DefaultBehavior defaultBehavior, @StringRes final int moreInfoURLResId, @DrawableRes final int iconResId) {
            this.messageTitle = messageTitle;
            this.messageText = messageText;
            this.defaultBehavior = defaultBehavior;
            this.moreInfoURLResId = moreInfoURLResId;
            this.iconResId = iconResId;
        }
    }

    /**
     * defines, in which situations the one time dialog should be shown by default, until "don't show again" is checked
     */
    public enum DefaultBehavior {
        SHOW_ALWAYS,
        SHOW_ONLY_AT_FRESH_INSTALL,
        SHOW_ONLY_AFTER_UPGRADE,
        SHOW_NEVER
    }

    /**
     * possible show states which are stored in the database
     */
    public enum DialogStatus {
        // values for id must not be changed, as there are database entries depending on it
        NONE(0),
        DIALOG_SHOW(1),
        DIALOG_HIDE(2);

        public final int id;

        DialogStatus(final int id) {
            this.id = id;
        }
    }

    private static DialogStatus getStatusById(final int id) {
        for (DialogStatus e : DialogStatus.values()) {
            if (id == e.id) {
                return e;
            }
        }
        return null;
    }

    /**
     * returns whether the one time dialog should be shown
     */
    public static boolean showDialog(final DialogType dialogType) {
        DialogStatus showStatus;

        if (dialogType.defaultBehavior == DefaultBehavior.SHOW_ALWAYS || dialogType.defaultBehavior == DefaultBehavior.SHOW_ONLY_AFTER_UPGRADE) {
            // on a fresh install, the default show behavior will be overwritten (see initializeOnFreshInstall())
            showStatus = DialogStatus.DIALOG_SHOW;
        } else {
            showStatus = DialogStatus.DIALOG_HIDE;
        }

        final DataStore.DBExtension temp = load(type, dialogType.name());
        if (null != temp) {
            final OneTimeDialogs dialog = new OneTimeDialogs(temp);
            final DialogStatus status = getStatusById((int) dialog.getLong1());
            if (status != DialogStatus.NONE) {
                showStatus = status;
            }
        }

        return showStatus == DialogStatus.DIALOG_SHOW;
    }

    /**
     * sets the show state for a specific dialog
     */
    public static void setStatus(final DialogType dialogType, final DialogStatus status) {
        setStatus(dialogType, status, DialogStatus.NONE);
    }

    /**
     * sets the current show state and the next show state, which will be applied at the next app restart
     */
    public static void setStatus(final DialogType dialogType, final DialogStatus currentStatus, final DialogStatus nextStatus) {
        removeAll(type, dialogType.name());
        add(type, dialogType.name(), currentStatus.id, nextStatus.id, 0, 0, "", "", "", "");
    }

    /**
     * switches to the next show state
     * this is called ones after app start, as basicOneTimeMessages are snoozing if "don't show again" was not checked
     */
    public static void nextStatus() {

        for (DialogType key : DialogType.values()) {
            final DataStore.DBExtension temp = load(type, key.name());

            if (null != temp) {
                final OneTimeDialogs dialog = new OneTimeDialogs(temp);

                if (getStatusById((int) dialog.getLong2()) != DialogStatus.NONE) {
                    setStatus(key, getStatusById((int) dialog.getLong2()));
                }
            }
        }
    }

    public static void initializeOnFreshInstall() {
        for (DialogType key : DialogType.values()) {
            if (key.defaultBehavior == DefaultBehavior.SHOW_ONLY_AFTER_UPGRADE) {
                setStatus(key, DialogStatus.DIALOG_HIDE);
            } else if (key.defaultBehavior == DefaultBehavior.SHOW_ONLY_AT_FRESH_INSTALL) {
                setStatus(key, DialogStatus.DIALOG_SHOW);
            }
        }
    }

    public static void resetAll() {
        for (DialogType key : DialogType.values()) {
            removeAll(type, key.name());
        }
        initializeOnFreshInstall();
    }

    /**
     * possible chosen actions for dialogs with "don't ask again" functionality.
     * The chosen action is stored in string1 of the DB entry.
     */
    public enum ChosenAction {
        OK,
        NEUTRAL,
        CANCEL
    }

    /**
     * returns the chosen action for a specific dialog, or defaultAction if none has been stored yet
     */
    public static ChosenAction getChosenAction(final DialogType dialogType, final ChosenAction defaultAction) {
        final DataStore.DBExtension temp = load(type, dialogType.name());
        if (temp != null) {
            final String stored = new OneTimeDialogs(temp).getString1();
            if (StringUtils.isNotBlank(stored)) {
                try {
                    return ChosenAction.valueOf(stored);
                } catch (final IllegalArgumentException e) {
                    // stored value is not a valid ChosenAction, fall through to default
                }
            }
        }
        return defaultAction;
    }

    /**
     * Sets the chosen action for a specific dialog and simultaneously sets the dialog status to DIALOG_HIDE.
     * Both operations are wrapped in a single database transaction to guarantee atomicity.
     */
    public static void setChosenAction(final DialogType dialogType, final ChosenAction action) {
        replaceAll(type, dialogType.name(), DialogStatus.DIALOG_HIDE.id, DialogStatus.NONE.id, 0, 0, action.name(), "", "", "");
    }
}
