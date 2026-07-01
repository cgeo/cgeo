package cgeo.geocaching.wherigo;


import cgeo.geocaching.R;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;


public class WherigoSavegameInfo {

    public static final String AUTOSAVE_NAME = "autosave";

    private static final Pattern GEOCODE_PATTERN = Pattern.compile("\\s((?:GC|OC)[0-9A-Z]{1,6})\\s");

    public static final Comparator<WherigoSavegameInfo> DEFAULT_COMPARATOR = CommonUtils.getNullHandlingComparator((o1, o2) -> {
        if (Objects.equals(o1.nameId, o2.nameId)) {
            return Long.compare((o1.saveDate == null ? -1 : o1.saveDate.getTime()), (o2.saveDate == null ? -1 : o2.saveDate.getTime()));
        }
        if (o1.nameId == null || o2.nameId == null) {
            return o1.nameId == null ? -1 : 1;
        }
        if (AUTOSAVE_NAME.equals(o1.nameId) || AUTOSAVE_NAME.equals(o2.nameId)) {
            return AUTOSAVE_NAME.equals(o1.nameId) ? -1 : 1;
        }
        final int i1 = o1.getNameIdAsNumber();
        final int i2 = o2.getNameIdAsNumber();
        if (i1 != i2) {
            return Integer.compare(i1 <= 0 ? Integer.MAX_VALUE : i1, i2 <= 0 ? Integer.MAX_VALUE : i2);
        }
        return o1.nameId.compareTo(o2.nameId);
    }, true);

    @NonNull public final ContentStorage.FileInformation cartridgeFileInfo;
    @Nullable public final ContentStorage.FileInformation fileInfo;
    @Nullable public final String nameId;
    @Nullable public final String nameCustom;
    @Nullable public final Date saveDate;
    @Nullable public final String geocode;

    //for a new game slot
    private WherigoSavegameInfo(@NonNull final ContentStorage.FileInformation cartridgeFileInfo) {
        this(cartridgeFileInfo, null, null);
    }

    //for a new save slot
    private WherigoSavegameInfo(@NonNull final ContentStorage.FileInformation cartridgeFileInfo, @Nullable final String nameId, @Nullable final String nameCustom) {
        this.cartridgeFileInfo = cartridgeFileInfo;
        this.fileInfo = null;
        this.nameId = nameId;
        this.nameCustom = StringUtils.isBlank(nameCustom) ? null : nameCustom.trim();
        this.saveDate = null;
        this.geocode = findGeocode(this.nameCustom);
    }

    //for existing savefiles
    private WherigoSavegameInfo(@NonNull final ContentStorage.FileInformation cartridgeFileInfo, @NonNull final ContentStorage.FileInformation fileInfo) {
        this.cartridgeFileInfo = cartridgeFileInfo;
        this.fileInfo = fileInfo;

        this.saveDate = new Date(this.fileInfo.lastModified);

        //extract nameId and nameCustom
        String name = fileInfo.name == null ? "" : fileInfo.name.trim();
        int idx = name.lastIndexOf(".");
        if (idx >= 0) {
            name = name.substring(0, idx);
        }
        final String cartridgeBaseName = getCartridgeSavefileNameBase(cartridgeFileInfo);
        if (name.startsWith(cartridgeBaseName + "-")) {
            name = name.substring(cartridgeBaseName.length() + 1);
        }

        idx = name.indexOf("-");
        this.nameId = fromFilename(idx < 0 ? name.trim() : name.substring(0, idx).trim());
        final String nameCustomCandidate = idx < 0 ? null : name.substring(idx + 1).trim();
        this.nameCustom = StringUtils.isBlank(nameCustomCandidate) ? null : fromFilename(nameCustomCandidate);
        this.geocode = findGeocode(this.nameCustom);
    }

    private static String getCartridgeSavefileNameBase(final ContentStorage.FileInformation cartridgeFileInfo) {
        String cartridgeBaseName = cartridgeFileInfo.name;
        final int idx = cartridgeBaseName.lastIndexOf(".");
        if (idx > 0) {
            cartridgeBaseName = cartridgeBaseName.substring(0, idx);
        }
        return cartridgeBaseName;
    }

    private static String toFilename(final String text) {
        return text.replaceAll("[^-a-zA-Z0-9]", "_");
    }

    private static String fromFilename(final String text) {
        return text.replaceAll("_", " ");
    }

    public String getSavefileName() {
        final String fileName = toFilename(nameId == null ? "" : nameId.trim()) + (StringUtils.isBlank(nameCustom) ? "" : "-" + toFilename(nameCustom.trim()));
        return getCartridgeSavefileNameBase(cartridgeFileInfo) + "-" + fileName + ".sav";
    }

    public static WherigoSavegameInfo getNewSavefile(final ContentStorage.FileInformation cartridgeFileInfo, final String nameId, final String nameCustom) {
        return new WherigoSavegameInfo(cartridgeFileInfo, nameId, nameCustom);
    }

    public static WherigoSavegameInfo getAutoSavefile(final ContentStorage.FileInformation cartridgeFileInfo) {
        return new WherigoSavegameInfo(cartridgeFileInfo, AUTOSAVE_NAME, null);
    }

    public static List<WherigoSavegameInfo> getLoadableSavegames(final ContentStorage.FileInformation cartridgeFileInfo) {
        final List<WherigoSavegameInfo> list = new ArrayList<>(getAvailableSaveFiles(cartridgeFileInfo));
        list.add(new WherigoSavegameInfo(cartridgeFileInfo)); // New Game
        list.sort(WherigoSavegameInfo.DEFAULT_COMPARATOR);
        return list;
    }

    public static List<WherigoSavegameInfo> getAllSaveFiles(final ContentStorage.FileInformation cartridgeFileInfo) {
        final List<WherigoSavegameInfo> list = new ArrayList<>(getAvailableSaveFiles(cartridgeFileInfo));
        list.sort(WherigoSavegameInfo.DEFAULT_COMPARATOR);
        return list;
    }

    public static List<WherigoSavegameInfo> getSavegameSlots(final ContentStorage.FileInformation cartridgeFileInfo) {
        final int[] maxExistingSlot = new int[] {0};
        final List<WherigoSavegameInfo> list = getAvailableSaveFiles(cartridgeFileInfo).stream()
                .filter(si -> !WherigoSavegameInfo.AUTOSAVE_NAME.equals(si.nameId)) // remove autosave
                .map(si -> {
                    maxExistingSlot[0] = Math.max(maxExistingSlot[0], si.getNameIdAsNumber());
                    return si;
                }).collect(Collectors.toCollection(ArrayList::new));
        //add one empty slot
        list.add(new WherigoSavegameInfo(cartridgeFileInfo, "" + (maxExistingSlot[0] + 1), null)); // new slot
        list.sort(WherigoSavegameInfo.DEFAULT_COMPARATOR);
        return list;
    }

    private static List<WherigoSavegameInfo> getAvailableSaveFiles(final ContentStorage.FileInformation cartridgeFileInfo) {
        if (cartridgeFileInfo == null || cartridgeFileInfo.parentFolder == null) {
            return Collections.emptyList();
        }
        final String cartridgeNameBase = getCartridgeSavefileNameBase(cartridgeFileInfo);
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(cartridgeFileInfo.parentFolder);
        return files.stream()
            .filter(fi -> fi.name.startsWith(cartridgeNameBase + "-") && fi.name.endsWith(".sav"))
            .map(fi -> new WherigoSavegameInfo(cartridgeFileInfo, fi))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private int getNameIdAsNumber() {
        if  (nameId == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(nameId));
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public boolean isNewGame() {
        return nameId == null;
    }

    public boolean isExistingSavefile() {
        return fileInfo != null;
    }

    public boolean isAutosave() {
        return AUTOSAVE_NAME.equals(nameId);
    }

    public boolean isDeletableByUser() {
        return fileInfo != null && fileInfo.uri != null && nameId != null  && !isAutosave();
    }

    public void delete() {
        if (fileInfo != null && fileInfo.uri != null) {
            ContentStorage.get().delete(fileInfo.uri);
        }
    }

    private static String findGeocode(final String text) {
        if (text == null) {
            return null;
        }
        final Matcher m = GEOCODE_PATTERN.matcher(" " + text + " ");
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    @Nullable
    public String getGeocode() {
        return geocode;
    }

    public String getUserDisplayableNameId() {
        if (StringUtils.isBlank(nameId)) {
            //new game
            return LocalizationUtils.getString(R.string.wherigo_savegame_name_empty);
        }
        String nameIdText = nameId;
        if (AUTOSAVE_NAME.equals(nameId)) {
            nameIdText = LocalizationUtils.getString(R.string.wherigo_savegame_name_autosave);
        } else {
            final int number = getNameIdAsNumber();
            if (number > 0) {
                nameIdText = LocalizationUtils.getString(R.string.wherigo_savegame_name_numeric, String.valueOf(number));
            }
        }
        return nameIdText;
    }

    public String getUserDisplayableName() {
        final String nameIdText = getUserDisplayableNameId();
        return StringUtils.isBlank(nameId) || StringUtils.isBlank(nameCustom) ? nameIdText : nameIdText + ": " + nameCustom;
    }

    public String getUserDisplayableSaveDate() {
        if (saveDate == null) {
            return LocalizationUtils.getString(R.string.wherigo_savegame_date_empty);
        }
        return Formatter.formatDateTime(saveDate.getTime());
    }

    public String toShortString() {
        return nameId + "-" + nameCustom + ":" + (saveDate == null ? "-" : Formatter.formatShortDateTime(saveDate.getTime()));
    }

    @NonNull
    @Override
    public String toString() {
        return "Savegame:" + nameId + "-" + nameCustom + "/File:" + fileInfo;
    }

}
