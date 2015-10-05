package cgeo.geocaching.utils;

import cgeo.calendar.CalendarAddon;
import cgeo.contacts.ContactsAddon;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.sensors.OrientationProvider;
import cgeo.geocaching.sensors.RotationProvider;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SystemInformation {

    private SystemInformation() {
        // Do not instantiate
    }

    @NonNull
    public static String getSystemInformation(@NonNull final Context context) {
        final boolean googlePlayServicesAvailable = CgeoApplication.getInstance().isGooglePlayServicesAvailable();
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
                .append("\nDirection sensor used: ").append(Settings.useOrientationSensor(context) ? "orientation" : "rotation vector")
                .append("\nHide own/found: ").append(Settings.isExcludeMyCaches())
                .append("\nMap strategy: ").append(Settings.getLiveMapStrategy().toString().toLowerCase(Locale.getDefault()))
                .append("\nHW acceleration: ").append(Settings.useHardwareAcceleration() ? "enabled" : "disabled")
                .append(" (").append(Settings.useHardwareAcceleration() == Settings.HW_ACCEL_DISABLED_BY_DEFAULT ? "manually changed" : "default state").append(')')
                .append("\nSystem language: ").append(Locale.getDefault());
        if (Settings.useEnglish()) {
            body.append(" (cgeo forced to English)");
        }
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
                connectors.append("\n - ").append(connector.getName());
                if (connector instanceof ILogin) {
                    final ILogin login = (ILogin) connector;
                    connectors.append(": ").append(login.isLoggedIn() ? "logged in" : "not logged in")
                            .append(" (").append(login.getLoginStatusString()).append(')');
                    if (login.getName().equals("geocaching.com") && login.isLoggedIn()) {
                        connectors.append(" / ").append(Settings.getGCMemberStatus());
                    }
                }
            }
        }
        body.append("\nGeocaching sites enabled:").append(connectorCount > 0 ? connectors : " none");
    }

    private static void appendAddons(@NonNull final StringBuilder body) {
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

    public static String presence(final boolean present) {
        return present ? "present" : "absent";
    }
}
