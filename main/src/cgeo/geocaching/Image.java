package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

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
     * Image builder class.
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
         * Create a new Image from Url.
         *
         * @param url
         *          The image url
         * @param title
         *          The image title
         * @param description
         *          The image description
         */
        public Builder(@Nullable final String url, @Nullable final String title, @Nullable final String description) {
            uri = url == null ? Uri.EMPTY : Uri.parse(url);
            this.title = title;
            this.description = description;
        }

        /**
         * Create a new Image from Url without description.
         *
         * @param url
         *          The image url
         * @param title
         *          The image title
         */
        public Builder(@Nullable final String url, @Nullable final String title) {
            this(url, title, null);
        }

        /**
         * Create a new Image from Uri without title and description.
         *
         * @param url
         *          The image Uri
         */
        public Builder(@Nullable final String url) {
            uri = StringUtils.isBlank(url) ? Uri.EMPTY : Uri.parse(url);
            title = null;
            description = null;
        }

        /**
         * Create a new Image from a File.
         * Set the filename as a title. No description.
         *
         * @param file
         *          The File image
         */
        public Builder(@Nullable final File file) {
            if (file == null) {
                uri = Uri.EMPTY;
                title = null;
            } else {
                uri = Uri.fromFile(file);
                title = file.getName();
            }
            description = null;
        }

        /**
         * Create a new Image from Uri without title and description.
         *
         * @param uri
         *          The image Uri
         */
        public Builder(@Nullable final Uri uri) {
            this.uri = uri == null ? Uri.EMPTY : uri;
            title = null;
            description = null;
        }

        /**
         * Create a new Image from Image.
         *
         * @param image
         *          The image Image
         */
        public Builder(@NonNull final Image image) {
            uri = image.uri;
            title = image.title;
            description = image.description;
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
        public void setUrl(final String url) {
            uri = Uri.parse(url);
        }

        /**
         * Set image from Uri.
         *
         * @param uri
         *          The image url from Uri
         */
        public void setUrl(final Uri uri) {
            this.uri = uri;
        }

        /**
         * Set image from File.
         *
         * @param file
         *          The image url from File
         */
        public void setUrl(final File file) {
            uri = Uri.fromFile(file);
        }

        /**
         * Set image title.
         *
         * @param title
         *          The image title
         */
        public void setTitle(@Nullable final String title) {
            this.title = title;
        }

        /**
         * Set image description.
         *
         * @param description
         *          The image description
         */
        public void setDescription(@Nullable final String description) {
            this.description = description;
        }
    }


    @NonNull final Uri uri;
    @Nullable final String title;
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
    public Image(@NonNull final Uri uri, @Nullable final String title, @Nullable final String description) {
        this.uri = uri;
        this.title = title;
        this.description = description;
    }

    public Image(@NonNull final Parcel in) {
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
            return new File(uri.toString());
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
        return FileUtils.isFileUrl(uri.toString());
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
}
