package cgeo.geocaching.wherigo;

import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.IOException;

import cz.matejcik.openwig.formats.CartridgeFile;

public class WherigoCartridgeInfo {
    public final ContentStorage.FileInformation fileInfo;
    public final String cguid;
    public final CartridgeFile closedCartridgeFile;
    public final Bitmap icon;

    private WherigoCartridgeInfo(final ContentStorage.FileInformation fileInfo, final String cguid, final CartridgeFile closedCartridgeFile, final Bitmap icon) {
        this.fileInfo = fileInfo;
        this.cguid = cguid;
        this.closedCartridgeFile = closedCartridgeFile;
        this.icon = icon;
    }

    public static WherigoCartridgeInfo get(final ContentStorage.FileInformation file, final boolean loadCartridgeFile, final boolean loadIcon) {
        if (file == null) {
            return null;
        }
        final String guid = getGuid(file);
        final CartridgeFile cf = loadCartridgeFile ? safeReadCartridge(file.uri) : null;
        final Bitmap icon = cf != null && loadIcon ? getCartridgeIcon(cf) : null;
        WherigoUtils.closeCartridgeQuietly(cf);
        return new WherigoCartridgeInfo(file, guid, cf, icon);
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

    private static Bitmap getCartridgeIcon(final CartridgeFile file) {
        if (file == null) {
            return null;
        }
        try {
            final byte[] iconData = file.getFile(file.iconId);
            return BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
        } catch (Exception e) {
            return null;
        }
    }
}
