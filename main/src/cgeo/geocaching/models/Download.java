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
import cgeo.geocaching.downloader.MapDownloaderJustDownload;
import cgeo.geocaching.downloader.MapDownloaderJustDownloadThemes;
import cgeo.geocaching.downloader.MapDownloaderMapsforge;
import cgeo.geocaching.downloader.MapDownloaderOpenAndroMaps;
import cgeo.geocaching.downloader.MapDownloaderOpenAndroMapsThemes;
import cgeo.geocaching.downloader.MapDownloaderOSMPaws;
import cgeo.geocaching.downloader.MapDownloaderOSMPawsThemes;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.utils.CalendarUtils;

import android.net.Uri;

import androidx.annotation.DrawableRes;
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
    @DrawableRes private final int iconRes;
    public boolean customMarker = false; // handled solely by caller

    public Download(final String name, final Uri uri, final boolean isDir, final String dateISO, final String sizeInfo, final DownloadType type, @DrawableRes final int iconRes) {
        this.name = CompanionFileUtils.getDisplayName(name);
        this.uri = uri;
        this.isDir = isDir;
        this.sizeInfo = sizeInfo;
        this.addInfo = "";
        this.dateInfo = CalendarUtils.parseYearMonthDay(dateISO);
        this.type = type;
        this.iconRes = iconRes;
    }

    public Download(final PendingDownload pendingDownload) {
        final DownloadTypeDescriptor desc = DownloadType.fromTypeId(pendingDownload.getOfflineMapTypeId());

        this.name = CompanionFileUtils.getDisplayName(pendingDownload.getFilename());
        this.uri = Uri.parse(pendingDownload.getRemoteUrl());
        this.isDir = false;
        this.sizeInfo = "";
        this.addInfo = "";
        this.dateInfo = pendingDownload.getDate();
        this.type = desc == null ? DownloadType.DOWNLOADTYPE_ALL_MAPRELATED : desc.type;
        this.iconRes = R.drawable.ic_menu_file;
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
        final AbstractDownloader downloader = DownloadType.getInstance(type.id);
        return downloader != null ? downloader.mapSourceName : "???";
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    public enum DownloadType {
        // id values must not be changed as they are referenced in the database & download companion files
        DOWNLOADTYPE_ALL_MAPRELATED(0, 0),         // virtual entry
        DOWNLOADTYPE_MAP_MAPSFORGE(1, R.string.downloadmap_mapfile),
        DOWNLOADTYPE_MAP_OPENANDROMAPS(2, R.string.downloadmap_mapfile),
        DOWNLOADTYPE_THEME_OPENANDROMAPS(3, R.string.downloadmap_themefile),
        DOWNLOADTYPE_MAP_FREIZEITKARTE(4, R.string.downloadmap_mapfile),
        DOWNLOADTYPE_THEME_FREIZEITKARTE(5, R.string.downloadmap_themefile),
        DOWNLOADTYPE_MAP_HYLLY(6, R.string.downloadmap_mapfile),
        DOWNLOADTYPE_THEME_HYLLY(7, R.string.downloadmap_themefile),
        DOWNLOADTYPE_MAP_PAWS(8, R.string.downloadmap_mapfile),
        DOWNLOADTYPE_THEME_PAWS(9, R.string.downloadmap_themefile),

        DOWNLOADTYPE_MAP_JUSTDOWNLOAD(50, R.string.downloadmap_othermapdownload),
        DOWNLOADTYPE_THEME_JUSTDOWNLOAD(51, R.string.downloadmap_otherthemedownload),

        DOWNLOADTYPE_BROUTER_TILES(90, R.string.downloadmap_tilefile);

        public final int id;
        @StringRes final int typeNameResId;
        public static final int DEFAULT = DOWNLOADTYPE_MAP_MAPSFORGE.id;
        private static final ArrayList<DownloadTypeDescriptor> offlineMapTypes = new ArrayList<>();
        private static final ArrayList<DownloadTypeDescriptor> offlineMapThemeTypes = new ArrayList<>();
        private static final ArrayList<DownloadTypeDescriptor> downloadTypes = new ArrayList<>();

        DownloadType(final int id, @StringRes final int typeNameResId) {
            this.id = id;
            this.typeNameResId = typeNameResId;
        }

        public static ArrayList<DownloadTypeDescriptor> getOfflineMapTypes() {
            buildTypelist();
            return offlineMapTypes;
        }

        public static ArrayList<DownloadTypeDescriptor> getOfflineAllMapRelatedTypes() {
            buildTypelist();
            final ArrayList<DownloadTypeDescriptor> mapRelatedTypes = new ArrayList<>(offlineMapTypes);
            mapRelatedTypes.addAll(offlineMapThemeTypes);
            return mapRelatedTypes;
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

        @StringRes
        public int getTypeNameResId() {
            return typeNameResId;
        }

        private static void buildTypelist() {
            if (offlineMapTypes.size() == 0) {
                // put all bundled map theme file types here - they will not be shown in the maps download selector
                offlineMapThemeTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_THEME_OPENANDROMAPS, MapDownloaderOpenAndroMapsThemes.getInstance(), R.string.mapserver_openandromaps_themes_name));
                offlineMapThemeTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_THEME_FREIZEITKARTE, MapDownloaderFreizeitkarteThemes.getInstance(), R.string.mapserver_freizeitkarte_themes_name));
                offlineMapThemeTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_THEME_HYLLY, MapDownloaderHyllyThemes.getInstance(), R.string.mapserver_hylly_themes_name));
                offlineMapThemeTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_THEME_PAWS, MapDownloaderOSMPawsThemes.getInstance(), R.string.mapserver_osmpaws_themes_name));

                // put all file types that should be listed in the downloader here
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_MAPSFORGE, MapDownloaderMapsforge.getInstance(), R.string.mapserver_mapsforge_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_OPENANDROMAPS, MapDownloaderOpenAndroMaps.getInstance(), R.string.mapserver_openandromaps_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_FREIZEITKARTE, MapDownloaderFreizeitkarte.getInstance(), R.string.mapserver_freizeitkarte_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_HYLLY, MapDownloaderHylly.getInstance(), R.string.mapserver_hylly_name));
                offlineMapTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_PAWS, MapDownloaderOSMPaws.getInstance(), R.string.mapserver_osmpaws_name));

                // all other download types
                downloadTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_MAP_JUSTDOWNLOAD, MapDownloaderJustDownload.getInstance(), R.string.downloadmap_mapfile));
                downloadTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_THEME_JUSTDOWNLOAD, MapDownloaderJustDownloadThemes.getInstance(), R.string.downloadmap_themefile));
                downloadTypes.add(new DownloadTypeDescriptor(DOWNLOADTYPE_BROUTER_TILES, BRouterTileDownloader.getInstance(), R.string.brouter_name));

                // adding maps and map themes to download types for completeness
                downloadTypes.addAll(offlineMapTypes);
                downloadTypes.addAll(offlineMapThemeTypes);
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
