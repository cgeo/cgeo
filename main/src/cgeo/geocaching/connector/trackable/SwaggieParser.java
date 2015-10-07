package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.utils.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.regex.Pattern;

final class SwaggieParser {

    private SwaggieParser() {
        // utility class
    }

    private static final Pattern PATTERN_NAME = Pattern.compile(Pattern.quote("<h1><a") + ".*?>(.*?)<");
    private static final Pattern PATTERN_GEOCODE = Pattern.compile(Pattern.quote("'/swaggie/") + "(.*?)'");
    private static final Pattern PATTERN_DESCRIPTION = Pattern.compile(Pattern.quote("'swaggie_description'>") + "(.*?)</div");
    private static final Pattern PATTERN_OWNER = Pattern.compile(">([^<]*?)</a> released");

    @Nullable
    public static Trackable parse(@NonNull final String page) {
        final Trackable trackable = new Trackable();
        final String name = TextUtils.getMatch(page, PATTERN_NAME, null);
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        trackable.setName(name);

        final String geocode = TextUtils.getMatch(page, PATTERN_GEOCODE, null);
        if (StringUtils.isEmpty(geocode)) {
            return null;
        }
        trackable.setGeocode(geocode);

        final String description = StringUtils.trim(TextUtils.getMatch(page, PATTERN_DESCRIPTION, StringUtils.EMPTY));
        if (StringUtils.isEmpty(description)) {
            return null;
        }
        trackable.setDetails(description);

        final String owner = StringUtils.trim(TextUtils.getMatch(page, PATTERN_OWNER, StringUtils.EMPTY));
        if (StringUtils.isEmpty(owner)) {
            return null;
        }
        trackable.setOwner(owner);

        trackable.setType("Swaggie");

        return trackable;
    }

}
