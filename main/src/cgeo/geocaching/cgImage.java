package cgeo.geocaching;

import android.os.Parcel;
import android.os.Parcelable;

public class cgImage implements Parcelable {
    public String url = "";
    public String title = "";
    public String description = "";

    public cgImage() {
    }

    public cgImage(final Parcel in) {
        url = in.readString();
        title = in.readString();
        description = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(title);
        dest.writeString(description);
    }

    public static final Parcelable.Creator<cgImage> CREATOR = new Parcelable.Creator<cgImage>() {
        public cgImage createFromParcel(Parcel in) {
            return new cgImage(in);
        }

        @Override
        public cgImage[] newArray(int size) {
            return new cgImage[size];
        }
    };
}
