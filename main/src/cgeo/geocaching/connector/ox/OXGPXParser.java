package cgeo.geocaching.connector.ox;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.files.GPX10Parser;

public class OXGPXParser extends GPX10Parser {

    private final boolean isDetailed;

    public OXGPXParser(int listIdIn, boolean isDetailed) {
        super(listIdIn);
        this.isDetailed = isDetailed;
    }

    @Override
    protected void afterParsing(cgCache cache) {
        cache.setUpdated(System.currentTimeMillis());
        if (isDetailed) {
            cache.setDetailedUpdate(cache.getUpdated());
            cache.setDetailed(true);
        }
    }
}
