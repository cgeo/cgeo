package cgeo.geocaching.connector.ec;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class ECConnector extends AbstractConnector {

    @NonNull
    private static final String CACHE_URL = "https://extremcaching.com/index.php/output-2/";

    /**
     * Pattern for EC codes
     */
    @NonNull
    private static final Pattern PATTERN_EC_CODE = Pattern.compile("EC[0-9]+", Pattern.CASE_INSENSITIVE);

    private ECConnector() {
        // singleton
        prefKey = R.string.preference_screen_ec;
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull private static final ECConnector INSTANCE = new ECConnector();
    }

    @NonNull
    public static ECConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return PATTERN_EC_CODE.matcher(geocode).matches();
    }

    @NonNull
    @Override
    public String[] getGeocodeSqlLikeExpressions() {
        return new String[]{"EC%"};
    }


    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return CACHE_URL + cache.getGeocode().replace("EC", "");
    }

    @Override
    @NonNull
    public String getName() {
        return "extremcaching.com";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return "EC";
    }

    @Override
    @NonNull
    public String getHost() {
        return "extremcaching.com";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return CACHE_URL;
    }

    @Override
    public boolean isActive() {
        return Settings.isECConnectorActive();
    }

    @Override
    public int getCacheMapMarkerId() {
        final String icons = Settings.getECIconSet();
        if (StringUtils.equals(icons, "1")) {
            return R.drawable.marker_other;
        }
        return R.drawable.marker_oc;
    }

    @Override
    public int getCacheMapMarkerBackgroundId() {
        final String icons = Settings.getECIconSet();
        if (StringUtils.equals(icons, "1")) {
            return R.drawable.background_other;
        }
        return R.drawable.background_oc;
    }

    @Override
    public int getCacheMapDotMarkerId() {
        final String icons = Settings.getECIconSet();
        if (StringUtils.equals(icons, "1")) {
            return R.drawable.dot_marker_other;
        }
        return R.drawable.dot_marker_oc;
    }

    @Override
    public int getCacheMapDotMarkerBackgroundId() {
        final String icons = Settings.getECIconSet();
        if (StringUtils.equals(icons, "1")) {
            return R.drawable.dot_background_other;
        }
        return R.drawable.dot_background_oc;
    }

    @Override
    @NonNull
    public String getLicenseText(@NonNull final Geocache cache) {
        // NOT TO BE TRANSLATED
        return "© " + cache.getOwnerDisplayName() + ", <a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a>, CC BY-NC-ND 3.0, alle Logeinträge © jeweiliger Autor";
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes(@NonNull final Geocache geocache) {
        final List<LogType> logTypes = new ArrayList<>();
        if (geocache.isEventCache()) {
            logTypes.add(LogType.WILL_ATTEND);
            logTypes.add(LogType.ATTENDED);
        } else {
            logTypes.add(LogType.FOUND_IT);
        }
        if (!geocache.isEventCache()) {
            logTypes.add(LogType.DIDNT_FIND_IT);
        }
        logTypes.add(LogType.NOTE);
        return logTypes;
    }

    @Override
    public int getMaxTerrain() {
        return 7;
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final String geocode = "EC" + StringUtils.substringAfter(url, "extremcaching.com/index.php/output-2/");
        if (canHandle(geocode)) {
            return geocode;
        }
        return super.getGeocodeFromUrl(url);
    }
}
