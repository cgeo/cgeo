package cgeo.geocaching.models;

import android.net.Uri;

public final class GCList {

    private final String guid;
    private final int caches;
    private final String name;
    private final boolean downloadable;
    private final long lastGenerationTime;
    private final int daysRemaining;
    private final boolean bookmarkList;

    public GCList(final String guid, final String name, final int caches, final boolean downloadable, final long lastGenerationTime, final int daysRemaining, final boolean bookmarkList) {
        this.guid = guid;
        this.name = name;
        this.caches = caches;
        this.downloadable = downloadable;
        this.lastGenerationTime = lastGenerationTime;
        this.daysRemaining = daysRemaining;
        this.bookmarkList = bookmarkList;
    }

    public boolean isDownloadable() {
        return downloadable;
    }

    public boolean isBookmarkList() {
        return bookmarkList;
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
        return isBookmarkList() ? Uri.parse("https://www.geocaching.com/plan/api/gpx/list/" + guid) : Uri.parse("https://www.geocaching.com/pocket/downloadpq.ashx?g=" + guid + "&src=web");
    }

}
