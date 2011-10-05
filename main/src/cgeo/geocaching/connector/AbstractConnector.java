package cgeo.geocaching.connector;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgeoapplication;

import android.os.Handler;

import java.util.UUID;

public abstract class AbstractConnector implements IConnector {

    @Override
    public boolean canHandle(String geocode) {
        return false;
    }

    @Override
    public boolean supportsRefreshCache(cgCache cache) {
        return false;
    }

    @Override
    public boolean supportsWatchList() {
        return false;
    }

    @Override
    public boolean supportsLogging() {
        return false;
    }

    @Override
    public String getLicenseText(final cgCache cache) {
        return null;
    }

    @Override
    public boolean supportsUserActions() {
        return false;
    }

    @Override
    public boolean supportsCachesAround() {
        return false;
    }

    @Override
    public UUID searchByGeocode(cgBase base, String geocode, String guid, cgeoapplication app, cgSearch search, int reason, Handler handler) {
        return null;
    }
}
