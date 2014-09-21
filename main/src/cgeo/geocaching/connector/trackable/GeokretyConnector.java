package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.AbstractLoggingActivity;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;
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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.xml.sax.InputSource;

import android.content.Context;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
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
                DataStore.saveTrackable(trackables.get(0));
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

    @Override
    public AbstractTrackableLoggingManager getTrackableLoggingManager(final AbstractLoggingActivity activity) {
        return new GeokretyLoggingManager(activity);
    }

    /**
     * Get geocode from geokrety id
     *
     */
    public static String geocode(final int id) {
        return String.format("GK%04X", id);
    }

    @Override
    public boolean isLoggable() {
        return true;
    }

    public static ImmutablePair<StatusCode, ArrayList<String>> postLogTrackable(final Geocache cache, final TrackableLog trackableLog, final Calendar date, final String log) {
        // See doc: http://geokrety.org/api.php
        Log.d("GeokretyConnector.postLogTrackable: nr=" + trackableLog.trackCode);
        if (trackableLog.brand != TrackableBrand.GEOKRETY) {
            Log.d("GeokretyConnector.postLogTrackable: receive invalid brand");
            return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, new ArrayList<String>());
        }
        if (trackableLog.action == LogTypeTrackable.DO_NOTHING) {
            Log.d("GeokretyConnector.postLogTrackable: received invalid logtype");
            return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, new ArrayList<String>());
        }
        try {
            // SecId is mandatory when using API, anonymous log are only possible via website
            if (null == Settings.getGeokretySecId() || Settings.getGeokretySecId().isEmpty()) {
                Log.e("GeokretyConnector.postLogTrackable: not authenticated");
                return new ImmutablePair<>(StatusCode.NO_LOGIN_INFO_STORED, new ArrayList<String>());
            }

            // Construct Post Parameters
            final Parameters params = new Parameters(
                    "secid", Settings.getGeokretySecId(),
                    "gzip", "0",
                    "nr", trackableLog.trackCode,
                    "formname", "ruchy",
                    "logtype", String.valueOf(trackableLog.action.gkid),
                    "data", String.format("%tY-%tm-%td", date, date, date), // YYYY-MM-DD
                    "godzina",String.format("%tH", date), // HH
                    "minuta", String.format("%tM", date), // MM
                    "comment", log,
                    "app", "c:geo", // getString(R.string.app_name), -- NEED HELP HERE
                    "app_ver", "dev", // getString(R.string.about_version) -- NEED HELP HERE
                    "mobile_lang", "en_EN.UTF-8" // How to get current locale ? -- NEED HELP HERE
            );
            // See doc: http://geokrety.org/help.php#acceptableformats
            if (null != cache && null != cache.getCoords() && null != cache.getGeocode()) {
                params.add("latlon", cache.getCoords().toString());
                params.add("wpt", cache.getGeocode());
            }

            final String page = Network.getResponseData(Network.postRequest(URL + "/ruchy.php", params));
            if (page == null) {
                Log.e("GeokretyConnector.postLogTrackable: No data from server");
                return new ImmutablePair<>(StatusCode.CONNECTION_FAILED_GK, new ArrayList<String>());
            }

            final ImmutablePair<Integer, ArrayList<String>> response = GeokretyParser.parseResponse(page);
            if (null == response) {
                Log.w("GeokretyConnector.postLogTrackable: Cannot parseResponse geokrety");
                return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, new ArrayList<String>());
            }
            if (!response.getRight().isEmpty()) {
                for (final String error: response.getRight()) {
                    Log.w("GeokretyConnector.postLogTrackable: "+ error);
                }
                return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, response.getRight());
            }
            Log.i("Geokrety Log successfully posted to trackable #" + trackableLog.trackCode);
            return new ImmutablePair<>(StatusCode.NO_ERROR, new ArrayList<String>());
        } catch (final Exception e) {
            Log.w("GeokretyConnector.searchTrackable", e);
            return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, new ArrayList<String>());
        }
    }
}
