package cgeo.geocaching.utils;

import cgeo.geocaching.location.Geopoint;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.eps.EpsDirectory;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.jpeg.JpegCommentDirectory;
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import static com.drew.metadata.exif.ExifDirectoryBase.TAG_ORIENTATION;

/**
 * Utilitles to access an image's metadata.
 * <br>
 * This class evaloves around class {@link com.drew.metadata.Metadata}
 */
public final class MetadataUtils {

    private interface SafeSupplier<T> {

        T get() throws MetadataException;
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
    public static Metadata readImageMetadata(final String description, final InputStream imageStream, final boolean closeAfter) {
        if (imageStream == null) {
            Log.i("[MetadataUtils] Null stream received for '" + description + "'");
            return null;
        }
        try {
            return ImageMetadataReader.readMetadata(imageStream);
        } catch (IOException | ImageProcessingException | RuntimeException ie) {
            Log.w("[MetadataUtils] Problem reading metadata from " + description, ie);
        } finally {
            if (closeAfter) {
                IOUtils.closeQuietly(imageStream);
            }
        }
        return null;
    }

    public static Geopoint getFirstGeopoint(final Metadata metadata) {
        return safeProcess("geopoint", metadata, null, () -> {
            final Collection<GpsDirectory> gpsDirectories = metadata.getDirectoriesOfType(GpsDirectory.class);
            if (gpsDirectories == null) {
                return null;
            }

            for (final GpsDirectory gpsDirectory : gpsDirectories) {
                // Try to read out the location, making sure it's non-zero
                final GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    return new Geopoint(geoLocation.getLatitude(), geoLocation.getLongitude());
                }
            }
            return null;
        });
    }

    public static int getOrientation(final Metadata metadata) {
        return safeProcess("orientation", metadata, ExifInterface.ORIENTATION_NORMAL, () -> {

            final Collection<ExifDirectoryBase> exifDirs = metadata.getDirectoriesOfType(ExifDirectoryBase.class);
            for (ExifDirectoryBase exifDir : exifDirs) {
                if (exifDir.containsTag(TAG_ORIENTATION)) {
                    return exifDir.getInt(TAG_ORIENTATION);
                }
            }

            return ExifInterface.ORIENTATION_NORMAL;
        });
    }

    public static String getComment(final Metadata metadata) {
        return safeProcess("comment", metadata, null, () -> {
            final StringBuilder comment = new StringBuilder();

            final Collection<ExifIFD0Directory> exifDirs = metadata.getDirectoriesOfType(ExifIFD0Directory.class);
            for (ExifIFD0Directory dir : exifDirs) {
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_IMAGE_DESCRIPTION));
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_USER_COMMENT));
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_WIN_SUBJECT));
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_WIN_COMMENT));
                addIf(comment, dir.getString(ExifDirectoryBase.TAG_WIN_KEYWORDS));
            }

            final Collection<JpegCommentDirectory> commentDirectories = metadata.getDirectoriesOfType(JpegCommentDirectory.class);

            for (final JpegCommentDirectory commentDirectory : commentDirectories) {
                addIf(comment, commentDirectory.getString(0));
            }

            final Collection<EpsDirectory> epsDirs = metadata.getDirectoriesOfType(EpsDirectory.class);
            for (final EpsDirectory dir : epsDirs) {
                addIf(comment, dir.getString(EpsDirectory.TAG_KEYWORDS));
            }

            final Collection<IptcDirectory> iptcDirs = metadata.getDirectoriesOfType(IptcDirectory.class);
            for (final IptcDirectory dir : iptcDirs) {
                addIf(comment, dir.getString(IptcDirectory.TAG_KEYWORDS));
            }
            final Collection<QuickTimeMetadataDirectory> quickDirs = metadata.getDirectoriesOfType(QuickTimeMetadataDirectory.class);
            for (final QuickTimeMetadataDirectory dir : quickDirs) {
                addIf(comment, dir.getString(QuickTimeMetadataDirectory.TAG_KEYWORDS));
            }
            return comment.toString();
        });
    }

    private static void addIf(final StringBuilder sb, final String s) {
        if (!StringUtils.isBlank(s)) {
            if (!sb.toString().isEmpty()) {
                sb.append(" - ");
            }
            sb.append(s);
        }
    }

    private static <T> T safeProcess(final String what, final Metadata metadata, final T defaultValue, final SafeSupplier<T> supplier) {
        if (metadata == null) {
            return defaultValue;
        }
        try {
            return supplier.get();
        } catch (final Exception e) {
            Log.i("[MetadataUtils] Problem reading '" + what + "'", e);
            return defaultValue;
        }
    }


}
