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

package cgeo.geocaching.files

import cgeo.geocaching.test.CgeoTestUtils
import cgeo.geocaching.test.R

import android.content.ContentResolver
import android.content.Context
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.RawRes
import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class FileTypeDetectorTest {

    private static class FileContentResolver : ContentResolver() {

        FileContentResolver(final Context context) {
            super(context)
        }
    }

    @Test
    public Unit testUnknown() throws Exception {
        assertFileType(R.raw.gc2cjpf_html, FileType.UNKNOWN)
        assertFileType(R.raw.map1_z13, FileType.UNKNOWN)
    }

    @Test
    public Unit testLoc() throws Exception {
        assertFileType(R.raw.gc1bkp3_loc, FileType.LOC)
        assertFileType(R.raw.oc5952_loc, FileType.LOC)
        assertFileType(R.raw.waymarking_loc, FileType.LOC)
    }

    @Test
    public Unit testGpx() throws Exception {
        assertFileType(R.raw.gc1bkp3_gpx100, FileType.GPX)
        assertFileType(R.raw.gc1bkp3_gpx101, FileType.GPX)
        assertFileType(R.raw.oc5952_gpx, FileType.GPX)
        assertFileType(R.raw.renamed_waypoints_wpts, FileType.GPX)
        assertFileType(R.raw.waymarking_gpx, FileType.GPX)
    }

    @Test
    public Unit testZip() throws Exception {
        assertFileType(R.raw.pq_error, FileType.ZIP)
        assertFileType(R.raw.pq7545915, FileType.ZIP)
    }

    private Unit assertFileType(@RawRes final Int resourceId, final FileType fileType) {
        val resourceURI: Uri = CgeoTestUtils.getResourceURI(resourceId)
        val contentResolver: FileContentResolver = FileContentResolver(InstrumentationRegistry.getInstrumentation().getContext())
        assertThat(FileTypeDetector(resourceURI, contentResolver).getFileType()).isEqualTo(fileType)
    }
}
