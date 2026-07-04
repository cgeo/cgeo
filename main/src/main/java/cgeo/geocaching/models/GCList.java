package cgeo.geocaching.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class GCList implements Parcelable {

    private final String guid;

    private String shortGuid;

    private String pqHash;
    private final int caches;
    private final String name;
    private final boolean downloadable;
    private final long lastGenerationTime;
    private final int daysRemaining;
    private final boolean bookmarkList;
    private final boolean ownUnpublished;

    public GCList(final String guid, final String name, final int caches, final boolean downloadable, final long lastGenerationTime, final int daysRemaining, final boolean bookmarkList, final String shortGuid, final String pqHash) {
        this(guid, name, caches, downloadable, lastGenerationTime, daysRemaining, bookmarkList, shortGuid, pqHash, false);
    }

    private GCList(final String guid, final String name, final int caches, final boolean downloadable, final long lastGenerationTime, final int daysRemaining, final boolean bookmarkList, final String shortGuid, final String pqHash, final boolean ownUnpublished) {
        this.guid = guid;
        this.name = name;
        this.caches = caches;
        this.downloadable = downloadable;
        this.lastGenerationTime = lastGenerationTime;
        this.daysRemaining = daysRemaining;
        this.bookmarkList = bookmarkList;
        this.shortGuid = shortGuid;
        this.pqHash = pqHash;
        this.ownUnpublished = ownUnpublished;
    }

    /** Pseudo list which fetches the caches the user owns but which are not (yet, or no longer) published. */
    public static GCList createOwnUnpublished(final String name) {
        return new GCList(null, name, -1, false, 0, -1, false, null, null, true);
    }

    protected GCList(final Parcel in) {
        guid = in.readString();
        shortGuid = in.readString();
        pqHash = in.readString();
        caches = in.readInt();
        name = in.readString();
        downloadable = in.readInt() != 0;
        lastGenerationTime = in.readLong();
        daysRemaining = in.readInt();
        bookmarkList = in.readInt() != 0;
        ownUnpublished = in.readInt() != 0;
    }

    public static final Creator<GCList> CREATOR = new Creator<GCList>() {
        @Override
        public GCList createFromParcel(final Parcel in) {
            return new GCList(in);
        }

        @Override
        public GCList[] newArray(final int size) {
            return new GCList[size];
        }
    };

    public boolean isDownloadable() {
        return downloadable;
    }

    public boolean isBookmarkList() {
        return bookmarkList;
    }

    public boolean isOwnUnpublished() {
        return ownUnpublished;
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

    public String getMimeType() {
        return isBookmarkList() ? "application/xml" : "application/zip";
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(guid);
        dest.writeString(shortGuid);
        dest.writeString(pqHash);
        dest.writeInt(caches);
        dest.writeString(name);
        dest.writeInt(downloadable ? 1 : 0);
        dest.writeLong(lastGenerationTime);
        dest.writeInt(daysRemaining);
        dest.writeInt(bookmarkList ? 1 : 0);
        dest.writeInt(ownUnpublished ? 1 : 0);
    }
}
