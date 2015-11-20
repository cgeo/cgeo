package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

class GeocachingSuConnector extends AbstractConnector {

    private static final CharSequence PREFIX_MULTISTEP_VIRTUAL = "MV";
    private static final CharSequence PREFIX_TRADITIONAL = "TR";
    private static final CharSequence PREFIX_VIRTUAL = "VI";
    private static final CharSequence PREFIX_MULTISTEP = "MS";
    private static final CharSequence PREFIX_EVENT = "EV";
    private static final CharSequence PREFIX_CONTEST = "CT";

    @Override
    @NonNull
    public String getName() {
        return "Geocaching.su";
    }

    @Override
    @NonNull
    public String getCacheUrl(final @NonNull Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.geocaching.su";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean canHandle(final @NonNull String geocode) {
        return (StringUtils.startsWithAny(StringUtils.upperCase(geocode), PREFIX_TRADITIONAL, PREFIX_MULTISTEP_VIRTUAL, PREFIX_VIRTUAL, PREFIX_MULTISTEP, PREFIX_EVENT, PREFIX_CONTEST)) && isNumericId(geocode.substring(2));
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return "http://" + getHost() + "/?pn=101";
    }

}
