package cgeo.geocaching.unifiedmap.mapsforge;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.Log;

import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileException;

public class MapsforgeFileUtils {

    private static final String OFFLINE_MAP_DEFAULT_ATTRIBUTION = "---";
    private static final Map<Uri, String> OFFLINE_MAP_ATTRIBUTIONS = new HashMap<>();

    private MapsforgeFileUtils() {
        // utility class
    }

    /**
     * checks whether the given Uri is a valid map file.
     * This method uses cached results from previous checks
     */
    public static boolean isValidMapFile(final Uri filePath) {
        return getAttributionIfValidFor(filePath) != null;
    }

    private static String getAttributionIfValidFor(final Uri filePath) {

        if (OFFLINE_MAP_ATTRIBUTIONS.containsKey(filePath)) {
            return OFFLINE_MAP_ATTRIBUTIONS.get(filePath);
        }
        final InputStream mapStream = createMapFileInputStream(filePath);
        if (mapStream == null) {
            //do NOT put this in cache, might be a temporary access problem
            return null;
        }

        OFFLINE_MAP_ATTRIBUTIONS.put(filePath, readAttributionFromMapFileIfValid(String.valueOf(filePath), mapStream));
        return OFFLINE_MAP_ATTRIBUTIONS.get(filePath);
    }

    private static InputStream createMapFileInputStream(final Uri mapUri) {
        if (mapUri == null) {
            return null;
        }
        return ContentStorage.get().openForRead(mapUri, true);
    }

    /**
     * Tries to open given uri as a mapfile.
     * If mapfile is invalid in any way (not available, not readable, wrong version, ...), then null is returned.
     * If mapfile is valid, then its attribution is read and returned (or a default attribution value in case attribution is null)
     */
    @Nullable
    private static String readAttributionFromMapFileIfValid(final String mapFileCtx, final InputStream mapStream) {

        MapFile mapFile = null;
        try {
            mapFile = createMapFile(mapFileCtx, mapStream);
            if (mapFile != null && mapFile.getMapFileInfo() != null && mapFile.getMapFileInfo().fileVersion <= 5) {
                if (StringUtils.isNotBlank(mapFile.getMapFileInfo().comment)) {
                    return mapFile.getMapFileInfo().comment;
                }
                if (StringUtils.isNotBlank(mapFile.getMapFileInfo().createdBy)) {
                    return mapFile.getMapFileInfo().createdBy;
                }
                //map file is valid but has no attribution -> return default value
                return OFFLINE_MAP_DEFAULT_ATTRIBUTION;
            }
        } catch (MapFileException ex) {
            Log.w(String.format("Exception reading mapfile '%s'", mapFileCtx), ex);
        } finally {
            closeMapFileQuietly(mapFile);
        }
        return null;
    }

    private static MapFile createMapFile(final String mapFileCtx, final InputStream fis) {
        if (fis != null) {
            try {
                return new MapFile((FileInputStream) fis, 0, Settings.getMapLanguage());
            } catch (MapFileException mfe) {
                Log.e("Problem opening map file '" + mapFileCtx + "'", mfe);
            }
        }
        return null;
    }

    private static void closeMapFileQuietly(final MapFile mapFile) {
        if (mapFile != null) {
            mapFile.close();
        }
    }

}
