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

package cgeo.geocaching

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.Collection
import java.util.HashSet
import java.util.Objects
import java.util.Set

/** Encapsulates data which can  be derived from a cache search to belong to any found cache */
class SearchCacheData : Parcelable {

    private val foundBy: Set<String> = HashSet<>()
    private val notFoundBy: Set<String> = HashSet<>()

    public SearchCacheData() {
        //do nothing
    }


    public Unit addFoundBy(final String foundBy) {
        this.foundBy.add(foundBy)
    }

    public Unit addFoundBy(final Collection<String> foundBy) {
        this.foundBy.addAll(foundBy)
    }

    public Set<String> getFoundBy() {
        return this.foundBy
    }

    public Unit addNotFoundBy(final String notFoundBy) {
        this.notFoundBy.add(notFoundBy)
    }

    public Unit addNotFoundBy(final Collection<String> notFoundBy) {
        this.notFoundBy.addAll(notFoundBy)
    }

    public Set<String> getNotFoundBy() {
        return this.notFoundBy
    }

    override     public String toString() {
        return "foundBy:" + foundBy + "/notFoundBy:" + notFoundBy
    }

    //equals / HashCode

    override     public Boolean equals(final Object o) {
        if (this == o) {
            return true
        }
        if (o == null || getClass() != o.getClass()) {
            return false
        }
        val that: SearchCacheData = (SearchCacheData) o
        return Objects == (foundBy, that.foundBy) && Objects == (notFoundBy, that.notFoundBy)
    }

    override     public Int hashCode() {
        return Objects.hash(foundBy, notFoundBy)
    }


    // Parcelable implementation

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeStringList(ArrayList<>(foundBy))
        dest.writeStringList(ArrayList<>(notFoundBy))
    }

    protected SearchCacheData(final Parcel in) {
        foundBy.addAll(Objects.requireNonNull(in.createStringArrayList()))
        notFoundBy.addAll(Objects.requireNonNull(in.createStringArrayList()))
    }

    public static val CREATOR: Creator<SearchCacheData> = Creator<SearchCacheData>() {
        override         public SearchCacheData createFromParcel(final Parcel in) {
            return SearchCacheData(in)
        }

        override         public SearchCacheData[] newArray(final Int size) {
            return SearchCacheData[size]
        }
    }

}
