package cgeo.geocaching.wherigo;


import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.Formatter;

import androidx.annotation.NonNull;

import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;


public class WherigoSavegameInfo {

    public static final String AUTOSAVE_NAME = "autosave";

    public static final Comparator<WherigoSavegameInfo> DEFAULT_COMPARATOR = CommonUtils.getNullHandlingComparator(new Comparator<WherigoSavegameInfo>() {
        @Override
        public int compare(final WherigoSavegameInfo o1, final WherigoSavegameInfo o2) {
            if (Objects.equals(o1.name, o2.name)) {
                return Long.compare((o1.saveDate == null ? -1 : o1.saveDate.getTime()), (o2.saveDate == null ? -1 : o2.saveDate.getTime()));
            }
            if (o1.name == null || o2.name == null) {
                return o1.name == null ? -1 : 1;
            }
            if (AUTOSAVE_NAME.equals(o1.name) || AUTOSAVE_NAME.equals(o2.name)) {
                return AUTOSAVE_NAME.equals(o1.name) ? -1 : 1;
            }
            final int i1 = o1.getNameAsNumber();
            final int i2 = o2.getNameAsNumber();
            if (i1 != i2) {
                return Integer.compare(i1 <= 0 ? Integer.MAX_VALUE : i1, i2 <= 0 ? Integer.MAX_VALUE : i2);
            }
            return o1.name.compareTo(o2.name);
        }
    }, true);

    public final ContentStorage.FileInformation fileInfo;
    public final String name;
    public final Date saveDate;

    public WherigoSavegameInfo(final ContentStorage.FileInformation fileInfo, final String name, final Date saveDate) {
        this.fileInfo = fileInfo;
        this.name = name;
        this.saveDate = saveDate;
    }

    public int getNameAsNumber() {
        if  (name == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(name));
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public String getUserDisplayableName() {
        if (StringUtils.isBlank(name)) {
            return "<New Game>";
        }
        if (AUTOSAVE_NAME.equals(name)) {
            return "Autosave-File";
        }

        final int number = getNameAsNumber();
        if (number > 0) {
            return "Slot " + number;
        }

        return name;
    }

    public String getUserDisplayableSaveDate() {
        if (saveDate == null) {
            return "<empty slot>";
        }
        return Formatter.formatDateTime(saveDate.getTime());
    }

    @NonNull
    @Override
    public String toString() {
        return "Savegame: " + name + "/File:" + fileInfo;
    }

}