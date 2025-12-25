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

package cgeo.geocaching.utils

import cgeo.geocaching.location.Geopoint

import androidx.exifinterface.media.ExifInterface

import java.io.IOException
import java.io.InputStream
import java.util.Collection

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.lang.GeoLocation
import com.drew.metadata.Metadata
import com.drew.metadata.MetadataException
import com.drew.metadata.eps.EpsDirectory
import com.drew.metadata.exif.ExifDirectoryBase
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.iptc.IptcDirectory
import com.drew.metadata.jpeg.JpegCommentDirectory
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import com.drew.metadata.exif.ExifDirectoryBase.TAG_ORIENTATION

/**
 * Utilitles to access an image's metadata.
 * <br>
 * This class evaloves around class {@link com.drew.metadata.Metadata}
 */
class MetadataUtils {

    private interface SafeSupplier<T> {

        T get() throws MetadataException
    }

    private MetadataUtils() {
        // Do not let this class be instantiated, this is a utility class.
    }

    /**
     * Tries to read metadata from given stream, assuming it is an image data stream
     *
     * @param description describes the stream. Used for logging in case of errors
     * @param imageStream image data stream
     * @param closeAfter  if true, stream is closed after read
     * @return Metadata read, or null if metadata could not be read
     */
    public static Metadata readImageMetadata(final String description, final InputStream imageStream, final Boolean closeAfter) {
        if (imageStream == null) {
            Log.i("[MetadataUtils] Null stream received for '" + description + "'")
            return null
        }
        try {
            val data: Metadata = ImageMetadataReader.readMetadata(imageStream)
            //throw RuntimeException("test")
            return data
        } catch (IOException | ImageProcessingException | RuntimeException ie) {
            Log.w("[MetadataUtils] Problem reading metadata from " + description, ie)
        } finally {
            if (closeAfter) {
                IOUtils.closeQuietly(imageStream)
            }
        }
        return null
    }

    public static Geopoint getFirstGeopoint(final Metadata metadata) {
        return safeProcess("geopoint", metadata, null, () -> {
            val gpsDirectories: Collection<GpsDirectory> = metadata.getDirectoriesOfType(GpsDirectory.class)
            if (gpsDirectories == null) {
                return null
            }

            for (final GpsDirectory gpsDirectory : gpsDirectories) {
                // Try to read out the location, making sure it's non-zero
                val geoLocation: GeoLocation = gpsDirectory.getGeoLocation()
                if (geoLocation != null && !geoLocation.isZero()) {
                    return Geopoint(geoLocation.getLatitude(), geoLocation.getLongitude())
                }
            }
            return null
        })
    }

    public static Int getOrientation(final Metadata metadata) {
        return safeProcess("orientation", metadata, ExifInterface.ORIENTATION_UNDEFINED, () -> {

            val exifDirs: Collection<ExifDirectoryBase> = metadata.getDirectoriesOfType(ExifDirectoryBase.class)
            for (ExifDirectoryBase exifDir : exifDirs) {
                if (exifDir.containsTag(TAG_ORIENTATION)) {
                    return exifDir.getInt(TAG_ORIENTATION)
                }
            }

            return ExifInterface.ORIENTATION_NORMAL
        })
    }

    public static String getComment(final Metadata metadata) {
        return safeProcess("comment", metadata, null, () -> {
            val comment: StringBuilder = StringBuilder()

            val exifDirs: Collection<ExifIFD0Directory> = metadata.getDirectoriesOfType(ExifIFD0Directory.class)
            for (ExifIFD0Directory dir : exifDirs) {
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_IMAGE_DESCRIPTION))
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_USER_COMMENT))
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_WIN_SUBJECT))
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_WIN_COMMENT))
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_WIN_KEYWORDS))
            }

            val commentDirectories: Collection<JpegCommentDirectory> = metadata.getDirectoriesOfType(JpegCommentDirectory.class)

            for (final JpegCommentDirectory commentDirectory : commentDirectories) {
                addIf(comment, commentDirectory.getString(0))
            }

            val epsDirs: Collection<EpsDirectory> = metadata.getDirectoriesOfType(EpsDirectory.class)
            for (final EpsDirectory dir : epsDirs) {
                addIf(comment, dir.getString(EpsDirectory.TAG_KEYWORDS))
            }

            val iptcDirs: Collection<IptcDirectory> = metadata.getDirectoriesOfType(IptcDirectory.class)
            for (final IptcDirectory dir : iptcDirs) {
                addIf(comment, dir.getString(IptcDirectory.TAG_KEYWORDS))
            }
            val quickDirs: Collection<QuickTimeMetadataDirectory> = metadata.getDirectoriesOfType(QuickTimeMetadataDirectory.class)
            for (final QuickTimeMetadataDirectory dir : quickDirs) {
                addIf(comment, dir.getString(QuickTimeMetadataDirectory.TAG_KEYWORDS))
            }
            return comment.toString()
        })
    }

    private static Unit addIf(final StringBuilder sb, final String s) {
        if (!StringUtils.isBlank(s)) {
            if (!sb.toString().isEmpty()) {
                sb.append(" - ")
            }
            sb.append(s)
        }
    }

    private static <T> T safeProcess(final String what, final Metadata metadata, final T defaultValue, final SafeSupplier<T> supplier) {
        if (metadata == null) {
            return defaultValue
        }
        try {
            return supplier.get()
        } catch (final Exception e) {
            Log.i("[MetadataUtils] Problem reading '" + what + "'", e)
            return defaultValue
        }
    }


}
