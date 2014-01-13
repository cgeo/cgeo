package cgeo.geocaching.connector.ox;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.files.GPX10Parser;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

public class OXGPXParser extends GPX10Parser {

    private final boolean isDetailed;

    public OXGPXParser(int listIdIn, boolean isDetailed) {
        super(listIdIn);
        this.isDetailed = isDetailed;
    }

    @Override
    protected void afterParsing(Geocache cache) {
        cache.setUpdated(System.currentTimeMillis());
        if (isDetailed) {
            cache.setDetailedUpdate(cache.getUpdated());
            cache.setDetailed(true);
        }
        removeTitleFromShortDescription(cache);
    }

    /**
     * The short description of OX caches contains "title by owner, type(T/D/Awesomeness)". That is a lot of
     * duplication.
     *
     * @param cache
     */
    private static void removeTitleFromShortDescription(final @NonNull Geocache cache) {
        cache.setShortDescription(StringUtils.trim(StringUtils.substringAfterLast(cache.getShortDescription(), ",")));
    }
}
