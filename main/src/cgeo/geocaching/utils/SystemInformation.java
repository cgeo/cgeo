package cgeo.geocaching.utils;

import cgeo.calendar.CalendarAddon;
import cgeo.contacts.ContactsAddon;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.playservices.GooglePlayServices;
import cgeo.geocaching.sensors.MagnetometerAndAccelerometerProvider;
import cgeo.geocaching.sensors.OrientationProvider;
import cgeo.geocaching.sensors.RotationProvider;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public final class SystemInformation {

    private SystemInformation() {
        // Do not instantiate
    }

    @NonNull
    public static String getSystemInformation(@NonNull final Context context) {
        final boolean googlePlayServicesAvailable = GooglePlayServices.isAvailable();
        final String usedDirectionSensor;
        if (Settings.useOrientationSensor(context)) {
            usedDirectionSensor = "orientation";
        } else if (RotationProvider.hasRotationSensor(context)) {
            usedDirectionSensor = "rotation vector";
        } else {
            usedDirectionSensor = "magnetometer & accelerometer";
        }
        final StringBuilder body = new StringBuilder("--- System information ---")
                .append("\nDevice: ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(", ").append(Build.BRAND).append(')')
                .append("\nAndroid version: ").append(VERSION.RELEASE)
                .append("\nAndroid build: ").append(Build.DISPLAY)
                .append("\nCgeo version: ").append(Version.getVersionName(context))
                .append("\nGoogle Play services: ").append(googlePlayServicesAvailable ? (Settings.useGooglePlayServices() ? "enabled" : "disabled") : "unavailable")
                .append("\nLow power mode: ").append(Settings.useLowPowerMode() ? "active" : "inactive")
                .append("\nCompass capabilities: ").append(Sensors.getInstance().hasCompassCapabilities() ? "yes" : "no")
                .append("\nRotation vector sensor: ").append(presence(RotationProvider.hasRotationSensor(context)))
                .append("\nOrientation sensor: ").append(presence(OrientationProvider.hasOrientationSensor(context)))
                .append("\nMagnetometer & Accelerometer sensor: ").append(presence(MagnetometerAndAccelerometerProvider.hasMagnetometerAndAccelerometerSensors(context)))
                .append("\nDirection sensor used: ").append(usedDirectionSensor)
                .append("\nHide own/found: ").append(Settings.isExcludeMyCaches())
                .append("\nMap strategy: ").append(Settings.getLiveMapStrategy().toString().toLowerCase(Locale.getDefault()))
                .append("\nHW acceleration: ").append(Settings.useHardwareAcceleration() ? "enabled" : "disabled")
                .append(" (").append(Settings.useHardwareAcceleration() == Settings.HW_ACCEL_DISABLED_BY_DEFAULT ? "manually changed" : "default state").append(')')
                .append("\nSystem language: ").append(Locale.getDefault())
                .append("\nLog date format: ").append(Formatter.formatShortDate(System.currentTimeMillis()))
                .append("\nDebug mode active: ").append(Settings.isDebug() ? "yes" : "no");
        if (Settings.useEnglish()) {
            body.append(" (cgeo forced to English)");
        }
        appendPermissions(context, body);
        appendConnectors(body);
        appendAddons(body);
        body.append("\n--- End of system information ---\n");
        return body.toString();
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
        if (CalendarAddon.isAvailable()) {
            addons.add("calendar");
        }
        if (ContactsAddon.isAvailable()) {
            addons.add("contacts");
        }
        body.append("\nInstalled cgeo plugins: ");
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
}
