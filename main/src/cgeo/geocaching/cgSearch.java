package cgeo.geocaching;

import cgeo.geocaching.enumerations.StatusCode;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class cgSearch implements Parcelable {

    final private List<String> geocodes;
    public StatusCode error = null;
    public String url = "";
    public String[] viewstates = null;
    public int totalCnt = 0;

    final public static Parcelable.Creator<cgSearch> CREATOR = new Parcelable.Creator<cgSearch>() {
        public cgSearch createFromParcel(Parcel in) {
            return new cgSearch(in);
        }

        public cgSearch[] newArray(int size) {
            return new cgSearch[size];
        }
    };

    public cgSearch() {
        this((List<String>) null);
    }

    public cgSearch(final List<String> geocodes) {
        if (geocodes == null) {
            this.geocodes = new ArrayList<String>();
        } else {
            this.geocodes = new ArrayList<String>(geocodes.size());
            this.geocodes.addAll(geocodes);
        }
    }

    public cgSearch(final Parcel in) {
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

}
