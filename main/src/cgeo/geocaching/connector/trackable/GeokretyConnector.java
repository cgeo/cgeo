package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.enumerations.TrackableBrand;
import cgeo.geocaching.loaders.AbstractCacheInventoryLoader;
import cgeo.geocaching.loaders.AbstractInventoryLoader;
import cgeo.geocaching.loaders.GeokretyCacheInventoryLoader;
import cgeo.geocaching.loaders.GeokretyInventoryLoader;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.xml.sax.InputSource;

import android.content.Context;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GeokretyConnector extends AbstractTrackableConnector {

    private static final Pattern PATTERN_GK_CODE = Pattern.compile("GK[0-9A-F]{4,}");
    private static final String URL = "http://geokrety.org";
    private static final String URLPROXY = "http://geokretymap.org";

    @Override
    public boolean canHandleTrackable(final String geocode) {
        return geocode != null && PATTERN_GK_CODE.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        return "http://geokrety.org/konkret.php?id=" + getId(trackable.getGeocode());
    }

    @Override
    @Nullable
    public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        return searchTrackable(geocode);
    }

    private static String getUrlCache() {
        return (Settings.isGeokretyCacheActive() ? URLPROXY : URL);
    }

    public static Trackable searchTrackable(final String geocode) {
        Log.d("GeokretyConnector.searchTrackable: gkid=" + getId(geocode));
        try {
            final String urlDetails = (Settings.isGeokretyCacheActive() ? URLPROXY + "/export-details.php" : URL + "/export2.php");

            final InputStream response = Network.getResponseStream(Network.getRequest(urlDetails + "?gkid=" + getId(geocode)));
            if (response == null) {
                Log.e("GeokretyConnector.searchTrackable: No data from server");
                return null;
            }
            final InputSource is = new InputSource(response);
            final List<Trackable> trackables = GeokretyParser.parse(is);
            if (!trackables.isEmpty()) {
                return trackables.get(0);
            }
        } catch (final Exception e) {
            Log.w("GeokretyConnector.searchTrackable", e);
        }
        // TODO maybe a fallback to no proxy would be cool?
        return null;
    }

    @Override
    public List<Trackable> searchTrackables(final String geocode) {
        Log.d("GeokretyConnector.searchTrackables: wpt=" + geocode);
        try {
            final InputStream response = Network.getResponseStream(Network.getRequest(getUrlCache() + "/export2.php?wpt=" + geocode));
            if (response == null) {
                Log.e("GeokretyConnector.searchTrackable: No data from server");
                return null;
            }
            final InputSource is = new InputSource(response);
            return GeokretyParser.parse(is);
        } catch (final Exception e) {
            Log.w("GeokretyConnector.searchTrackables", e);
            return null;
        }
    }

    @Override
    public List<Trackable> loadInventory() {
        return loadInventory(0);
    }

    public static List<Trackable> loadInventory(final int userid) {
        Log.d("GeokretyConnector.loadInventory: userid=" + userid);
        try {
            final Parameters params = new Parameters("inventory", "1");
            if (userid > 0) {
                // retrieve someone inventory
                params.put("userid", String.valueOf(userid));
            } else {
                // Retrieve intentory, with tracking codes
                params.put("secid", Settings.getGeokretySecId());
            }
            final InputStream response = Network.getResponseStream(Network.getRequest(URL + "/export2.php", params));
            if (response == null) {
                Log.e("GeokretyConnector.loadInventory: No data from server");
                return new ArrayList<>();
            }
            final InputSource is = new InputSource(response);
            return GeokretyParser.parse(is);
        } catch (final Exception e) {
            Log.w("GeokretyConnector.loadInventory", e);
            return new ArrayList<>();
        }
    }

    public static int getId(final String geocode) {
        try {
            final String hex = geocode.substring(2);
            return Integer.parseInt(hex, 16);
        } catch (final NumberFormatException e) {
            Log.e("Trackable.getId", e);
        }
        return -1;
    }

    @Override
    public @Nullable
    String getTrackableCodeFromUrl(@NonNull final String url) {
        // http://geokrety.org/konkret.php?id=38545
        String id = StringUtils.substringAfterLast(url, "konkret.php?id=");
        if (StringUtils.isNumeric(id)) {
            return geocode(Integer.parseInt(id));
        }
        // http://geokretymap.org/38545
        id = StringUtils.substringAfterLast(url, "geokretymap.org/");
        if (StringUtils.isNumeric(id)) {
            return geocode(Integer.parseInt(id));
        }
        return null;
    }

    @Override
    public TrackableBrand getBrand() {
        return TrackableBrand.GEOKRETY;
    }

    @Override
    public boolean isGenericLoggable() {
        return true;
    }

    @Override
    public boolean isActive() {
        return Settings.isGeokretyConnectorActive();
    }

    @Override
    public boolean isRegistered() {
        return Settings.isRegisteredForGeokrety();
    }

    @Override
    public int getInventoryLoaderId() {
        return Loaders.INVENTORY_GEOKRETY.getLoaderId();
    }

    @Override
    public int getCacheInventoryLoaderId() {
        return Loaders.CACHE_INVENTORY_GEOKRETY.getLoaderId();
    }

    @Override
    public AbstractInventoryLoader getInventoryLoader(final Context context) {
        return new GeokretyInventoryLoader(context, this);
    }

    @Override
    public AbstractCacheInventoryLoader getCacheInventoryLoader(final Context context, final String geocode) {
        return new GeokretyCacheInventoryLoader(context, this, geocode);
    }

    /**
     * Get geocode from geokrety id
     *
     */
    public static String geocode(final int id) {
        return String.format("GK%04X", id);
    }

}
