package cgeo.geocaching.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class GCList implements Parcelable {

    /** reference code (bmCode) used by geocaching.com for the "special" ignored list at /plan/lists/ignored */
    public static final String SPECIAL_LIST_IGNORED_CODE = "ignored";
    /** reference code (bmCode) used by geocaching.com for the "special" favorites list at /plan/lists/favorites */
    public static final String SPECIAL_LIST_FAVORITES_CODE = "favorites";

    private final String guid;

    private String shortGuid;

    private String pqHash;
    private final int caches;
    private final String name;
    private final boolean downloadable;
    private final long lastGenerationTime;
    private final int daysRemaining;
    private final boolean bookmarkList;
    private final boolean specialList;

    public GCList(final String guid, final String name, final int caches, final boolean downloadable, final long lastGenerationTime, final int daysRemaining, final boolean bookmarkList, final String shortGuid, final String pqHash) {
        this(guid, name, caches, downloadable, lastGenerationTime, daysRemaining, bookmarkList, shortGuid, pqHash, false);
    }

    public GCList(final String guid, final String name, final int caches, final boolean downloadable, final long lastGenerationTime, final int daysRemaining, final boolean bookmarkList, final String shortGuid, final String pqHash, final boolean specialList) {
        this.guid = guid;
        this.name = name;
        this.caches = caches;
        this.downloadable = downloadable;
        this.lastGenerationTime = lastGenerationTime;
        this.daysRemaining = daysRemaining;
        this.bookmarkList = bookmarkList;
        this.shortGuid = shortGuid;
        this.pqHash = pqHash;
        this.specialList = specialList;
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
        specialList = in.readInt() != 0;
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

    /**
     * A "special" list is a geocaching.com list which is not a real (user-created) bookmark list, but a
     * virtual one such as the "ignored" or "favorites" list found at https://www.geocaching.com/plan/lists/.
     * Such lists have no real GUID and can't be queried through the regular bookmark list API, therefore they
     * need to be fetched and parsed differently (see {@link cgeo.geocaching.connector.gc.GCParser#searchBySpecialList}).
     */
    public boolean isSpecialList() {
        return specialList;
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
        dest.writeInt(specialList ? 1 : 0);
    }
}
