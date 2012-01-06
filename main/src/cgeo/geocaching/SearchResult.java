package cgeo.geocaching;

import cgeo.geocaching.enumerations.StatusCode;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SearchResult implements Parcelable {

    final protected Set<String> geocodes;
    public StatusCode error = null;
    public String url = "";
    public String[] viewstates = null;
    public int totalCnt = 0;

    final public static Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };

    public SearchResult() {
        this((Set<String>) null);
    }

    public SearchResult(SearchResult searchResult) {
        if (searchResult != null) {
            this.geocodes = new HashSet<String>(searchResult.geocodes);
            this.error = searchResult.error;
            this.url = searchResult.url;
            this.viewstates = searchResult.viewstates;
            this.totalCnt = searchResult.totalCnt;
        } else {
            this.geocodes = new HashSet<String>();
        }
    }

    public SearchResult(final Set<String> geocodes) {
        if (geocodes == null) {
            this.geocodes = new HashSet<String>();
        } else {
            this.geocodes = new HashSet<String>(geocodes.size());
            this.geocodes.addAll(geocodes);
        }
    }

    public SearchResult(final Parcel in) {
        ArrayList<String> list = new ArrayList<String>();
        in.readStringList(list);
        geocodes = new HashSet<String>(list);
        error = (StatusCode) in.readSerializable();
        url = in.readString();
        final int length = in.readInt();
        if (length >= 0) {
            viewstates = new String[length];
            in.readStringArray(viewstates);
        }
        totalCnt = in.readInt();
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        out.writeStringArray(geocodes.toArray(new String[geocodes.size()]));
        out.writeSerializable(error);
        out.writeString(url);
        if (viewstates == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(viewstates.length);
            out.writeStringArray(viewstates);
        }
        out.writeInt(totalCnt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Set<String> getGeocodes() {
        return Collections.unmodifiableSet(geocodes);
    }

    public int getCount() {
        return geocodes.size();
    }

    public boolean addGeocode(final String geocode) {
        return geocodes.add(geocode);
    }

    public static StatusCode getError(final SearchResult search) {
        if (search == null) {
            return null;
        }

        return search.error;
    }

    public static boolean setError(final SearchResult search, final StatusCode error) {
        if (search == null) {
            return false;
        }

        search.error = error;

        return true;
    }

    public static String getUrl(final SearchResult search) {
        if (search == null) {
            return null;
        }

        return search.url;
    }

    public static boolean setUrl(final SearchResult search, String url) {
        if (search == null) {
            return false;
        }

        search.url = url;

        return true;
    }

    public static String[] getViewstates(final SearchResult search) {
        if (search == null) {
            return null;
        }

        return search.viewstates;
    }

    public static boolean setViewstates(final SearchResult search, String[] viewstates) {
        if (cgBase.isEmpty(viewstates) || search == null) {
            return false;
        }

        search.viewstates = viewstates;

        return true;
    }

    public static int getTotal(final SearchResult search) {
        if (search == null) {
            return 0;
        }

        return search.totalCnt;
    }

    public static int getCount(final SearchResult search) {
        if (search == null) {
            return 0;
        }

        return search.getCount();
    }

}
