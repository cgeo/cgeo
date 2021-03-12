/**
 * static helper class for handling datafiles
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.mapaccess.StorageConfigHelper;

import java.io.File;

public final class RoutingHelper {

    private RoutingHelper() {
        // utility class
    }

    public static File getAdditionalMaptoolDir(final String segmentDir) {
        return StorageConfigHelper.getAdditionalMaptoolDir(segmentDir);
    }

    public static File getSecondarySegmentDir(final String segmentDir) {
        return StorageConfigHelper.getSecondarySegmentDir(segmentDir);
    }


    public static boolean hasDirectoryAnyDatafiles(final String segmentDir) {
        if (hasAnyDatafiles(new File(segmentDir))) {
            return true;
        }
        // check secondary, too
        final File secondary = StorageConfigHelper.getSecondarySegmentDir(segmentDir);
        if (secondary != null) {
            return hasAnyDatafiles(secondary);
        }
        return false;
    }

    private static boolean hasAnyDatafiles(final File dir) {
        final String[] fileNames = dir.list();
        for (String fileName : fileNames) {
            if (fileName.endsWith(".rd5")) {
                return true;
            }
        }
        return false;
    }
}
