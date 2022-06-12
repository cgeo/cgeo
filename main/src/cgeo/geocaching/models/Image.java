package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.text.WordUtils;

/**
 * Represent an Image along with Title and Description.
 * Mostly used for representing image in/from logs.
 */
public class Image implements Parcelable {

    public enum ImageCategory {
        UNCATEGORIZED(R.string.image_category_uncategorized),
        LISTING(R.string.image_category_listing),
        LOG(R.string.image_category_log),
        OWN(R.string.image_category_own);

        @StringRes
        private final int textId;

        ImageCategory(@StringRes final int textId) {
            this.textId = textId;
        }

        public String getI18n() {
            return LocalizationUtils.getStringWithFallback(textId, WordUtils.capitalizeFully(this.name()));
        }
    }

    /**
     * Static empty image, linked to nothing.
     */
    public static final Image NONE = new Image(Uri.EMPTY, null, null, -1, null, null);

    @NonNull public final Uri uri;
    @Nullable public final String title;
    public final int targetScale; //for offline log images
    @Nullable final String description;
    @NonNull public final ImageCategory category;
    @Nullable public final String contextInformation;

    private Pair<String, String> mimeInformation = null;

    /**
     * Helper class for building or manipulating Image references.
     *
     * Use #buildUpon() to obtain a builder representing an existing Image.
     */
    public static class Builder {
        @NonNull private Uri uri;
        @Nullable private String title;
        @Nullable private String description;
        private int targetScale; //needed for offline log images
        private ImageCategory category;
        private String contextInformation;

        /**
         * Create a new Image.
         */
        public Builder() {
            uri = Uri.EMPTY;
            title = null;
            description = null;
            targetScale = -1;
            category = ImageCategory.UNCATEGORIZED;
            contextInformation = null;
        }

        /**
         * Set image url from String.
         */
        @NonNull
        public Image build() {
            return new Image(uri, title, description, targetScale, category, contextInformation);
        }

        /**
         * Set image url from String.
         *
         * @param url The image url from String
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
         * @param uri The image url from Uri
         */
        @NonNull
        public Builder setUrl(@NonNull final Uri uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Set image from Image.
         *
         * @param image The image url from Image
         */
        @NonNull
        public Builder setUrl(@NonNull final Image image) {
            uri = image.uri;
            return this;
        }

        /**
         * Set image title.
         *
         * @param title The image title
         */
        @NonNull
        public Builder setTitle(@Nullable final String title) {
            this.title = title;
            return this;
        }

        /**
         * Set image description.
         *
         * @param description The image description
         */
        @NonNull
        public Builder setDescription(@Nullable final String description) {
            this.description = description;
            return this;
        }

        /**
         * For Offline Log Images: set wanted scaling factor when sending image to provider.
         */
        @NonNull
        public Builder setTargetScale(final int targetScale) {
            this.targetScale = targetScale;
            return this;
        }

        @NonNull
        public Builder setCategory(final ImageCategory category) {
            this.category = category;
            return this;
        }

        @NonNull
        public Builder setContextInformation(final String contextInformation) {
            this.contextInformation = contextInformation;
            return this;
        }
    }


    /**
     * Create a new Image from Url.
     *
     * @param uri         The image uri
     * @param title       The image title
     * @param description The image description
     */
    private Image(@NonNull final Uri uri, @Nullable final String title, @Nullable final String description, final int targetScale, final ImageCategory cat, @Nullable final String contextInformation) {
        this.uri = uri;
        this.title = title;
        this.description = description;
        this.targetScale = targetScale;
        this.category = cat == null ? ImageCategory.UNCATEGORIZED : cat;
        this.contextInformation = contextInformation;
    }

    private Image(@NonNull final Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        title = in.readString();
        description = in.readString();
        category = ImageCategory.values()[in.readInt()];
        contextInformation = in.readString();
        targetScale = in.readInt();
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
        dest.writeInt(category.ordinal());
        dest.writeString(contextInformation);
        dest.writeInt(targetScale);
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
     * @return A new Image Builder
     */
    public Builder buildUpon() {
        return new Builder()
                .setUrl(uri)
                .setTitle(title)
                .setDescription(description)
                .setTargetScale(targetScale)
                .setCategory(category)
                .setContextInformation(contextInformation);
    }

    /**
     * Get image title.
     *
     * @return the image title
     */
    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * Get image description.
     *
     * @return the image description
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Get the image Url.
     *
     * @return the image url
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
     * @return the image uri
     */
    @NonNull
    public Uri getUri() {
        return uri;
    }

    /**
     * Get the image filesystem path.
     *
     * @return the image url path
     */
    @NonNull
    public String getPath() {
        return isLocalFile() ? uri.getPath() : "";
    }

    public String getFileName() {
        return isLocalFile() ? getFile().getName() : "";
    }

    /**
     * Get the image as File.
     * If file is not local, return Null
     *
     * @return the image File
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
     * @return True if the image has a title
     */
    public boolean hasTitle() {
        return StringUtils.isNotBlank(title);
    }

    /**
     * Check if the image has a description.
     *
     * @return True if the image has a description
     */
    public boolean hasDescription() {
        return StringUtils.isNotBlank(description);
    }

    /**
     * Open the image in an external activity.
     * Do nothing if image url is empty.
     *
     * @param fromActivity The calling activity
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

    @Nullable
    public String getMimeType() {
        ensureMimeInformation();
        return mimeInformation.first;
    }

    @Nullable
    public String getMimeFileExtension() {
        ensureMimeInformation();
        return mimeInformation.second;
    }

    public boolean isImageOrUnknownUri() {
        final String mimeType = getMimeType();
        return mimeType == null || mimeType.startsWith("image/");
    }

    private void ensureMimeInformation() {
        if (mimeInformation == null) {
            mimeInformation = new Pair<>(UriUtils.getMimeType(getUri()), UriUtils.getMimeFileExtension(getUri()));
        }
    }


    /**
     * Check if the image Uri is Empty.
     *
     * @return True if Uri is Empty or blank
     */
    public boolean isEmpty() {
        return uri.equals(Uri.EMPTY) || StringUtils.isBlank(uri.toString());
    }

    /**
     * Compare two Images.
     *
     * @param o The Object to compare
     * @return True if all fields match
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
                && StringUtils.equals(description, image.description)
                && targetScale == image.targetScale
                && category == image.category;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(uri).append(title).append(description).build();
    }

    @NonNull
    @Override
    public String toString() {
        return "[Uri:" + uri + "/Title:" + title + "/Desc:" + description + "/cat:" + category + "/targetScale:" + targetScale + "]";
    }
}
