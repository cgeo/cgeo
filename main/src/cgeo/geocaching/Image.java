package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

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

    public Image(final File file) {
        this("file://" + file.getAbsolutePath(), file.getName(), null);
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

    public void openInBrowser(final Activity fromActivity) {
        if (StringUtils.isBlank(url)) {
            return;
        }
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            fromActivity.startActivity(browserIntent);
        } catch (final ActivityNotFoundException e) {
            Log.e("Cannot find suitable activity", e);
            ActivityMixin.showToast(fromActivity, R.string.err_application_no);
        }
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

    /**
     * Check if the URL represents a file on the local file system.
     *
     * @return <tt>true</tt> if the URL scheme is <tt>file</tt>, <tt>false</tt> otherwise
     */
    public boolean isLocalFile() {
        return FileUtils.isFileUrl(url);
    }

    /**
     * Local file name when {@link #isLocalFile()} is <tt>true</tt>.
     *
     * @return the local file
     */
    public File localFile() {
        return FileUtils.urlToFile(url);
    }
}
