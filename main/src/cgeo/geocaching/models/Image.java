package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represent an Image along with Title and Description.
 * Mostly used for representing image in/from logs.
 */
public class Image implements Parcelable {

    /**
     * Static empty image, linked to nothing.
     */
    public static final Image NONE = new Image(Uri.EMPTY, null, null);

    /**
     * Helper class for building or manipulating Image references.
     *
     * Use #buildUpon() to obtain a builder representing an existing Image.
     */
    public static class Builder {
        @NonNull private Uri uri;
        @Nullable private String title;
        @Nullable private String description;

        /**
         * Create a new Image.
         *
         */
        public Builder() {
            uri = Uri.EMPTY;
            title = null;
            description = null;
        }

        /**
         * Set image url from String.
         *
         */
        @NonNull
        public Image build() {
            return new Image(uri, title, description);
        }

        /**
         * Set image url from String.
         *
         * @param url
         *          The image url from String
         */
        @NonNull
        public Builder setUrl(@NonNull final String url) {
            if (StringUtils.isEmpty(url)) {
                uri = Uri.EMPTY;
                return this;
            }

            // Assume uri has a scheme
            uri = Uri.parse(url);
            if (uri.isRelative()) {
                // If not the case treat it as a file
                uri = Uri.fromFile(new File(url));
            }
            return this;
        }

        /**
         * Set image from Uri.
         *
         * @param uri
         *          The image url from Uri
         */
        @NonNull
        public Builder setUrl(@NonNull final Uri uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Set image from File.
         *
         * @param file
         *          The image url from File
         */
        @NonNull
        public Builder setUrl(@NonNull final File file) {
            uri = Uri.fromFile(file);
            return this;
        }

        /**
         * Set image from Image.
         *
         * @param image
         *          The image url from Image
         */
        @NonNull
        public Builder setUrl(@NonNull final Image image) {
            uri = image.uri;
            return this;
        }

        /**
         * Set image title.
         *
         * @param title
         *          The image title
         */
        @NonNull
        public Builder setTitle(@Nullable final String title) {
            this.title = title;
            return this;
        }

        /**
         * Set image description.
         *
         * @param description
         *          The image description
         */
        @NonNull
        public Builder setDescription(@Nullable final String description) {
            this.description = description;
            return this;
        }
    }


    @NonNull public final Uri uri;
    @Nullable public final String title;
    @Nullable final String description;

    /**
     * Create a new Image from Url.
     *
     * @param uri
     *          The image uri
     * @param title
     *          The image title
     * @param description
     *          The image description
     */
    private Image(@NonNull final Uri uri, @Nullable final String title, @Nullable final String description) {
        this.uri = uri;
        this.title = title;
        this.description = description;
    }

    private Image(@NonNull final Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        title = in.readString();
        description = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(uri, 0);
        dest.writeString(title);
        dest.writeString(description);
    }

    public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {
        @Override
        public Image createFromParcel(final Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(final int size) {
            return new Image[size];
        }
    };

    /**
     * Constructs a new builder, copying the attributes from this Image.
     *
     * @return
     *          A new Image Builder
     */
    public Builder buildUpon() {
        return new Builder()
                .setUrl(uri)
                .setTitle(title)
                .setDescription(description);
    }

    /**
     * Get image title.
     *
     * @return
     *          the image title
     */
    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * Get image description.
     *
     * @return
     *          the image description
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Get the image Url.
     *
     * @return
     *          the image url
     */
    @NonNull
    public String getUrl() {
        if (isEmpty()) {
            return "";
        }
        return uri.toString();
    }

    /**
     * Get the image Uri.
     *
     * @return
     *          the image uri
     */
    @NonNull
    public Uri getUri() {
        return uri;
    }

    /**
     * Get the image filesystem path.
     *
     * @return
     *          the image url path
     */
    @NonNull
    public String getPath() {
        return isLocalFile() ? uri.getPath() : "";
    }

    /**
     * Get the image as File.
     * If file is not local, return Null
     *
     * @return
     *          the image File
     */
    @Nullable
    public File getFile() {
        if (isLocalFile()) {
            return new File(uri.getPath());
        }
        return null;
    }

    /**
     * Check if image has a title.
     *
     * @return
     *          True if the image has a title
     */
    public boolean hasTitle() {
        return StringUtils.isNotBlank(title);
    }

    /**
     * Check if the image has a description.
     *
     * @return
     *          True if the image has a description
     */
    public boolean hasDescription() {
        return StringUtils.isNotBlank(description);
    }

    /**
     * Open the image in an external activity.
     * Do nothing if image url is empty.
     *
     * @param fromActivity
     *          The calling activity
     */
    public void openInBrowser(final Activity fromActivity) {
        if (isEmpty()) {
            return;
        }
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            fromActivity.startActivity(browserIntent);
        } catch (final ActivityNotFoundException e) {
            Log.e("Cannot find suitable activity", e);
            ActivityMixin.showToast(fromActivity, R.string.err_application_no);
        }
    }

    /**
     * Check if the URL represents a file on the local file system.
     *
     * @return <tt>true</tt> if the URL scheme is <tt>file</tt>, <tt>false</tt> otherwise
     */
    public boolean isLocalFile() {
        return FileUtils.isFileUrl(getUrl());
    }

    /**
     * Local file name when {@link #isLocalFile()} is <tt>true</tt>.
     *
     * @return the local file
     */
    public File localFile() {
        return FileUtils.urlToFile(uri.toString());
    }

    /**
     * Check if the image exists locally.
     * Return False if Image is not local.
     * Todo: May check if we have a cached Image for remote Uri
     *
     * @return
     *          True if image exists on local filesystem
     */
    public boolean existsLocal() {
        if (!isLocalFile()) {
            return false;
        }
        return new File(getPath()).exists();
    }

    /**
     * Check if the image Uri is Empty.
     *
     * @return
     *          True if Uri is Empty or blank
     */
    public boolean isEmpty() {
        return uri.equals(Uri.EMPTY) || StringUtils.isBlank(uri.toString());
    }

    /**
     * Compare two Images.
     *
     * @param o
     *          The Object to compare
     * @return
     *          True if all fields match
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Image image = (Image) o;

        return uri.equals(image.uri)
                && StringUtils.equals(title, image.title)
                && StringUtils.equals(description, image.description);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(uri).append(title).append(description).build();
    }

    @Override
    public String toString() {
        return "[Uri:" + uri + "/Title:" + title + "/Desc:" + description + "]";
    }
}
