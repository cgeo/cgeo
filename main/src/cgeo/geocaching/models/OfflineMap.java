package cgeo.geocaching.models;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.downloader.AbstractDownloader;
import cgeo.geocaching.downloader.BRouterTileDownloader;
import cgeo.geocaching.downloader.CompanionFileUtils;
import cgeo.geocaching.downloader.MapDownloaderFreizeitkarte;
import cgeo.geocaching.downloader.MapDownloaderFreizeitkarteThemes;
import cgeo.geocaching.downloader.MapDownloaderMapsforge;
import cgeo.geocaching.downloader.MapDownloaderOpenAndroMaps;
import cgeo.geocaching.downloader.MapDownloaderOpenAndroMapsThemes;
import cgeo.geocaching.utils.CalendarUtils;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;

public class OfflineMap {

    private final String name;
    private final Uri uri;
    private final boolean isDir;
    private final long dateInfo;
    private final String sizeInfo;
    private String addInfo;
    private final OfflineMapType type;

    public OfflineMap(final String name, final Uri uri, final boolean isDir, final String dateISO, final String sizeInfo, final OfflineMapType type) {
        this.name = CompanionFileUtils.getDisplayName(name);
        this.uri = uri;
        this.isDir = isDir;
        this.sizeInfo = sizeInfo;
        this.addInfo = "";
        this.dateInfo = CalendarUtils.parseYearMonthDay(dateISO);
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Uri getUri() {
        return uri;
    }

    public boolean getIsDir() {
        return isDir;
    }

    public String getDateInfoAsString() {
        return CalendarUtils.yearMonthDay(dateInfo);
    }

    public long getDateInfo() {
        return dateInfo;
    }

    public String getSizeInfo() {
        return sizeInfo;
    }

    public void setAddInfo(final String addInfo) {
        this.addInfo = addInfo;
    }

    public String getAddInfo() {
        return addInfo;
    }

    public OfflineMapType getType() {
        return type;
    }

    public String getTypeAsString() {
        return OfflineMapType.getInstance(type.id).mapSourceName;
    }

    public enum OfflineMapType {
        // id values must not be changed as they are referenced in the database & download companion files
        MAP_DOWNLOAD_TYPE_MAPSFORGE(1),
        MAP_DOWNLOAD_TYPE_OPENANDROMAPS(2),
        MAP_DOWNLOAD_TYPE_OPENANDROMAPS_THEMES(3),
        MAP_DOWNLOAD_TYPE_FREIZEITKARTE(4),
        MAP_DOWNLOAD_TYPE_FREIZEITKARTE_THEMES(5),

        DOWNLOAD_TYPE_BROUTER_TILES(90);

        public final int id;
        public static final int DEFAULT = MAP_DOWNLOAD_TYPE_MAPSFORGE.id;
        private static final ArrayList<OfflineMapTypeDescriptor> offlineMapTypes = new ArrayList<>();
        private static final ArrayList<OfflineMapTypeDescriptor> downloadTypes = new ArrayList<>();

        OfflineMapType(final int id) {
            this.id = id;
        }

        public static ArrayList<OfflineMapTypeDescriptor> getOfflineMapTypes() {
            buildOfflineMapTypesList();
            return offlineMapTypes;
        }

        @Nullable
        public static AbstractDownloader getInstance(final int typeId) {
            buildOfflineMapTypesList();
            for (OfflineMapTypeDescriptor descriptor : downloadTypes) {
                if (descriptor.type.id == typeId) {
                    return descriptor.instance;
                }
            }
            return null;
        }

        private static void buildOfflineMapTypesList() {
            if (offlineMapTypes.size() == 0) {
                // only those entries which should be visible in the offline maps download selector
                offlineMapTypes.add(new OfflineMapTypeDescriptor(MAP_DOWNLOAD_TYPE_MAPSFORGE, MapDownloaderMapsforge.getInstance(), R.string.mapserver_mapsforge_name));
                offlineMapTypes.add(new OfflineMapTypeDescriptor(MAP_DOWNLOAD_TYPE_OPENANDROMAPS, MapDownloaderOpenAndroMaps.getInstance(), R.string.mapserver_openandromaps_name));
                offlineMapTypes.add(new OfflineMapTypeDescriptor(MAP_DOWNLOAD_TYPE_OPENANDROMAPS_THEMES, MapDownloaderOpenAndroMapsThemes.getInstance(), R.string.mapserver_openandromaps_themes_name));
                offlineMapTypes.add(new OfflineMapTypeDescriptor(MAP_DOWNLOAD_TYPE_FREIZEITKARTE, MapDownloaderFreizeitkarte.getInstance(), R.string.mapserver_freizeitkarte_name));
                offlineMapTypes.add(new OfflineMapTypeDescriptor(MAP_DOWNLOAD_TYPE_FREIZEITKARTE_THEMES, MapDownloaderFreizeitkarteThemes.getInstance(), R.string.mapserver_freizeitkarte_themes_name));

                // all other download types
                downloadTypes.addAll(offlineMapTypes);
                downloadTypes.add(new OfflineMapTypeDescriptor(DOWNLOAD_TYPE_BROUTER_TILES, BRouterTileDownloader.getInstance(), R.string.brouter_name));
            }
        }

        public static OfflineMapTypeDescriptor fromTypeId(final int id) {
            buildOfflineMapTypesList();
            for (OfflineMapTypeDescriptor descriptor : downloadTypes) {
                if (descriptor.type.id == id) {
                    return descriptor;
                }
            }
            return null;
        }
    }

    public static class OfflineMapTypeDescriptor {
        public final OfflineMapType type;
        public final AbstractDownloader instance;
        public final int name;

        @NonNull
        @Override
        public String toString() {
            return CgeoApplication.getInstance().getString(name);
        }

        OfflineMapTypeDescriptor(final OfflineMapType type, final AbstractDownloader instance, final @StringRes int name) {
            this.type = type;
            this.instance = instance;
            this.name = name;
        }
    }
}
