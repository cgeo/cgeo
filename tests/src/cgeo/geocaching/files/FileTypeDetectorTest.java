package cgeo.geocaching.files;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import org.eclipse.jdt.annotation.NonNull;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

public class FileTypeDetectorTest extends AbstractResourceInstrumentationTestCase {

    private static class FileContentResolver extends ContentResolver {

        public FileContentResolver(Context context) {
            super(context);
        }
    }

    public void testUnknown() throws Exception {
        assertFileType(R.raw.gc2cjpf_html, FileType.UNKNOWN);
        assertFileType(R.raw.map1, FileType.UNKNOWN);
    }

    public void testLoc() throws Exception {
        assertFileType(R.raw.gc1bkp3_loc, FileType.LOC);
        assertFileType(R.raw.oc5952_loc, FileType.LOC);
        assertFileType(R.raw.waymarking_loc, FileType.LOC);
    }

    public void testGpx() throws Exception {
        assertFileType(R.raw.gc1bkp3_gpx100, FileType.GPX);
        assertFileType(R.raw.gc1bkp3_gpx101, FileType.GPX);
        assertFileType(R.raw.oc5952_gpx, FileType.GPX);
        assertFileType(R.raw.renamed_waypoints_wpts, FileType.GPX);
        assertFileType(R.raw.waymarking_gpx, FileType.GPX);
    }

    public void testZip() throws Exception {
        assertFileType(R.raw.pq_error, FileType.ZIP);
        assertFileType(R.raw.pq7545915, FileType.ZIP);
    }

    private void assertFileType(final int resourceId, final @NonNull FileType fileType) {
        final Uri resourceURI = getResourceURI(resourceId);
        final FileContentResolver contentResolver = new FileContentResolver(getInstrumentation().getContext());
        assertThat(new FileTypeDetector(resourceURI, contentResolver).getFileType()).isEqualTo(fileType);
    }
}
