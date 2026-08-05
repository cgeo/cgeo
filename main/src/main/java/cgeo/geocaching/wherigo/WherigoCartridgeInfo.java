package cgeo.geocaching.wherigo;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class WherigoCartridgeInfo {
    private final ContentStorage.FileInformation fileInfo;
    private final String cguid;
    private final boolean readIcon;
    private final boolean readSplash;

    private CartridgeFile closedCartridgeFile;
    private byte[] iconData;
    private byte[] splashData;

    public WherigoCartridgeInfo(final ContentStorage.FileInformation fileInfo, final boolean readIcon, final boolean readSplash) {
        this.fileInfo = fileInfo;
        this.cguid = getGuid(fileInfo);
        this.readIcon = readIcon;
        this.readSplash = readSplash;
    }

    public ContentStorage.FileInformation getFileInfo() {
        return fileInfo;
    }

    @NonNull
    public Geopoint getCartridgeLocation() {
        return closedCartridgeFile == null ? Geopoint.ZERO : new Geopoint(closedCartridgeFile.latitude, closedCartridgeFile.longitude);
    }

    public String getCGuid() {
        return cguid;
    }

    public CartridgeFile getCartridgeFile() {
        ensureCartridgeData(false, false);
        return closedCartridgeFile;
    }

    public byte[] getIconData() {
        ensureCartridgeData(true, false);
        return iconData == null || iconData.length == 0 ? null : iconData;
    }

    public byte[] getSplashData() {
        ensureCartridgeData(false, true);
        return splashData == null || splashData.length == 0 ? null : splashData;
    }

    private void ensureCartridgeData(final boolean forceIcon, final boolean forceSplash) {
        if (closedCartridgeFile != null && (iconData != null || !forceIcon) && (splashData != null || !forceSplash)) {
            return;
        }

        closedCartridgeFile = safeReadCartridge(fileInfo.uri);
        if (closedCartridgeFile == null) {
            this.iconData = new byte[0];
            this.splashData = new byte[0];
            return;
        }
        try {
            if (forceIcon || readIcon) {
                this.iconData = closedCartridgeFile.getFile(closedCartridgeFile.iconId);
            }
            if (forceSplash || readSplash) {
                this.splashData = closedCartridgeFile.getFile(closedCartridgeFile.splashId);
            }
        } catch (Exception e) {
            Log.w("Problem reading data from cartridgeFile " + this, e);
            if (this.iconData != null) {
                this.iconData = new byte[0];
            }
            if (this.splashData != null) {
                this.splashData = new byte[0];
            }
        }
        WherigoUtils.closeCartridgeQuietly(closedCartridgeFile);
    }

    @Nullable
    private static String getGuid(final ContentStorage.FileInformation fileInfo) {
        if (fileInfo == null || fileInfo.name == null || !fileInfo.name.endsWith(".gwc")) {
            return null;
        }
        final String guid = fileInfo.name.substring(0, fileInfo.name.length() - 4);
        final int idx = guid.indexOf("_");
        return idx <= 0 ? guid : guid.substring(0, idx);
    }

    private static CartridgeFile safeReadCartridge(final Uri uri) {
        try {
            return WherigoUtils.readCartridge(uri);
        } catch (IOException ie) {
            Log.d("Couldn't read Cartridge '" + uri + "'", ie);
            return null;
        }
    }

    @NonNull
    public String getName() {
        final CartridgeFile file = getCartridgeFile();
        if (file != null && !StringUtils.isBlank(file.name)) {
            return file.name;
        }
        if (fileInfo != null && !StringUtils.isBlank(fileInfo.name)) {
            return fileInfo.name;
        }
        return "-";
    }

    @NonNull
    @Override
    public String toString() {
        return "CGuid:" + cguid + ", File:[" + fileInfo + "]";
    }

    public static Folder getCartridgeFolder() {
        return PersistableFolder.WHERIGO.getFolder();
    }

    public static WherigoCartridgeInfo getCartridgeForCGuid(final String cguid) {
        final List<WherigoCartridgeInfo> cartridges = getAvailableCartridges(guid -> Objects.equals(cguid, guid));
        return cartridges.isEmpty() ? null : cartridges.get(0);
    }

    public static List<WherigoCartridgeInfo> getAvailableCartridges(final Predicate<String> guidFilter) {
        final List<ContentStorage.FileInformation> candidates = ContentStorage.get().list(getCartridgeFolder()).stream()
                .filter(fi -> fi.name.endsWith(".gwc")).collect(Collectors.toList());
        final List<WherigoCartridgeInfo> result = new ArrayList<>(candidates.size());
        for (ContentStorage.FileInformation candidate : candidates) {
            if (guidFilter != null && !guidFilter.test(getGuid(candidate))) {
                continue;
            }
            result.add(new WherigoCartridgeInfo(candidate, true, false));
        }
        return result;
    }

}
