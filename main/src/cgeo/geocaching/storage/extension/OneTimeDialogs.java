package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;


public class OneTimeDialogs extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_ONE_TIME_DIALOGS;

    private OneTimeDialogs(final DataStore.DBExtension copyFrom) {
        this.id = copyFrom.getId();
        this.key = copyFrom.getKey();
        this.long1 = copyFrom.getLong1();
    }

    public enum DialogType {
        // names must not be changed, as there are database entries depending on it

        EXPLAIN_OFFLINE_FOUND_COUNTER;
    }

    public enum DialogStatus {
        // values for id must not be changed, as there are database entries depending on it
        DIALOG_SHOW(0),
        DIALOG_HIDE (1);

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
        return getStatusById((int) dialog.getLong1());
    }

    public static void setStatus(final DialogType dialogType, final DialogStatus status) {
        removeAll(type, dialogType.name());
        add(type, dialogType.name(), status.id, 0, "", "");
    }
}
