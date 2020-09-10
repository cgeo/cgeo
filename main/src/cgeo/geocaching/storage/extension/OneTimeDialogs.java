package cgeo.geocaching.storage.extension;

import cgeo.geocaching.R;
import cgeo.geocaching.storage.DataStore;


public class OneTimeDialogs extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_ONE_TIME_DIALOGS;

    private OneTimeDialogs(final DataStore.DBExtension copyFrom) {
        this.id = copyFrom.getId();
        this.key = copyFrom.getKey();
        this.long1 = copyFrom.getLong1();
        this.long2 = copyFrom.getLong2();
    }

    public enum DialogType {
        // names must not be changed, as there are database entries depending on it

        EXPLAIN_OFFLINE_FOUND_COUNTER(R.string.settings_information, R.string.info_feature_offline_counter);

        public int messageTitle;
        public int messageText;

        DialogType(final int messageTitle, final int messageText) {
            this.messageTitle = messageTitle;
            this.messageText = messageText;
        }
    }

    public enum DialogStatus {
        // values for id must not be changed, as there are database entries depending on it
        NONE(0),
        DIALOG_SHOW(1),
        DIALOG_HIDE (2);

        public int id;

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

    public static DialogStatus getStatus(final DialogType dialogType, final DialogStatus defaultStatus) {
        final DataStore.DBExtension temp = load(type, dialogType.name());
        if (null == temp) {
            return defaultStatus;
        }
        final OneTimeDialogs dialog = new OneTimeDialogs(temp);
        final DialogStatus status = getStatusById((int) dialog.getLong1());
        return status == DialogStatus.NONE ? defaultStatus : status;
    }

    public static void setStatus(final DialogType dialogType, final DialogStatus status) {
        removeAll(type, dialogType.name());
        add(type, dialogType.name(), status.id, 0, "", "");
    }

    public static void setStatus(final DialogType dialogType, final DialogStatus currentStatus, final DialogStatus nextStatus) {
        removeAll(type, dialogType.name());
        add(type, dialogType.name(), currentStatus.id, nextStatus.id, "", "");
    }

    public static void nextStatus() {

        for (DialogType key : DialogType.values()) {
            final DataStore.DBExtension temp = load(type, key.name());

            if (null != temp) {
                final OneTimeDialogs dialog = new OneTimeDialogs(temp);

                if (getStatusById((int) dialog.getLong2()) != DialogStatus.NONE) {
                    removeAll(type, key.name());
                    add(type, key.name(), dialog.getLong2(), 0, "", "");
                }
            }
        }
    }
}
