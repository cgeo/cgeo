package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.playservices.GooglePlayServices;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.sensors.MagnetometerAndAccelerometerProvider;
import cgeo.geocaching.sensors.OrientationProvider;
import cgeo.geocaching.sensors.RotationProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.FolderUtils;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.PersistableUri;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.html.HtmlUtils;
import cgeo.geocaching.wherigo.WherigoGame;
import cgeo.geocaching.wherigo.WherigoSavegameInfo;
import cgeo.geocaching.wherigo.WherigoThingType;

import android.app.ActivityManager;
import android.content.Context;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.common.GoogleApiAvailability;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class SystemInformation {

    private SystemInformation() {
        // Do not instantiate
    }

    @NonNull
    public static String getSystemInformation(@NonNull final Context context) {
        final String usedDirectionSensor;
        if (Settings.useOrientationSensor(context)) {
            usedDirectionSensor = "orientation";
        } else if (RotationProvider.hasRotationSensor(context)) {
            usedDirectionSensor = "rotation vector";
        } else {
            usedDirectionSensor = "magnetometer & accelerometer";
        }
        final String hideWaypoints = (Settings.isExcludeWpOriginal() ? "original " : "") + (Settings.isExcludeWpParking() ? "parking " : "") + (Settings.isExcludeWpVisited() ? "visited" : "");
        final StringBuilder body = new StringBuilder("## System information").append("\n")
                .append("\nc:geo version: ").append(Version.getVersionName(context)).append("\n")
                .append("\nDatetime: ").append(Formatter.formatDateTime(System.currentTimeMillis())).append("\n")

                .append("\nDevice:")
                .append("\n-------")
                .append("\n- Device type: ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(", ").append(Build.BRAND).append(')')
                .append("\n- Available processors: ").append(Runtime.getRuntime().availableProcessors())
                .append("\n- Android version: ").append(VERSION.RELEASE)
                .append("\n- Android build: ").append(Build.DISPLAY);
        appendScreenResolution(context, body);
        body.append("\n- Sailfish OS detected: ").append(EnvironmentUtils.isSailfishOs());
        appendGooglePlayServicesVersion(context, body);
        appendMemoryInfo(context, body);
        body.append("\n")
                .append("\nSensor and location:")
                .append("\n-------")
                .append("\n- Low power mode: ").append(Settings.useLowPowerMode() ? "active" : "inactive")
                .append("\n- Compass capabilities: ").append(LocationDataProvider.getInstance().hasCompassCapabilities() ? "yes" : "no")
                .append("\n- Rotation vector sensor: ").append(presence(RotationProvider.hasRotationSensor(context)))
                .append("\n- Orientation sensor: ").append(presence(OrientationProvider.hasOrientationSensor(context)))
                .append("\n- Magnetometer & Accelerometer sensor: ").append(presence(MagnetometerAndAccelerometerProvider.hasMagnetometerAndAccelerometerSensors(context)))
                .append("\n- Direction sensor used: ").append(usedDirectionSensor)

                .append("\n")
                .append("\nProgram settings:")
                .append("\n-------");
        appendSettings(body);
        body
                .append("\n- Set language: ").append(Settings.getUserLanguage().isEmpty() ? Locale.getDefault() + " (system default)" : Settings.getUserLanguage())
                .append("\n- System date format: ").append(Formatter.getShortDateFormat())
                .append("\n- Time zone: ").append(CalendarUtils.getUserTimeZoneString())
                .append("\n- Debug mode active: ").append(Settings.isDebug() ? "yes" : "no")
                .append("\n- Log Settings: ").append(Log.getLogSettingsForDisplay())
                .append("\n- Last manual backup: ").append(BackupUtils.hasBackup(BackupUtils.newestBackupFolder(false)) ? BackupUtils.getNewestBackupDateTime(false) : "never")
                .append("\n- Last auto backup: ").append(BackupUtils.hasBackup(BackupUtils.newestBackupFolder(true)) ? BackupUtils.getNewestBackupDateTime(true) : "never");
        appendRoutingModes(body, context);
        appendMapModeSettings(body);
        appendMapSourceInformation(body, context);
        body
                .append("\n")
                .append("\nFilters:")
                .append("\n-------")
                .append("\n- Hide waypoints: ").append(hideWaypoints.isEmpty() ? "-" : hideWaypoints);
        appendFilters(body);

        body
                .append("\n")
                .append("\nServices:")
                .append("\n-------");
        appendConnectors(body);
        if (GCConnector.getInstance().isActive()) {
            body.append("\n- Geocaching.com date format: ").append(Settings.getGcCustomDate());
            body.append("\n- Geocaching.com website language: ").append(GCLogin.getInstance().getWebsiteLanguage());
        }
        final Pair<String, Long> gcError = Settings.getLastLoginErrorGC();
        if (gcError != null) {
            body.append("\n- Last login error on geocaching.com: ").append(gcError.first).append(" (").append(Formatter.formatDateForFilename(gcError.second)).append(")");
        }
        final long gcSuccess = Settings.getLastLoginSuccessGC();
        if (gcSuccess != 0) {
            body.append("\n- Last successful login on geocaching.com: ").append(Formatter.formatDateForFilename(gcSuccess));
        }
        body.append("\n- Routing: ").append(Settings.useInternalRouting() ? "internal" : "external").append(" / BRouter installed: ").append(ProcessUtils.isInstalled(context.getString(R.string.package_brouter)));

        appendPermissions(context, body);

        appendWherigo(body);

        body.append("\n")
                .append("\nPaths")
                .append("\n-------");
        appendDirectory(body, "\n- System internal c:geo dir: ", LocalStorage.getInternalCgeoDirectory());
        appendDirectory(body, "\n- Legacy User storage c:geo dir: ", LocalStorage.getExternalPublicCgeoDirectory());
        appendDirectory(body, "\n- Geocache data: ", LocalStorage.getGeocacheDataDirectory());
        appendDirectory(body, "\n- Internal theme sync (is turned " + (RenderThemeHelper.isThemeSynchronizationActive() ? "ON" : "off") + "): ", LocalStorage.getMapThemeInternalSyncDir());
        body.append("\n- Map render theme path: ").append(Settings.getSelectedMapRenderTheme());
        appendPublicFolders(body);
        appendPersistedDocumentUris(body);
        appendPersistedUriPermission(body, context);

        body.append("\n")
                .append("\nDatabase")
                .append("\n-------");
        appendDatabase(body);

        body.append("\n\n--- End of system information ---\n");
        return body.toString();
    }

    private static void appendMemoryInfo(@NonNull final Context context, @NonNull final StringBuilder body) {
        body.append("\n- Memory: ");
        final ActivityManager.MemoryInfo memoryInfo = EnvironmentUtils.getMemoryInfo(context);
        if (memoryInfo == null) {
            body.append("null");
        } else {
            body.append(" Available:").append(Formatter.formatBytes(memoryInfo.availMem))
                .append(", Total:").append(Formatter.formatBytes(memoryInfo.totalMem))
                .append(", Threshold: ").append(Formatter.formatBytes(memoryInfo.threshold))
                .append(", low:").append(memoryInfo.lowMemory);
        }
    }

    private static void appendDatabase(@NonNull final StringBuilder body) {
        final File dbFile = DataStore.databasePath();
        body.append("\n- File: ").append(dbFile)
                .append(" (").append(versionInfoToString(DataStore.getActualDBVersion(), DataStore.getExpectedDBVersion()))
                .append(", Size:").append(Formatter.formatBytes(dbFile.length())).append(") on ")
                .append(Settings.isDbOnSDCard() ? "user storage" : "system internal storage");
        body.append("\n- Data: ").append(DataStore.getTableCounts());
        body.append("\n- Extension Data: ").append(DataStore.getExtensionTableKeyCounts());
    }

    private static void appendSettings(@NonNull final StringBuilder body) {
        body.append("\n- Settings: ").append(versionInfoToString(Settings.getActualVersion(), Settings.getExpectedVersion()))
                .append(", Count:").append(Settings.getPreferencesCount());
    }

    private static void appendFilters(@NonNull final StringBuilder body) {
        for (GeocacheFilterContext.FilterType filterType : GeocacheFilterContext.FilterType.values()) {
            if (filterType == GeocacheFilterContext.FilterType.TRANSIENT) {
                continue;
            }
            body.append("\n- ").append(filterType.name()).append(": ");
            final GeocacheFilter filter = new GeocacheFilterContext(filterType).get();
            body.append(filter.toUserDisplayableString()).append(" (").append(filter.toConfig()).append(")");
        }
        final Collection<GeocacheFilter> storedFilters = GeocacheFilter.Storage.getStoredFilters();
        if (!storedFilters.isEmpty()) {
            body.append("\n- ").append("Additional stored filters: ").append(storedFilters.size());
        }
    }

    private static void appendDirectory(@NonNull final StringBuilder body, @NonNull final String label, @NonNull final File directory) {
        body.append(label).append(directory).append(" (").append(Formatter.formatBytes(FileUtils.getFreeDiskSpace(directory))).append(" free)")
                .append(" ").append(versionInfoToString(LocalStorage.getCurrentVersion(), LocalStorage.getExpectedVersion()));
        try {
            if (directory.getAbsolutePath().startsWith(LocalStorage.getInternalCgeoDirectory().getAbsolutePath())) {
                body.append(" internal");
            } else if (Environment.isExternalStorageRemovable(directory)) {
                body.append(" external removable");
            } else {
                body.append(" external non-removable");
            }
            if (directory.isDirectory()) {
                body.append(" isDir(").append(directory.list().length).append(" entries)");
            } else if (directory.isFile()) {
                body.append(" isFile");
            } else {
                body.append(" notExisting");
            }
        } catch (final IllegalArgumentException ignored) {
            // thrown if the directory isn't pointing to an external storage
            body.append(" internal");
        }
    }

    private static void appendPublicFolders(@NonNull final StringBuilder body) {
        body.append("\n- Public Folders: #").append(PersistableFolder.values().length);
        for (PersistableFolder folder : PersistableFolder.values()) {
            final boolean isAvailable = ContentStorage.get().ensureFolder(folder);
            final FolderUtils.FolderInfo folderInfo = FolderUtils.get().getFolderInfo(folder.getFolder());
            final ImmutablePair<Long, Long> freeSpace = FolderUtils.get().getDeviceInfo(folder.getFolder());
            body.append("\n  - ").append(folder.name()).append(": ")
                    .append(UriUtils.uriToString(ContentStorage.get().getUriForFolder(folder.getFolder())))
                    .append(" (av:").append(isAvailable)
                    .append(", files:>=").append(folderInfo.fileCount)
                    .append(", size:>=").append(Formatter.formatBytes(folderInfo.totalFileSize))
                    .append(", free:>=").append(Formatter.formatBytes(freeSpace.left))
                    .append(")");
        }
    }

    private static void appendPersistedDocumentUris(@NonNull final StringBuilder body) {
        body.append("\n- PersistedDocumentUris: #").append(PersistableUri.values().length);
        for (PersistableUri persDocUri : PersistableUri.values()) {
            body.append("\n  - ").append(persDocUri);
        }
    }

    private static void appendRoutingModes(@NonNull final StringBuilder body, @NonNull final Context ctx) {
        body.append("\n- Routing mode: ").append(LocalizationUtils.getEnglishString(ctx, Settings.getRoutingMode().infoResId)).append(" (");
        for (RoutingMode mode : RoutingMode.values()) {
            if (mode != RoutingMode.OFF && mode != RoutingMode.STRAIGHT) {
                if (mode != RoutingMode.WALK) {
                    body.append(" / ");
                }
                final String profile = Settings.getRoutingProfile(mode);
                body.append(profile == null ? "-" : profile);
            }
        }
        body.append(")");
    }

    private static void appendMapModeSettings(@NonNull final StringBuilder body) {
        body
            .append("\n- Map mode: ")
            .append(Settings.useLegacyMaps() ? "legacy" : "UnifiedMap")
            .append(Settings.isLiveMap() ? " / live" : "")
            .append(" / OSM multi-threading: ").append(Settings.hasOSMMultiThreading() ? Settings.getMapOsmThreads() : "off");
    }

    private static void appendMapSourceInformation(@NonNull final StringBuilder body, @NonNull final Context ctx) {
        body.append("\n- Map: ");
        if (Settings.useLegacyMaps()) {
            final MapSource source = Settings.getMapSource();
            final ImmutablePair<String, Boolean> mapAttrs = source.calculateMapAttribution(ctx);
            body.append(source.getName()) // unfortunately localized but an English string would require large refactoring. The sourceId provides an unlocalized and unique identifier.
                    .append("\n  - Id: ").append(source.getId())
                    .append("\n  - Attrs: ").append(mapAttrs == null ? "none" : HtmlUtils.extractText(mapAttrs.left).replace("\n", " / "))
                    .append("\n  - Theme: ").append(StringUtils.isBlank(Settings.getSelectedMapRenderTheme()) ? "none" : Settings.getSelectedMapRenderTheme());
        } else {
            final AbstractTileProvider tileProvider = Settings.getTileProvider();
            final Pair<String, Boolean> mapAttrs = tileProvider.getMapAttribution();
            body.append(tileProvider.getTileProviderName())
                    .append("\n  - Id: ").append(tileProvider.getId())
                    .append("\n  - Attrs: ").append(mapAttrs == null ? "none" : HtmlUtils.extractText(mapAttrs.first).replace("\n", " / "))
                    .append("\n  - Theme: ").append(StringUtils.isBlank(Settings.getSelectedMapRenderTheme()) ? "none" : Settings.getSelectedMapRenderTheme());
        }
    }


    private static void appendPersistedUriPermission(@NonNull final StringBuilder body, @NonNull final Context context) {
        final List<UriPermission> uriPerms = context.getContentResolver().getPersistedUriPermissions();
        body.append("\n- Persisted Uri Permissions: #").append(uriPerms.size());
        for (UriPermission uriPerm : uriPerms) {
            body.append("\n  - ").append(UriUtils.uriPermissionToString(uriPerm));
        }
    }

    private static void appendConnectors(@NonNull final StringBuilder body) {
        final StringBuilder connectors = new StringBuilder(128);
        int connectorCount = 0;
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (connector.isActive()) {
                connectorCount++;
                connectors.append("\n   - ").append(connector.getName());
                if (connector instanceof ILogin) {
                    final ILogin login = (ILogin) connector;
                    connectors.append(": ").append(login.isLoggedIn() ? "Logged in" : "Not logged in")
                            .append(" (").append(login.getLoginStatusString() /* unfortunately localized but an English string would require large refactoring */).append(')');
                    if (login.getName().equals("geocaching.com") && login.isLoggedIn()) {
                        connectors.append(" / ").append(Settings.getGCMemberStatus());
                    }
                }
            }
        }
        body.append("\n- Geocaching sites enabled:").append(connectorCount > 0 ? connectors : " None");
    }

    private static String presence(final boolean present) {
        return present ? "present" : "absent";
    }

    private static void appendPermission(final Context context, final StringBuilder body, final String permission) {
        body.append("\n- ").append(StringUtils.remove(permission, "android.permission.")).append(": ").append(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED ? "granted" : "DENIED");
    }

    private static void appendPermissions(final Context context, final StringBuilder body) {
        body.append("\n")
                .append("\nPermissions")
                .append("\n-------");
        for (PermissionContext pc : PermissionContext.values()) {
            for (String permission : pc.getPermissions()) {
                appendPermission(context, body, permission);
            }
        }
    }

    private static void appendWherigo(final StringBuilder body) {
        final WherigoGame game = WherigoGame.get();
        final ContentStorage.FileInformation  cartridgeFileInfo = game.getCartridgeInfo() == null ? null : game.getCartridgeInfo().getFileInfo();
        final CharSequence loadFileInfo = TextUtils.join(WherigoSavegameInfo.getAllSaveFiles(cartridgeFileInfo), WherigoSavegameInfo::toShortString, ", ");
        final CharSequence visibleThingsCounts = TextUtils.join(Arrays.asList(WherigoThingType.values()), tt -> tt.name() + ":" + tt.getThingsForUserDisplay().size(), ", ");
        body.append("\n")
            .append("\nWherigo")
            .append("\n-------")
            .append("\n- playing:").append(game.isPlaying() + ", debug:" + game.isDebugMode() + ", debugFC:" + game.isDebugModeForCartridge())
            .append("\n- Name: " + game.getCartridgeName() + " (" + game.getCGuid() + ")")
            .append("\n- Cache context: " + game.getContextGeocacheName())
            .append("\n- Last Error: " + game.getLastError())
            .append("\n- Last Played: " + game.getLastPlayedCGuid() + " / " + game.getLastSetContextGeocode())
            .append("\n- Visible things: " + visibleThingsCounts)
            .append("\n- Cartridge File: " + cartridgeFileInfo)
            .append("\n- Load Slots: " + loadFileInfo);
    }

    private static void appendGooglePlayServicesVersion(final Context context, final StringBuilder body) {
        final boolean googlePlayServicesAvailable = GooglePlayServices.isAvailable();
        body.append("\n- Google Play services: ").append(googlePlayServicesAvailable ? (Settings.useGooglePlayServices() ? "enabled" : "disabled") : "unavailable");
        if (googlePlayServicesAvailable) {
            body.append(" - ");
            try {
                body.append(StringUtils.defaultIfBlank(context.getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0).versionName, "unknown version"));
            } catch (final PackageManager.NameNotFoundException e) {
                body.append("unretrievable version (").append(e.getMessage()).append(')');
            }
        }
    }

    private static void appendScreenResolution(final Context context, final StringBuilder body) {
        final Configuration config = context.getResources().getConfiguration();
        final Point size = new Point();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(size);

        body.append("\n- Screen resolution: ").append(size.x).append("x").append(size.y).append("px (").append(config.screenWidthDp).append("x").append(config.screenHeightDp).append("dp)");
        body.append("\n- Pixel density: ").append(context.getResources().getDisplayMetrics().scaledDensity);
        body.append("\n- System font scale: ").append(android.provider.Settings.System.getFloat(context.getContentResolver(), android.provider.Settings.System.FONT_SCALE, 1f)).append(" / used scale: ").append(config.fontScale);
    }

    private static String versionInfoToString(final int actualVersion, final int expectedVersion) {
        return "v" + actualVersion +
                (actualVersion == expectedVersion ? "" : "[Expected v" + expectedVersion + "]");
    }
}
