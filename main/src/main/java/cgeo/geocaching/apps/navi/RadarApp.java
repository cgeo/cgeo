package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.GeopointFormatter.Format;
import cgeo.geocaching.utils.ProcessUtils;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * Radar of GPS Status and Toolbox.
 * <p>
 * Version 12 and later no longer handle {@link #INTENT}, but accept radar targets as
 * deep link {@code https://mobiwia.com/radar?q=<lat>,<lon>(<name>)} (see #18889).
 * Older versions are still started via the legacy radar intent.
 * If GPS Status handles neither, the legacy radar intent is passed to any other radar app.
 */
class RadarApp extends AbstractRadarApp {

    private static final String INTENT = "com.google.android.radar.SHOW_RADAR";
    private static final String PACKAGE_NAME = "com.eclipsim.gpsstatus2";
    /** radar deep link of GPS Status 12+, target is passed as {@code ?q=<lat>,<lon>(<name>)} */
    private static final String RADAR_LINK_BASE = "https://mobiwia.com/radar";

    RadarApp() {
        super(R.string.cache_menu_radar, INTENT, PACKAGE_NAME);
    }

    @Override
    public boolean isInstalled() {
        // the legacy radar intent of any radar app includes GPS Status <= 11
        return isRadarLinkSupported() || ProcessUtils.isActivityAvailable(new Intent(INTENT));
    }

    @NonNull
    @Override
    protected Intent createIntent(@NonNull final Geopoint point, @Nullable final String name) {
        if (isRadarLinkSupported()) {
            return createRadarLinkIntent(Uri.parse(RADAR_LINK_BASE + "?q=" + createRadarTargetQuery(point, name)));
        }
        final Intent intent = super.createIntent(point, name);
        if (!ProcessUtils.isActivityAvailable(intent)) {
            // GPS Status does not handle the legacy radar intent, let any other radar app handle it
            intent.setPackage(null);
        }
        return intent;
    }

    @Override
    protected void addCoordinates(final Intent intent, final Geopoint coords) {
        intent.putExtra(RADAR_EXTRA_LATITUDE, (float) coords.getLatitude());
        intent.putExtra(RADAR_EXTRA_LONGITUDE, (float) coords.getLongitude());
    }

    private static boolean isRadarLinkSupported() {
        // the intent filter matches scheme, host and path only, query parameters are irrelevant
        return ProcessUtils.isActivityAvailable(createRadarLinkIntent(Uri.parse(RADAR_LINK_BASE)));
    }

    @NonNull
    private static Intent createRadarLinkIntent(@NonNull final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        // restrict to GPS Status, otherwise a browser could handle the link
        intent.setPackage(PACKAGE_NAME);
        return intent;
    }

    /**
     * Builds the value of the {@code q} parameter: {@code <lat>,<lon>} with an optional {@code (<name>)}.
     */
    @NonNull
    private static String createRadarTargetQuery(@NonNull final Geopoint point, @Nullable final String name) {
        final String coords = GeopointFormatter.format(Format.LAT_DECDEGREE_RAW, point)
                + "," + GeopointFormatter.format(Format.LON_DECDEGREE_RAW, point);
        if (StringUtils.isBlank(name)) {
            return coords;
        }
        // parentheses delimit the name, so they must not occur inside it
        final String label = StringUtils.trim(StringUtils.replaceChars(name, "()", "[]"));
        return coords + "(" + Uri.encode(label) + ")";
    }
}
