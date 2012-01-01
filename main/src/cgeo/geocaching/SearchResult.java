package cgeo.geocaching;

import cgeo.geocaching.enumerations.StatusCode;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchResult implements Parcelable {

    final private List<String> geocodes;
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
        this((List<String>) null);
    }

    public SearchResult(SearchResult searchResult) {
        if (searchResult != null) {
            this.geocodes = new ArrayList<String>(searchResult.geocodes);
            this.error = searchResult.error;
            this.url = searchResult.url;
            this.viewstates = searchResult.viewstates;
            this.totalCnt = searchResult.totalCnt;
        } else {
            this.geocodes = new ArrayList<String>();
        }
    }

    public SearchResult(final List<String> geocodes) {
        if (geocodes == null) {
            this.geocodes = new ArrayList<String>();
        } else {
            this.geocodes = new ArrayList<String>(geocodes.size());
            this.geocodes.addAll(geocodes);
        }
    }

    public SearchResult(final Parcel in) {
        geocodes = new ArrayList<String>();
        in.readStringList(geocodes);
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
        out.writeStringList(geocodes);
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

    public List<String> getGeocodes() {
        return Collections.unmodifiableList(geocodes);
    }

    public int getCount() {
        return geocodes.size();
    }

    public void addGeocode(final String geocode) {
        geocodes.add(geocode);
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
