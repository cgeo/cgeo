package cgeo.geocaching.utils;

import cgeo.geocaching.location.Geopoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import org.apache.commons.io.IOUtils;

/**
 * Utilitles to access an image's metadata.
 *
 * This class evaloves around class {@link com.drew.metadata.Metadata}
 */
public final class MetadataUtils {

    private MetadataUtils() {
        // Do not let this class be instantiated, this is a utility class.
    }

    /**
     * Tries to read metadata from given stream, assuming it is an image data stream
     * @param description describes the stream. Used for logging in case of errors
     * @param imageStream image data stream
     * @param closeAfter if true, stream is closed after read
     * @return Metadata read, or null if metadata could not be read
     */
    public static Metadata readImageMetadata(final String description, final InputStream imageStream, final boolean closeAfter) {
        if (imageStream == null) {
            Log.i("[MetadataUtils] Null stream received for '" + description + "'");
            return null;
        }
        try {
            return ImageMetadataReader.readMetadata(imageStream);
        } catch (IOException | ImageProcessingException ie) {
            Log.w("[MetadataUtils] Problem reading metadata from " + description, ie);
        } finally {
            if (closeAfter) {
                IOUtils.closeQuietly(imageStream);
            }
        }
        return null;
    }

    public static Geopoint getFirstGeopoint(final Metadata metadata) {
        if (metadata == null) {
            return null;
        }
        try {
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
        } catch (final Exception e) {
            Log.i("[MetadataUtils] Problem reading coordinates", e);
        }
        return null;

    }

}
