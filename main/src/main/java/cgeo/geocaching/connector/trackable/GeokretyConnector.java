package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.AbstractLoggingActivity;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.Version;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.xml.sax.InputSource;

public class GeokretyConnector extends AbstractTrackableConnector {

    /*
    1) tracking code:

    is generated from the alphabet:
    "a b c d e f g h i j k l m n p q r s t u v w x y z 1 2 3 4 5 6 7 8 9"
    (no O and 0)
    sanity-check for tracking code: if generated code look like reference
    number (ie GKxxxx):

    preg_match("#^gk[0-9a-f]{4}$#i", $tc)

    2) reference number (GKxxxx):
    it is just a subsequent number in the database ($id) converted to hex:

    $gk=sprintf("GK%04X",$id);
    $id=hexdec(substr($gk, 2, 4));
     */
    private static final Pattern PATTERN_GK_CODE = Pattern.compile("GK[0-9A-F]{4,}");
    private static final Pattern PATTERN_GK_CODE_EXTENDED = Pattern.compile("(GK[0-9A-F]{4,})|([1-9A-NP-Z]{6})");
    private static final String HOST = "new-theme.staging.geokrety.org";
    public static final String URL = "https://" + HOST;
    private static final String URLPROXY = "https://api.geokrety.org";

    @Override
    @NonNull
    public String getHost() {
        return HOST;
    }

    @Override
    @NonNull
    public String getHostUrl() {
        return URL;
    }

    @Override
    @Nullable
    public String getProxyUrl() {
        return URLPROXY;
    }

    @Override
    public int getPreferenceActivity() {
        return R.string.preference_screen_geokrety;
    }

    @Override
    public boolean canHandleTrackable(@Nullable final String geocode) {
        return geocode != null && PATTERN_GK_CODE.matcher(geocode).matches();
    }

    @Override
    public boolean canHandleTrackable(@Nullable final String geocode, @Nullable final TrackableBrand brand) {
        if (brand != TrackableBrand.GEOKRETY) {
            return canHandleTrackable(geocode);
        }
        return geocode != null && PATTERN_GK_CODE_EXTENDED.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getServiceTitle() {
        return CgeoApplication.getInstance().getString(R.string.init_geokrety);
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        //https://geokrety.org/konkret.php?id=46464
        return URL + "/konkret.php?id=" + getId(trackable.getGeocode());
    }

    @Override
    @Nullable
    public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        return searchTrackable(geocode);
    }

    @Nullable
    @WorkerThread
    public static Trackable searchTrackable(final String geocode) {
        final int gkid;

        if (StringUtils.startsWithIgnoreCase(geocode, "GK")) {
            gkid = getId(geocode);
            if (gkid < 0) {
                Log.d("GeokretyConnector.searchTrackable: Unable to retrieve GK numeric ID by ReferenceNumber");
                return null;
            }
        } else {
            // This is probably a Tracking Code
            Log.d("GeokretyConnector.searchTrackable: geocode=" + geocode);

            final String geocodeFound = getGeocodeFromTrackingCode(geocode);
            if (geocodeFound == null) {
                Log.d("GeokretyConnector.searchTrackable: Unable to retrieve trackable by TrackingCode");
                return null;
            }
            gkid = getId(geocodeFound);
        }

        Log.d("GeokretyConnector.searchTrackable: gkid=" + gkid);
        try {
            final String[] gkUlrs = {
                    URLPROXY + "/gk/" + gkid + "/details",
                    URL + "/export2.php" + "?gkid=" + gkid,
            };
            InputStream response = null;
            for (final String urlDetails : gkUlrs) {
                response = Network.getResponseStream(Network.getRequest(urlDetails));
                if (response != null) {
                    break;
                }
                Log.d("GeokretyConnector.searchTrackable: No data from address " + urlDetails);
            }
            if (response == null) {
                Log.d("GeokretyConnector.searchTrackable: No data for gkid " + gkid);
                return null;
            }
            try {
                final InputSource is = new InputSource(response);
                final List<Trackable> trackables = GeokretyParser.parse(is);

                if (CollectionUtils.isNotEmpty(trackables)) {
                    final Trackable trackable = trackables.get(0);
                    DataStore.saveTrackable(trackable);
                    return trackable;
                }
            } finally {
                IOUtils.closeQuietly(response);
            }
        } catch (final Exception e) {
            Log.w("GeokretyConnector.searchTrackable", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<Trackable> searchTrackables(final String geocode) {
        Log.d("GeokretyConnector.searchTrackables: wpt=" + geocode);
        try {
            final String geocodeEncoded = URLEncoder.encode(geocode, "utf-8");
            final String[] gkUlrs = {
                    URLPROXY + "/wpt/" + geocodeEncoded,
                    URL + "/export2.php?wpt=" + geocodeEncoded,
            };
            InputStream response = null;
            for (final String urlDetails : gkUlrs) {
                response = Network.getResponseStream(Network.getRequest(urlDetails));
                if (response != null) {
                    break;
                }
                Log.d("GeokretyConnector.searchTrackables: No data from from address " + urlDetails);
            }
            if (response == null) {
                Log.d("GeokretyConnector.searchTrackables: No data for geocode " + geocode);
                return Collections.emptyList();
            }
            try {
                final InputSource is = new InputSource(response);
                return GeokretyParser.parse(is);
            } finally {
                IOUtils.closeQuietly(response);
            }
        } catch (final Exception e) {
            Log.w("GeokretyConnector.searchTrackables", e);
            return Collections.emptyList();
        }
    }

    @Override
    @NonNull
    public List<Trackable> loadInventory() {
        return loadInventory(0);
    }

    @NonNull
    @WorkerThread
    private static List<Trackable> loadInventory(final int userid) {
        Log.d("GeokretyConnector.loadInventory: userid=" + userid);
        try {
            final Parameters params = new Parameters("inventory", "1");
            if (userid > 0) {
                // retrieve someone inventory
                params.put("userid", String.valueOf(userid));
            } else {
                if (StringUtils.isBlank(Settings.getGeokretySecId())) {
                    return Collections.emptyList();
                }
                // Retrieve inventory, with tracking codes
                params.put("secid", Settings.getGeokretySecId());
            }
            final InputStream response = Network.getResponseStream(Network.getRequest(URL + "/export2.php", params));
            if (response == null) {
                Log.d("GeokretyConnector.loadInventory: No data from server");
                return Collections.emptyList();
            }
            try {
                final InputSource is = new InputSource(response);
                return GeokretyParser.parse(is);
            } finally {
                IOUtils.closeQuietly(response);
            }
        } catch (final Exception e) {
            Log.w("GeokretyConnector.loadInventory", e);
            return Collections.emptyList();
        }
    }

    @Override
    @NonNull
    public Observable<TrackableLog> trackableLogInventory() {
        return Observable.fromIterable(loadInventory()).map(new TrackableLogFunction());
    }

    private static class TrackableLogFunction implements Function<Trackable, TrackableLog> {
        @Override
        public TrackableLog apply(final Trackable trackable) {
            return new TrackableLog(
                    trackable.getGeocode(),
                    trackable.getTrackingcode(),
                    trackable.getName(),
                    getId(trackable.getGeocode()),
                    0,
                    trackable.getBrand()
            );
        }
    }

    public static int getId(final String geocode) {
        try {
            final String hex = geocode.substring(2);
            return Integer.parseInt(hex, 16);
        } catch (final NumberFormatException e) {
            Log.e("GeokretyConnector.getId", e);
        }
        return -1;
    }

    @Override
    @Nullable
    public String getTrackableCodeFromUrl(@NonNull final String url) {
        // https://geokrety.org/konkret.php?id=38545
        final String gkId = StringUtils.substringAfterLast(url, "konkret.php?id=");
        if (StringUtils.isNumeric(gkId)) {
            return geocode(Integer.parseInt(gkId));
        }
        // https://api.geokrety.org/gk/38545
        // https://api.geokrety.org/gk/38545/details
        String gkapiId = StringUtils.substringAfterLast(url, "api.geokrety.org/gk/");
        gkapiId = StringUtils.substringBeforeLast(gkapiId, "/");
        if (StringUtils.isNumeric(gkapiId)) {
            return geocode(Integer.parseInt(gkapiId));
        }
        return null;
    }

    @Override
    @Nullable
    public String getTrackableTrackingCodeFromUrl(@NonNull final String url) {
        // http://geokrety.org/m/qr.php?nr=<TRACKING_CODE>
        final String gkTrackingCode = StringUtils.substringAfterLast(url, "qr.php?nr=");
        if (StringUtils.isAlphanumeric(gkTrackingCode)) {
            return gkTrackingCode;
        }
        return null;
    }

    /**
     * Lookup Trackable Geocode from Tracking Code.
     *
     * @param trackingCode the Trackable Tracking Code to lookup
     * @return the Trackable Geocode
     */
    @Nullable
    @WorkerThread
    private static String getGeocodeFromTrackingCode(final String trackingCode) {
        final String response = Network.getResponseData(Network.getRequest(URLPROXY + "/nr2id/" + trackingCode));
        // An empty response means "not found"
        if (response == null || StringUtils.equals(response, "0")) {
            return null;
        }
        return geocode(Integer.parseInt(response));
    }

    @Override
    @NonNull
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
        return Settings.isRegisteredForGeokretyLogging() && isActive();
    }

    @Override
    public boolean recommendLogWithGeocode() {
        return true;
    }

    @Override
    public AbstractTrackableLoggingManager getTrackableLoggingManager(final AbstractLoggingActivity activity) {
        return new GeokretyLoggingManager(activity);
    }

    /**
     * Get geocode from GeoKrety id
     */
    public static String geocode(final int id) {
        return String.format("GK%04X", id);
    }

    @Override
    public boolean isLoggable() {
        return true;
    }

    @WorkerThread
    public static ImmutablePair<StatusCode, List<String>> postLogTrackable(final Context context, final Geocache cache, final TrackableLog trackableLog, final Calendar date, final String log) {
        // See doc: http://geokrety.org/api.php
        Log.d("GeokretyConnector.postLogTrackable: nr=" + trackableLog.trackCode);
        if (trackableLog.brand != TrackableBrand.GEOKRETY) {
            Log.d("GeokretyConnector.postLogTrackable: received invalid brand");
            return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, Collections.emptyList());
        }
        if (trackableLog.action == LogTypeTrackable.DO_NOTHING) {
            Log.d("GeokretyConnector.postLogTrackable: received invalid logtype");
            return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, Collections.emptyList());
        }
        try {
            // SecId is mandatory when using API, anonymous log are only possible via website
            final String secId = Settings.getGeokretySecId();
            if (StringUtils.isEmpty(secId)) {
                Log.d("GeokretyConnector.postLogTrackable: not authenticated");
                return new ImmutablePair<>(StatusCode.NO_LOGIN_INFO_STORED, Collections.emptyList());
            }
            // XXX: Use always CET timezone for Geokrety logging
            // See https://github.com/cgeo/cgeo/issues/9496
            date.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));

            // Construct Post Parameters
            final Parameters params = new Parameters(
                    "secid", secId,
                    "gzip", "0",
                    "nr", trackableLog.trackCode,
                    "formname", "ruchy",
                    "logtype", String.valueOf(trackableLog.action.gkid),
                    "data", String.format(Locale.ENGLISH, "%tY-%tm-%td", date, date, date), // YYYY-MM-DD
                    "godzina", String.format("%tH", date), // HH
                    "minuta", String.format("%tM", date), // MM
                    "comment", log,
                    "app", context.getString(R.string.app_name),
                    "app_ver", Version.getVersionName(context),
                    "mobile_lang", Settings.getApplicationLocale() + ".UTF-8"
            );

            // See doc: http://geokrety.org/help.php#acceptableformats
            if (cache != null) {
                final Geopoint coords = cache.getCoords();
                if (coords != null) {
                    params.add("latlon", coords.toString());
                }

                final String geocode = cache.getGeocode();
                if (StringUtils.isNotEmpty(geocode)) {
                    params.add("wpt", geocode);
                }
            }

            final String page = Network.getResponseData(Network.postRequest(URL + "/ruchy.php", params));
            if (page == null) {
                Log.d("GeokretyConnector.postLogTrackable: No data from server");
                return new ImmutablePair<>(StatusCode.CONNECTION_FAILED_GK, Collections.emptyList());
            }

            final ImmutablePair<Integer, List<String>> response = GeokretyParser.parseResponse(page);
            if (response == null) {
                Log.w("GeokretyConnector.postLogTrackable: Cannot parseResponse GeoKrety");
                return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, Collections.emptyList());
            }

            final List<String> errors = response.getRight();
            if (CollectionUtils.isNotEmpty(errors)) {
                for (final String error : errors) {
                    Log.w("GeokretyConnector.postLogTrackable: " + error);
                }
                return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, errors);
            }
            Log.i("Geokrety Log successfully posted to trackable #" + trackableLog.trackCode);
            return new ImmutablePair<>(StatusCode.NO_ERROR, Collections.emptyList());
        } catch (final RuntimeException e) {
            Log.w("GeokretyConnector.searchTrackable", e);
            return new ImmutablePair<>(StatusCode.LOG_POST_ERROR_GK, Collections.emptyList());
        }
    }

    public static String getCreateAccountUrl() {
        return URL + "/adduser.php";
    }
}
