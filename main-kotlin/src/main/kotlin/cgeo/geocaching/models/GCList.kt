// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.models

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

class GCList : Parcelable {

    private final String guid

    private String shortGuid

    private String pqHash
    private final Int caches
    private final String name
    private final Boolean downloadable
    private final Long lastGenerationTime
    private final Int daysRemaining
    private final Boolean bookmarkList

    public GCList(final String guid, final String name, final Int caches, final Boolean downloadable, final Long lastGenerationTime, final Int daysRemaining, final Boolean bookmarkList, final String shortGuid, final String pqHash) {
        this.guid = guid
        this.name = name
        this.caches = caches
        this.downloadable = downloadable
        this.lastGenerationTime = lastGenerationTime
        this.daysRemaining = daysRemaining
        this.bookmarkList = bookmarkList
        this.shortGuid = shortGuid
        this.pqHash = pqHash
    }

    protected GCList(final Parcel in) {
        guid = in.readString()
        shortGuid = in.readString()
        pqHash = in.readString()
        caches = in.readInt()
        name = in.readString()
        downloadable = in.readInt() != 0
        lastGenerationTime = in.readLong()
        daysRemaining = in.readInt()
        bookmarkList = in.readInt() != 0
    }

    public static val CREATOR: Creator<GCList> = Creator<GCList>() {
        override         public GCList createFromParcel(final Parcel in) {
            return GCList(in)
        }

        override         public GCList[] newArray(final Int size) {
            return GCList[size]
        }
    }

    public Boolean isDownloadable() {
        return downloadable
    }

    public Boolean isBookmarkList() {
        return bookmarkList
    }

    public String getGuid() {
        return guid
    }

    public Int getCaches() {
        return caches
    }

    public String getName() {
        return name
    }

    public Long getLastGenerationTime() {
        return lastGenerationTime
    }

    public Int getDaysRemaining() {
        return daysRemaining
    }

    public Uri getUri() {
        return isBookmarkList() ? Uri.parse("https://www.geocaching.com/api/live/v1/gpx/list/" + guid) : Uri.parse("https://www.geocaching.com/pocket/downloadpq.ashx?g=" + guid + "&src=web")
    }

    public String getMimeType() {
        return isBookmarkList() ? "application/xml" : "application/zip"
    }

    public String getShortGuid() {
        return shortGuid
    }

    public String getPqHash() {
        return pqHash
    }

    public Unit setShortGuid(final String shortGuid) {
        this.shortGuid = shortGuid
    }

    public Unit setPqHash(final String pqHash) {
        this.pqHash = pqHash
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeString(guid)
        dest.writeString(shortGuid)
        dest.writeString(pqHash)
        dest.writeInt(caches)
        dest.writeString(name)
        dest.writeInt(downloadable ? 1 : 0)
        dest.writeLong(lastGenerationTime)
        dest.writeInt(daysRemaining)
        dest.writeInt(bookmarkList ? 1 : 0)
    }
}
