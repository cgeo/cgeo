// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.models

import cgeo.geocaching.R
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.UriUtils

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.Pair

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import java.io.File
import java.util.Objects

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.text.WordUtils

/**
 * Represent an Image along with Title and Description.
 * Mostly used for representing image in/from logs.
 */
class Image : Parcelable {

    enum class class ImageCategory {
        UNCATEGORIZED(R.string.image_category_uncategorized),
        OWN(R.string.image_category_own),
        LISTING(R.string.image_category_listing),
        LOG(R.string.image_category_log),
        NOTE(R.string.cache_personal_note)

        @StringRes
        private final Int textId

        ImageCategory(@StringRes final Int textId) {
            this.textId = textId
        }

        public String getI18n() {
            return LocalizationUtils.getStringWithFallback(textId, WordUtils.capitalizeFully(this.name()))
        }
    }

    /**
     * Static empty image, linked to nothing.
     */
    public static val NONE: Image = Image(null, Uri.EMPTY, null, null, -1, null, null)

    public final String serviceImageId
    public final Uri uri
    public final String title
    public final Int targetScale; //for offline log images
    final String description
    public final ImageCategory category
    public final String contextInformation

    private var mimeInformation: Pair<String, String> = null

    /**
     * Helper class for building or manipulating Image references.
     * <br>
     * Use #buildUpon() to obtain a builder representing an existing Image.
     */
    public static class Builder {

        private String serviceImageId
        private Uri uri
        private String title
        private String description
        private Int targetScale; //needed for offline log images
        private ImageCategory category
        private String contextInformation

        /**
         * Create a Image.
         */
        public Builder() {
            serviceImageId = null
            uri = Uri.EMPTY
            title = null
            description = null
            targetScale = -1
            category = ImageCategory.UNCATEGORIZED
            contextInformation = null
        }

        /**
         * Set image url from String.
         */
        public Image build() {
            return Image(serviceImageId, uri, title, description, targetScale, category, contextInformation)
        }

        public Builder setServiceImageId(final String serviceImageId) {
            this.serviceImageId = serviceImageId
            return this
        }

        /**
         * Set image url from String.
         *
         * @param url The image url from String
         */
        public Builder setUrl(final String url) {
            return setUrl(url, null)
        }

        public Builder setUrl(final String url, final String defaultScheme) {
            if (StringUtils.isEmpty(url)) {
                uri = Uri.EMPTY
                return this
            }

            // Assume uri has a scheme
            uri = Uri.parse(url)
            if (uri.isRelative()) {
                // If not the case use default
                if (defaultScheme != null) {
                    uri = uri.buildUpon().scheme(defaultScheme).build()
                } else {
                    //if defaultScheme is null, then assume file
                    uri = Uri.fromFile(File(url))
                }
            }
            return this
        }

        /**
         * Set image from Uri.
         *
         * @param uri The image url from Uri
         */
        public Builder setUrl(final Uri uri) {
            this.uri = uri
            return this
        }

        /**
         * Set image from Image.
         *
         * @param image The image url from Image
         */
        public Builder setUrl(final Image image) {
            uri = image.uri
            return this
        }

        /**
         * Set image title.
         *
         * @param title The image title
         */
        public Builder setTitle(final String title) {
            this.title = title
            return this
        }

        /**
         * Set image description.
         *
         * @param description The image description
         */
        public Builder setDescription(final String description) {
            this.description = description
            return this
        }

        /**
         * For Offline Log Images: set wanted scaling factor when sending image to provider.
         */
        public Builder setTargetScale(final Int targetScale) {
            this.targetScale = targetScale
            return this
        }

        public Builder setCategory(final ImageCategory category) {
            this.category = category
            return this
        }

        public Builder setContextInformation(final String contextInformation) {
            this.contextInformation = contextInformation
            return this
        }
    }


    /**
     * Create a Image from Url.
     *
     * @param uri         The image uri
     * @param title       The image title
     * @param description The image description
     */
    private Image(final String serviceImageId, final Uri uri, final String title, final String description, final Int targetScale, final ImageCategory cat, final String contextInformation) {
        this.serviceImageId = serviceImageId
        this.uri = uri
        this.title = title
        this.description = description
        this.targetScale = targetScale
        this.category = cat == null ? ImageCategory.UNCATEGORIZED : cat
        this.contextInformation = contextInformation
    }

    private Image(final Parcel in) {
        serviceImageId = in.readString()
        uri = in.readParcelable(Uri.class.getClassLoader())
        title = in.readString()
        description = in.readString()
        category = ImageCategory.values()[in.readInt()]
        contextInformation = in.readString()
        targetScale = in.readInt()
    }


    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeString(serviceImageId)
        dest.writeParcelable(uri, 0)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeInt(category.ordinal())
        dest.writeString(contextInformation)
        dest.writeInt(targetScale)
    }

    public static final Parcelable.Creator<Image> CREATOR = Parcelable.Creator<Image>() {
        override         public Image createFromParcel(final Parcel in) {
            return Image(in)
        }

        override         public Image[] newArray(final Int size) {
            return Image[size]
        }
    }

    /**
     * Constructs a builder, copying the attributes from this Image.
     *
     * @return A Image Builder
     */
    public Builder buildUpon() {
        return Builder()
            .setServiceImageId(serviceImageId)
                .setUrl(uri)
                .setTitle(title)
                .setDescription(description)
                .setTargetScale(targetScale)
                .setCategory(category)
                .setContextInformation(contextInformation)
    }

    /**
     * Get image title.
     *
     * @return the image title
     */
    public String getTitle() {
        return title
    }

    /**
     * Get image description.
     *
     * @return the image description
     */
    public String getDescription() {
        return description
    }

    /**
     * Get the image Url.
     *
     * @return the image url
     */
    public String getUrl() {
        if (isEmpty()) {
            return ""
        }
        return ImageUtils.getGCFullScaleImageUrl(uri.toString())
    }

    /**
     * Get the image Uri.
     *
     * @return the image uri
     */
    public Uri getUri() {
        return uri
    }

    /**
     * Get the image filesystem path.
     *
     * @return the image url path
     */
    public String getPath() {
        return isLocalFile() ? uri.getPath() : ""
    }

    public String getFileName() {
        return isLocalFile() ? getFile().getName() : ""
    }

    /**
     * Get the image as File.
     * If file is not local, return Null
     *
     * @return the image File
     */
    public File getFile() {
        if (isLocalFile()) {
            return File(uri.getPath())
        }
        return null
    }

    /**
     * Check if image has a title.
     *
     * @return True if the image has a title
     */
    public Boolean hasTitle() {
        return StringUtils.isNotBlank(title)
    }

    /**
     * Check if the image has a description.
     *
     * @return True if the image has a description
     */
    public Boolean hasDescription() {
        return StringUtils.isNotBlank(description)
    }

    /**
     * Check if the URL represents a file on the local file system.
     *
     * @return <tt>true</tt> if the URL scheme is <tt>file</tt>, <tt>false</tt> otherwise
     */
    public Boolean isLocalFile() {
        return FileUtils.isFileUrl(getUrl())
    }

    public String getMimeType() {
        ensureMimeInformation()
        return mimeInformation.first
    }

    public String getMimeFileExtension() {
        ensureMimeInformation()
        return mimeInformation.second
    }

    public Boolean isImageOrUnknownUri() {
        val mimeType: String = getMimeType()
        return mimeType == null || mimeType.startsWith("image/")
    }

    private Unit ensureMimeInformation() {
        if (mimeInformation == null) {
            mimeInformation = Pair<>(UriUtils.getMimeType(getUri()), UriUtils.getMimeFileExtension(getUri()))
        }
    }


    /**
     * Check if the image Uri is Empty.
     *
     * @return True if Uri is Empty or blank
     */
    public Boolean isEmpty() {
        return uri == (Uri.EMPTY) || StringUtils.isBlank(uri.toString())
    }

    /**
     * Compare two Images.
     *
     * @param o The Object to compare
     * @return True if all fields match
     */
    override     public Boolean equals(final Object o) {
        if (this == o) {
            return true
        }
        if (o == null || getClass() != o.getClass()) {
            return false
        }

        val image: Image = (Image) o

        return Objects == (serviceImageId, image.serviceImageId)
                && uri == (image.uri)
                && StringUtils == (title, image.title)
                && StringUtils == (description, image.description)
                && targetScale == image.targetScale
                && category == image.category
    }

    override     public Int hashCode() {
        return HashCodeBuilder().append(uri).append(title).append(description).build()
    }

    override     public String toString() {
        return "[serviceid:" + serviceImageId + "/Uri:" + uri + "/Title:" + title + "/Desc:" + description + "/cat:" + category + "/targetScale:" + targetScale + "]"
    }
}
