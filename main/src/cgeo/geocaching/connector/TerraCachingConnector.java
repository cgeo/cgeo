package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.regex.Pattern;

class TerraCachingConnector extends AbstractConnector {

    @NonNull private final static Pattern PATTERN_GEOCODE = Pattern.compile("TC[0-9A-Z]{1,3}", Pattern.CASE_INSENSITIVE);

    @Override
    @NonNull
    public String getName() {
        return "TerraCaching";
    }

    @Override
    @Nullable
    public String getCacheUrl(@NonNull final Geocache cache) {
        return null;
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.terracaching.com/";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return "http://www.TerraCaching.com/viewcache.cgi?C=";
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return PATTERN_GEOCODE.matcher(geocode).matches();
    }
}
