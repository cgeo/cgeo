package cgeo.geocaching.models;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.downloader.AbstractDownloader;
import cgeo.geocaching.downloader.BRouterTileDownloader;
import cgeo.geocaching.downloader.CompanionFileUtils;
import cgeo.geocaching.downloader.MapDownloaderFreizeitkarte;
import cgeo.geocaching.downloader.MapDownloaderFreizeitkarteThemes;
import cgeo.geocaching.downloader.MapDownloaderHylly;
import cgeo.geocaching.downloader.MapDownloaderHyllyThemes;
import cgeo.geocaching.downloader.MapDownloaderMapsforge;
import cgeo.geocaching.downloader.MapDownloaderOpenAndroMaps;
import cgeo.geocaching.downloader.MapDownloaderOpenAndroMapsThemes;
import cgeo.geocaching.utils.CalendarUtils;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;

public class Download {

    private final String name;
    private final Uri uri;
    private final boolean isDir;
    private final long dateInfo;
    private final String sizeInfo;
    private String addInfo;
    private final DownloadType type;

    public Download(final String name, final Uri uri, final boolean isDir, final String dateISO, final String sizeInfo, final DownloadType type) {
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

    public DownloadType getType() {
        return type;
    }

    public String getTypeAsString() {
        return DownloadType.getInstance(type.id).mapSourceName;
    }

    public enum DownloadType {
        // id values must not be changed as they are referenced in the database & download companion files
        DOWNLOADTYPE_ALL_MAPRELATED(0),         // virtual entry
        DOWNLOADTYPE_MAP_MAPSFORGE(1),
        DOWNLOADTYPE_MAP_OPENANDROMAPS(2),
        DOWNLOADTYPE_THEME_OPENANDROMAPS(3),
        DOWNLOADTYPE_MAP_FREIZEITKARTE(4),
        DOWNLOADTYPE_THEME_FREIZEITKARTE(5),
        DOWNLOADTYPE_MAP_HYLLY(6),
        DOWNLOADTYPE_THEME_HYLLY(7),

        DOWNLOADTYPE_BROUTER_TILES(90);

        public final int id;
        public static final int DEFAULT = DOWNLOADTYPE_MAP_MAPSFORGE.id;
        private static final ArrayList<DownloadTypeDescriptor> offlineMapTypes = new ArrayList<>();
        private static final ArrayList<DownloadTypeDescriptor> downloadTypes = new ArrayList<>();

        DownloadType(final int id) {
            this.id = id;
        }

        public static ArrayList<DownloadTypeDescriptor> getOfflineMapTypes() {
            buildTypelist();
            return offlineMapTypes;
        }

        @Nullable
        public static AbstractDownloader getInstance(final int typeId) {
            buildTypelist();
            for (DownloadTypeDescriptor descriptor : downloadTypes) {
                if (descriptor.type.id == typeId) {
                    return descriptor.instance;
                }
            }
            return null;
        }

        private static void buildTypelist() {
            if (offlineMapTypes.size() == 0) {
                // only those entries which should be visible in the offline maps download selector
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_MAPSFORGE, MapDownloaderMapsforge.getInstance(), R.string.mapserver_mapsforge_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_OPENANDROMAPS, MapDownloaderOpenAndroMaps.getInstance(), R.string.mapserver_openandromaps_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_THEME_OPENANDROMAPS, MapDownloaderOpenAndroMapsThemes.getInstance(), R.string.mapserver_openandromaps_themes_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_FREIZEITKARTE, MapDownloaderFreizeitkarte.getInstance(), R.string.mapserver_freizeitkarte_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_THEME_FREIZEITKARTE, MapDownloaderFreizeitkarteThemes.getInstance(), R.string.mapserver_freizeitkarte_themes_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_HYLLY, MapDownloaderHylly.getInstance(), R.string.mapserver_hylly_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_THEME_HYLLY, MapDownloaderHyllyThemes.getInstance(), R.string.mapserver_hylly_themes_name));

                // all other download types
                downloadTypes.addAll(offlineMapTypes);
                downloadTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_BROUTER_TILES, BRouterTileDownloader.getInstance(), R.string.brouter_name));
            }
        }

        public static DownloadTypeDescriptor fromTypeId(final int id) {
            buildTypelist();
            for (DownloadTypeDescriptor descriptor : downloadTypes) {
                if (descriptor.type.id == id) {
                    return descriptor;
                }
            }
            return null;
        }
    }

    public static class DownloadTypeDescriptor {
        public final DownloadType type;
        public final AbstractDownloader instance;
        public final int name;

        @NonNull
        @Override
        public String toString() {
            return CgeoApplication.getInstance().getString(name);
        }

        DownloadTypeDescriptor(final DownloadType type, final AbstractDownloader instance, final @StringRes int name) {
            this.type = type;
            this.instance = instance;
            this.name = name;
        }
    }
}
