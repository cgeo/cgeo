package cgeo.geocaching.models;

import android.net.Uri;

public final class GCList {

    private final String guid;

    private String shortGuid;

    private String pqHash;
    private final int caches;
    private final String name;
    private final boolean downloadable;
    private final long lastGenerationTime;
    private final int daysRemaining;
    private final boolean bookmarkList;

    public GCList(final String guid, final String name, final int caches, final boolean downloadable, final long lastGenerationTime, final int daysRemaining, final boolean bookmarkList, final String shortGuid, final String pqHash) {
        this.guid = guid;
        this.name = name;
        this.caches = caches;
        this.downloadable = downloadable;
        this.lastGenerationTime = lastGenerationTime;
        this.daysRemaining = daysRemaining;
        this.bookmarkList = bookmarkList;
        this.shortGuid = shortGuid;
        this.pqHash = pqHash;
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
        return isBookmarkList() ? Uri.parse("https://www.geocaching.com/api/live/v1/gpx/list/" + guid) : Uri.parse("https://www.geocaching.com/pocket/downloadpq.ashx?g=" + guid + "&src=web");
    }

    public String getShortGuid() {
        return shortGuid;
    }

    public String getPqHash() {
        return pqHash;
    }

    public void setShortGuid(final String shortGuid) {
        this.shortGuid = shortGuid;
    }

    public void setPqHash(final String pqHash) {
        this.pqHash = pqHash;
    }

}
