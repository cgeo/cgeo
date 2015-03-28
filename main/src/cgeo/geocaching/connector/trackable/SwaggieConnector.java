package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.enumerations.TrackableBrand;
import cgeo.geocaching.network.Network;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.regex.Pattern;

public final class SwaggieConnector extends AbstractTrackableConnector {

    private static final Pattern PATTERN_SW_CODE = Pattern.compile("SW[0-9]{4}");

    @Override
    public boolean canHandleTrackable(final String geocode) {
        return geocode != null && PATTERN_SW_CODE.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        return getUrl(trackable.getGeocode());
    }

    @Override
    @Nullable
    public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        final String page = Network.getResponseData(Network.getRequest(getUrl(geocode)));
        if (page == null) {
            return null;
        }
        return SwaggieParser.parse(page);
    }

    @Override
    @Nullable
    public String getTrackableCodeFromUrl(@NonNull final String url) {
        final String geocode = StringUtils.upperCase(StringUtils.substringAfterLast(url, "swaggie/"));
        if (canHandleTrackable(geocode)) {
            return geocode;
        }
        return null;
    }

    private static String getUrl(final String geocode) {
        return "http://geocaching.com.au/swaggie/" + geocode;
    }

    @Override
    public TrackableBrand getBrand() {
        return TrackableBrand.SWAGGIE;
    }
}
