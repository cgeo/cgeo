package cgeo.geocaching.utils;

import cgeo.contacts.ContactsAddon;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.playservices.GooglePlayServices;
import cgeo.geocaching.sensors.MagnetometerAndAccelerometerProvider;
import cgeo.geocaching.sensors.OrientationProvider;
import cgeo.geocaching.sensors.RotationProvider;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.HwAccel;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PublicLocalFolder;
import cgeo.geocaching.storage.PublicLocalStorage;

import android.Manifest;
import android.content.Context;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.common.GoogleApiAvailability;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
        final String hideCaches = (Settings.isExcludeMyCaches() ? "own/found " : "") + (Settings.isExcludeDisabledCaches() ? "disabled " : "") + (Settings.isExcludeArchivedCaches() ? "archived" : "");
        final String hideWaypoints = (Settings.isExcludeWpOriginal() ? "original " : "") + (Settings.isExcludeWpParking() ? "parking " : "") + (Settings.isExcludeWpVisited() ? "visited" : "");
        final StringBuilder body = new StringBuilder("--- System information ---")
                .append("\nDevice: ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(", ").append(Build.BRAND).append(')')
                .append("\nAndroid version: ").append(VERSION.RELEASE)
                .append("\nAndroid build: ").append(Build.DISPLAY)
                .append("\nc:geo version: ").append(Version.getVersionName(context));
        appendGooglePlayServicesVersion(context, body);
        body
            .append("\nLow power mode: ").append(Settings.useLowPowerMode() ? "active" : "inactive")
            .append("\nCompass capabilities: ").append(Sensors.getInstance().hasCompassCapabilities() ? "yes" : "no")
            .append("\nRotation vector sensor: ").append(presence(RotationProvider.hasRotationSensor(context)))
            .append("\nOrientation sensor: ").append(presence(OrientationProvider.hasOrientationSensor(context)))
            .append("\nMagnetometer & Accelerometer sensor: ").append(presence(MagnetometerAndAccelerometerProvider.hasMagnetometerAndAccelerometerSensors(context)))
            .append("\nDirection sensor used: ").append(usedDirectionSensor)
            .append("\nHide caches: ").append(hideCaches.isEmpty() ? "-" : hideCaches)
            .append("\nHide waypoints: ").append(hideWaypoints.isEmpty() ? "-" : hideWaypoints)
            .append("\nHW acceleration: ").append(Settings.useHardwareAcceleration() ? "enabled" : "disabled")
            .append(" (").append(Settings.useHardwareAcceleration() == HwAccel.hwAccelShouldBeEnabled() ? "default state" : "manually changed").append(')')
            .append("\nSystem language: ").append(Locale.getDefault()).append(" / user-defined language: ").append(Settings.getUserLanguage())
            .append("\nSystem date format: ").append(Formatter.getShortDateFormat())
            .append("\nDebug mode active: ").append(Settings.isDebug() ? "yes" : "no");
        appendDirectory(body, "\nSystem internal c:geo dir: ", LocalStorage.getInternalCgeoDirectory());
        appendDirectory(body, "\nUser storage c:geo dir: ", LocalStorage.getExternalPublicCgeoDirectory());
        appendDirectory(body, "\nGeocache data: ", LocalStorage.getGeocacheDataDirectory());
        appendPublicFolders(body);
        appendPersistedUriPermission(body, context);
        appendDatabase(body);
        body
                .append("\nLast backup: ").append(BackupUtils.hasBackup(BackupUtils.newestBackupFolder()) ? BackupUtils.getNewestBackupDateTime() : "never")
                .append("\nGPX import path: ").append(Settings.getGpxImportDir())
                .append("\nGPX export path: ").append(Settings.getGpxExportDir())
                .append("\nOffline maps path: ").append(PublicLocalFolder.OFFLINE_MAPS)
                .append("\nMap render theme path: ").append(Settings.getCustomRenderThemeFilePath())
                .append("\nLive map mode: ").append(Settings.isLiveMap())
                .append("\nGlobal filter: ").append(Settings.getCacheType().pattern)
                .append("\nSailfish OS detected: ").append(EnvironmentUtils.isSailfishOs());
        appendPermissions(context, body);
        appendConnectors(body);
        if (GCConnector.getInstance().isActive()) {
            body.append("\nGeocaching.com date format: ").append(Settings.getGcCustomDate());
        }
        appendAddons(body);
        body.append("\nBRouter connection available: ").append(Routing.isAvailable());
        body.append("\n--- End of system information ---\n");
        return body.toString();
    }

    private static void appendDatabase(@NonNull final StringBuilder body) {
        final File dbFile = DataStore.databasePath();
        body.append("\nDatabase: ").append(dbFile)
                .append(" (").append(Formatter.formatBytes(dbFile.length())).append(") on ")
                .append(Settings.isDbOnSDCard() ? "user storage" : "system internal storage");
    }

    private static void appendDirectory(@NonNull final StringBuilder body, @NonNull final String label, @NonNull final File directory) {
        body.append(label).append(directory).append(" (").append(Formatter.formatBytes(FileUtils.getFreeDiskSpace(directory))).append(" free)");
        try {
            if (directory.getAbsolutePath().startsWith(LocalStorage.getInternalCgeoDirectory().getAbsolutePath())) {
                body.append(" internal");
            } else if (Environment.isExternalStorageRemovable(directory)) {
                body.append(" external removable");
            } else {
                body.append(" external non-removable");
            }
        } catch (final IllegalArgumentException ignored) {
            // thrown if the directory isn't pointing to an external storage
            body.append(" internal");
        }
    }

    private static void appendPublicFolders(@NonNull final StringBuilder body) {
        body.append("\nPublic Folders: #").append(PublicLocalFolder.ALL.length);
        for (PublicLocalFolder folder : PublicLocalFolder.ALL) {
            body.append("\n- ").append(folder.toString())
                .append(" (").append(PublicLocalStorage.get().getFolderSystemInformation(folder)).append(")");
        }
    }

    private static void appendPersistedUriPermission(@NonNull final StringBuilder body, @NonNull  final Context context) {
        final List<UriPermission> uriPerms = context.getContentResolver().getPersistedUriPermissions();
        body.append("\nPersisted Uri Permissions: #").append(uriPerms.size());
        for (UriPermission uriPerm : uriPerms) {
            body.append("\n- ").append(PublicLocalStorage.uriPermissionToString(uriPerm));
        }
    }

    private static void appendConnectors(@NonNull final StringBuilder body) {
        final StringBuilder connectors = new StringBuilder(128);
        int connectorCount = 0;
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (connector.isActive()) {
                connectorCount++;
                connectors.append("\n   ").append(connector.getName());
                if (connector instanceof ILogin) {
                    final ILogin login = (ILogin) connector;
                    connectors.append(": ").append(login.isLoggedIn() ? "Logged in" : "Not logged in")
                            .append(" (").append(login.getLoginStatusString()).append(')');
                    if (login.getName().equals("geocaching.com") && login.isLoggedIn()) {
                        connectors.append(" / ").append(Settings.getGCMemberStatus());
                    }
                }
            }
        }
        body.append("\nGeocaching sites enabled:").append(connectorCount > 0 ? connectors : " None");
    }

    private static void appendAddons(final StringBuilder body) {
        final List<String> addons = new ArrayList<>(2);
        if (ContactsAddon.isAvailable()) {
            addons.add("contacts");
        }
        body.append("\nInstalled c:geo plugins: ");
        body.append(CollectionUtils.isNotEmpty(addons) ? StringUtils.join(addons, ", ") : " none");
    }

    private static String presence(final boolean present) {
        return present ? "present" : "absent";
    }

    private static void appendPermission(final Context context, final StringBuilder body, final String name, final String permission) {
        body.append('\n').append(name).append(" permission: ").append(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED ? "granted" : "DENIED");
    }

    private static void appendPermissions(final Context context, final StringBuilder body) {
        appendPermission(context, body, "Fine location", Manifest.permission.ACCESS_FINE_LOCATION);
        appendPermission(context, body, "Write external storage", Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private static void appendGooglePlayServicesVersion(final Context context, final StringBuilder body) {
        final boolean googlePlayServicesAvailable = GooglePlayServices.isAvailable();
        body.append("\nGoogle Play services: ").append(googlePlayServicesAvailable ? (Settings.useGooglePlayServices() ? "enabled" : "disabled") : "unavailable");
        if (googlePlayServicesAvailable) {
            body.append(" - ");
            try {
                body.append(StringUtils.defaultIfBlank(context.getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0).versionName, "unknown version"));
            } catch (final PackageManager.NameNotFoundException e) {
                body.append("unretrievable version (").append(e.getMessage()).append(')');
            }
        }
    }
}
