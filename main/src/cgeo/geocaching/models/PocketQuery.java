package cgeo.geocaching.models;

import java.util.Date;

public final class PocketQuery {

    private final String guid;
    private final int caches;
    private final String name;
    private final boolean downloadable;
    private final Date lastGeneration;
    private final int daysRemaining;

    public PocketQuery(final String guid, final String name, final int caches, final boolean downloadable, final Date lastGeneration, final int daysRemaining) {
        this.guid = guid;
        this.name = name;
        this.caches = caches;
        this.downloadable = downloadable;
        this.lastGeneration = lastGeneration;
        this.daysRemaining = daysRemaining;
    }

    public boolean isDownloadable() {
        return downloadable;
    }

    public String getGuid() {
        return guid;
    }

    public int getCaches() {
        return caches;
    }

    public String getName() {
        return name;
    }

    public Date getLastGeneration() {
        return lastGeneration;
    }

    public int getDaysRemaining() {
        return daysRemaining;
    }

}
