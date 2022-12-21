package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.capability.Smiley;
import cgeo.geocaching.connector.capability.SmileyCapability;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

public class OCConnector extends AbstractConnector implements SmileyCapability {

    @NonNull
    private final String host;
    private final boolean https;
    @NonNull
    private final String name;
    private final Pattern codePattern;
    private final String[] sqlLikeExpressions;
    private static final Pattern GPX_ZIP_FILE_PATTERN = Pattern.compile("oc[a-z]{2,3}\\d{5,}\\.zip", Pattern.CASE_INSENSITIVE);

    private static final List<LogType> STANDARD_LOG_TYPES = Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, LogType.NOTE);
    private static final List<LogType> EVENT_LOG_TYPES = Arrays.asList(LogType.WILL_ATTEND, LogType.ATTENDED, LogType.NOTE);
    @NonNull private final String abbreviation;

    public OCConnector(@NonNull final String name, @NonNull final String host, final boolean https, final String prefix, @NonNull final String abbreviation) {
        this.name = name;
        this.host = host;
        this.https = https;
        this.abbreviation = abbreviation;
        codePattern = Pattern.compile(prefix + "[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
        sqlLikeExpressions = new String[]{prefix + "%"};
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return codePattern.matcher(geocode).matches();
    }

    @NotNull
    @Override
    public String[] getGeocodeSqlLikeExpressions() {
        return sqlLikeExpressions;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return abbreviation;
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    @Nullable
    public String getCacheLogUrl(@NonNull final Geocache cache, final @NonNull LogEntry logEntry) {
        final String internalId = getServiceSpecificLogId(logEntry.serviceLogId);
        if (StringUtils.isNotBlank(internalId)) {
            return getCacheUrl(cache) + "&log=A#log" + internalId;
        }
        return null;
    }

    @Override
    public String getServiceSpecificLogId(final String logId) {
        //OC serviceLogId has format: 'log_uuid:internal_id', 'internal_id' may be missing
        //the id usable in other contexts is the internal_id
        if (StringUtils.isBlank(logId)) {
            return null;
        }
        final int idx = logId.lastIndexOf(":");
        if (idx >= 0) {
            return logId.substring(idx + 1);
        }
        return null; //do not display uuid
    }

    @Override
    @NonNull
    public String getHost() {
        return host;
    }

    @Override
    public boolean isZippedGPXFile(@NonNull final String fileName) {
        return GPX_ZIP_FILE_PATTERN.matcher(fileName).matches();
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return getSchemeAndHost() + "/viewcache.php?wp=";
    }

    @Override
    public int getCacheMapMarkerId() {
        return R.drawable.marker_oc;
    }

    @Override
    public int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_oc;
    }

    @Override
    public int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker_oc;
    }

    @Override
    public int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_oc;
    }

    @Override
    @NonNull
    public final List<LogType> getPossibleLogTypes(@NonNull final Geocache cache) {
        if (cache.isEventCache()) {
            return EVENT_LOG_TYPES;
        }

        return STANDARD_LOG_TYPES;
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        // different opencaching installations have different supported URLs

        // host.tld/geocode
        final String shortHost = getShortHost();
        final Uri uri = Uri.parse(url);
        if (!StringUtils.containsIgnoreCase(uri.getHost(), shortHost)) {
            return null;
        }
        final String path = uri.getPath();
        if (StringUtils.isBlank(path)) {
            return null;
        }
        final String firstLevel = path.substring(1);
        if (canHandle(firstLevel)) {
            return firstLevel;
        }

        // host.tld/viewcache.php?wp=geocode
        final String secondLevel = path.startsWith("/viewcache.php") ? uri.getQueryParameter("wp") : "";
        return (secondLevel != null && canHandle(secondLevel)) ? secondLevel : super.getGeocodeFromUrl(url);
    }

    @Override
    @Nullable
    public String getGeocodeFromText(@NonNull final String text) {
        // Text containing a Geocode
        return getGeocodeFromUrl(TextUtils.getMatch(text, Pattern.compile("((https?://|)(www.|)opencach[^\\s/$.?#].[^\\s]*)", Pattern.CASE_INSENSITIVE), false, ""));
    }

    @Override
    @Nullable
    public String getCreateAccountUrl() {
        return getSchemeAndHost() + "/register.php";
    }

    @Override
    public List<Smiley> getSmileys() {
        return OCSmileysProvider.getSmileys();
    }

    @Override
    @Nullable
    public Smiley getSmiley(final int id) {
        return OCSmileysProvider.getSmiley(id);
    }

    @Override
    public boolean isHttps() {
        return https;
    }

    /**
     * Return the scheme part including the colon and the slashes.
     *
     * @return either "https://" or "http://"
     */
    protected String getSchemePart() {
        return https ? "https://" : "http://";
    }

    /**
     * Return the scheme part and the host (e.g., "https://opencache.uk").
     */
    protected String getSchemeAndHost() {
        return getSchemePart() + host;
    }

    @Override
    @NonNull
    public List<UserAction> getUserActions(final UserAction.UAContext user) {
        final List<UserAction> actions = super.getUserActions(user);
        // caches stored before parsing the UserId will not have the field set, so we must check for correct existence here
        if (NumberUtils.isDigits(user.userName)) {
            actions.add(new UserAction(R.string.user_menu_open_browser, R.drawable.ic_menu_face, context -> ShareUtils.openUrl(context.getContext(), getSchemeAndHost() + "/viewprofile.php?userid=" + Network.encode(context.userName))));
            actions.add(new UserAction(R.string.user_menu_send_message, R.drawable.ic_menu_message, context -> ShareUtils.openUrl(context.getContext(), getSchemeAndHost() + "/mailto.php?userid=" + Network.encode(context.userName))));
        }
        return actions;
    }
}
