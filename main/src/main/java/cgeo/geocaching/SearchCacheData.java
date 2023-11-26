package cgeo.geocaching;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Encapsulates data which can  be derived from a cache search to belong to any found cache */
public class SearchCacheData implements Parcelable {

    private final Set<String> foundBy = new HashSet<>();
    private final Set<String> notFoundBy = new HashSet<>();

    public SearchCacheData() {
        //do nothing
    }


    public void addFoundBy(final String foundBy) {
        this.foundBy.add(foundBy);
    }

    public void addFoundBy(final Collection<String> foundBy) {
        this.foundBy.addAll(foundBy);
    }

    public Set<String> getFoundBy() {
        return this.foundBy;
    }

    public void addNotFoundBy(final String notFoundBy) {
        this.notFoundBy.add(notFoundBy);
    }

    public void addNotFoundBy(final Collection<String> notFoundBy) {
        this.notFoundBy.addAll(notFoundBy);
    }

    public Set<String> getNotFoundBy() {
        return this.notFoundBy;
    }

    @NonNull
    @Override
    public String toString() {
        return "foundBy:" + foundBy + "/notFoundBy:" + notFoundBy;
    }

    //equals / HashCode

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SearchCacheData that = (SearchCacheData) o;
        return Objects.equals(foundBy, that.foundBy) && Objects.equals(notFoundBy, that.notFoundBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foundBy, notFoundBy);
    }


    // Parcelable implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeStringList(new ArrayList<>(foundBy));
        dest.writeStringList(new ArrayList<>(notFoundBy));
    }

    protected SearchCacheData(final Parcel in) {
        foundBy.addAll(Objects.requireNonNull(in.createStringArrayList()));
        notFoundBy.addAll(Objects.requireNonNull(in.createStringArrayList()));
    }

    public static final Creator<SearchCacheData> CREATOR = new Creator<SearchCacheData>() {
        @Override
        public SearchCacheData createFromParcel(final Parcel in) {
            return new SearchCacheData(in);
        }

        @Override
        public SearchCacheData[] newArray(final int size) {
            return new SearchCacheData[size];
        }
    };

}
