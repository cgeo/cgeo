package cgeo.geocaching.utils;

import android.net.Uri;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class UriUtilsTest {

    //Example for folder /cgeo
    private static final String DOC_URI_EXAMPLE = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Fcgeo/document/primary%3ADocuments%2Fcgeo%2Flogfiles%2Flogcat_2020-12-28_17-22-20-2.txt";

    private static final String DOC_URI_EXAMPLE_DECODED = "content://com.android.externalstorage.documents/tree/primary:Documents/cgeo/document/primary:Documents/cgeo/logfiles/logcat_2020-12-28_17-22-20-2.txt";

    //real-world Uri examples and their wanted display
    //Each test case has entries "Description", "expected display Uri", "raw Uri
    public static final String[][] URI_DISPLAY_TESTS = new String[][]{
            new String[]{"Internal /cgeo", "/cgeo", "content://com.android.externalstorage.documents/tree/primary%3Acgeo"},
            new String[]{"Internal /cgeo/maps", "/cgeo/maps", "content://com.android.externalstorage.documents/tree/primary%3Acgeo/document/primary%3Acgeo%2Fmaps"},
            new String[]{"Internal /cgeo/logfiles", "/cgeo/logfiles", "content://com.android.externalstorage.documents/tree/primary%3Acgeo%2Flogfiles"},
            new String[]{"Internal file in /cgeo/logfiles", "/cgeo/logfiles/log.txt", "content://com.android.externalstorage.documents/tree/primary%3Acgeo%2Flogfiles%2Flog.txt"},
            new String[]{"SDCARD /cgeo with known volume id", "TEST-SDCARD/cgeo", "content://com.android.externalstorage.documents/tree/TEST-1111-2222%3Acgeo"},
            new String[]{"SDCARD /Music/cgeo with unknown volume id", "TEST-UNKNOWNID/Music/cgeo", "content://com.android.externalstorage.documents/tree/TEST-UNKNOWNID%3AMusic%2Fcgeo"},
            new String[]{"SDCARD /cgeo with unknown volume id", "TEST-UNKNOWNID/cgeo", "content://com.android.externalstorage.documents/tree/TEST-UNKNOWNID%3Acgeo"},
            new String[]{"Internal /Documents/cgeo (special home handling)", "[Documents]/cgeo", "content://com.android.externalstorage.documents/tree/home%3Acgeo"},
            new String[]{"Download root (special provider handling)", "Downloads:/downloads", "content://com.android.providers.downloads.documents/tree/downloads"},
            new String[]{"Download /cgeo (special provider handling)", "Downloads:raw/storage/emulated/0/Download/cgeo", "content://com.android.providers.downloads.documents/tree/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2Fcgeo"},
            new String[]{"Download /cgeo (provider handling + numbers)", "Downloads:msd/29", "content://com.android.providers.downloads.documents/tree/msd%3A29"},
            new String[]{"File", "/storage/emulated/0/cgeo", "file:///storage/emulated/0/cgeo"},
            new String[]{"A null uri", "---", null},
            new String[]{"An empty uri", "---", ""},
            new String[]{"A strange uri", "/strange", "content://////strange"},
            new String[]{"A public web uri", "http://www.cgeo.org/test", "http://www.cgeo.org/test"},
    };

    @Test
    public void getUserDisplayableUri() {
        int idx = 1;
        for (String[] uriDisplayTestCase : URI_DISPLAY_TESTS) {
            final Uri uri = uriDisplayTestCase[2] == null ? null : Uri.parse(uriDisplayTestCase[2]);
            assertThat(UriUtils.toUserDisplayableString(uri)).as("Test Case " + idx + " of " + URI_DISPLAY_TESTS.length + ": " + uriDisplayTestCase[0]).isEqualTo(uriDisplayTestCase[1]);
            idx++;
        }
    }

    @Test
    public void getLastPathSegment() {

        //A typical example of a non-trivial SAF Document Uri as returned by the framework
        //for tjis ecxample. Uri.getLastPathSegment gives you: primary:Documents/cgeo/logfiles/logcat_2020-12-28_17-22-20-2.txt
        final Uri uri = Uri.parse(DOC_URI_EXAMPLE);
        assertThat(UriUtils.getLastPathSegment(uri)).isEqualTo("logcat_2020-12-28_17-22-20-2.txt");

        //a little stability test
        assertThat(UriUtils.getLastPathSegment(null)).isNull();
    }

    @Test
    public void filesAndUris() throws IOException {

        //A non-file uri
        final Uri nonFileUri = Uri.parse("http://www.cgeo.org");
        assertThat(UriUtils.isFileUri(nonFileUri)).isFalse();
        assertThat(UriUtils.toFile(nonFileUri)).isNull();

        //a File Uri
        final File someFile = File.createTempFile("tempfileforunittest", "tmp");
        final Uri fileUri = Uri.fromFile(someFile);
        assertThat(UriUtils.isFileUri(fileUri)).isTrue();
        assertThat(UriUtils.toFile(fileUri)).isEqualTo(someFile);

        assertThat(someFile.delete()).isTrue();
    }

    @Test
    public void contentAndUris() {
        assertThat(UriUtils.isContentUri(Uri.parse(DOC_URI_EXAMPLE))).isTrue();
        assertThat(UriUtils.isContentUri(Uri.parse("/eins/zwei/drei"))).isFalse();
        assertThat(UriUtils.isContentUri(Uri.parse("http://www.cgeo.org"))).isFalse();
    }

    @Test
    public void appendPath() {
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "eins/zwei").toString()).isEqualTo(DOC_URI_EXAMPLE + "/eins/zwei");
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "/eins/zwei/").toString()).isEqualTo(DOC_URI_EXAMPLE + "/eins/zwei");
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "  /eins/zwei  ").toString()).isEqualTo(DOC_URI_EXAMPLE + "/eins/zwei");
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "   ").toString()).isEqualTo(DOC_URI_EXAMPLE);
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), null).toString()).isEqualTo(DOC_URI_EXAMPLE);
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "  ///  ").toString()).isEqualTo(DOC_URI_EXAMPLE);
    }

    @Test
    public void toStringDecoded() {
        assertThat(UriUtils.toCompareString(Uri.parse(DOC_URI_EXAMPLE))).isEqualTo(DOC_URI_EXAMPLE_DECODED);
    }

    @Test
    public void pseudoTreeUris() {
        assertThat(UriUtils.getPseudoTreeUriForFileUri(Uri.parse("file:///storage/emulated/0/cgeo"))).isEqualTo(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Acgeo/document/primary%3Acgeo"));
        assertThat(UriUtils.getPseudoTreeUriForFileUri(Uri.parse("file:///storage/14FA-2B11/cgeo"))).isEqualTo(Uri.parse("content://com.android.externalstorage.documents/tree/14FA-2B11%3Acgeo/document/14FA-2B11%3Acgeo"));
        assertThat(UriUtils.getPseudoTreeUriForFileUri(Uri.parse("file:///storage/14FA-2B11/cgeo_subdir/real_base"))).isEqualTo(Uri.parse("content://com.android.externalstorage.documents/tree/14FA-2B11%3Acgeo_subdir%2Freal_base/document/14FA-2B11%3Acgeo_subdir%2Freal_base"));
        assertThat(UriUtils.getPseudoTreeUriForFileUri(Uri.parse("file:///storage/emulated/0/internal_subdir/another_subdir/cgeo_base"))).isEqualTo(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Ainternal_subdir%2Fanother_subdir%2Fcgeo_base/document/primary%3Ainternal_subdir%2Fanother_subdir%2Fcgeo_base"));
        //content uri stays content uri
        assertThat(UriUtils.getPseudoTreeUriForFileUri(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Acgeo/document/primary%3Acgeo"))).isEqualTo(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Acgeo/document/primary%3Acgeo"));
        //strangely formatted file uris stay as they are (no exception)
        assertThat(UriUtils.getPseudoTreeUriForFileUri(Uri.parse("file:///storage"))).isEqualTo(Uri.parse("file:///storage"));
    }
}
