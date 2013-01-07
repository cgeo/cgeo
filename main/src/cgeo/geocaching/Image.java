package cgeo.geocaching;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Image implements Parcelable {
    private final String url;
    private final String title;
    private final String description;

    public Image(final String url, final String title, final String description) {
        this.url = url;
        this.title = title;
        this.description = description;
    }

    public Image(final String url, final String title) {
        this(url, title, null);
    }

    public Image(final Parcel in) {
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

    public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void openInBrowser(final Context fromActivity) {
        if (StringUtils.isBlank(url)) {
            return;
        }
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        fromActivity.startActivity(browserIntent);
    }

    @Override
    public String toString() {
        if (null != title) {
            return title;
        }

        if (url != null) {
            return url;
        }

        return "???";
    }
}
