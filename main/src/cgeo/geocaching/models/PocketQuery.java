package cgeo.geocaching.models;

import android.net.Uri;

public final class PocketQuery {

    private final String guid;
    private final int caches;
    private final String name;
    private final boolean downloadable;
    private final long lastGenerationTime;
    private final int daysRemaining;

    public PocketQuery(final String guid, final String name, final int caches, final boolean downloadable, final long lastGenerationTime, final int daysRemaining) {
        this.guid = guid;
        this.name = name;
        this.caches = caches;
        this.downloadable = downloadable;
        this.lastGenerationTime = lastGenerationTime;
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

    public long getLastGenerationTime() {
        return lastGenerationTime;
    }

    public int getDaysRemaining() {
        return daysRemaining;
    }

    public Uri getUri() {
        return Uri.parse("https://www.geocaching.com/pocket/downloadpq.ashx?g=" + guid + "&src=web");
    }

}
