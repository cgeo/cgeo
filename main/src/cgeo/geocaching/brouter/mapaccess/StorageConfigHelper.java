/**
 * Access to the storageconfig.txt config file
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class StorageConfigHelper {
    private StorageConfigHelper() {
        // utility class
    }

    public static File getSecondarySegmentDir(final String segmentDir) {
        return getStorageLocation(segmentDir, "secondary_segment_dir=");
    }

    public static File getAdditionalMaptoolDir(final String segmentDir) {
        return getStorageLocation(segmentDir, "additional_maptool_dir=");
    }

    private static File getStorageLocation(final String segmentDir, final String tag) {
        File res = null;
        BufferedReader br = null;
        final String configFile = segmentDir + "/storageconfig.txt";
        try {
            br = new BufferedReader(new FileReader(configFile));
            for (; ; ) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith(tag)) {
                    final String path = line.substring(tag.length()).trim();
                    res = path.startsWith("/") ? new File(path) : new File(new File(segmentDir), path);
                    if (!res.exists()) {
                        res = null;
                    }
                    break;
                }
            }
        } catch (Exception e) { /* ignore */ } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ee) { /* ignore */ }
            }
        }
        return res;
    }

}
