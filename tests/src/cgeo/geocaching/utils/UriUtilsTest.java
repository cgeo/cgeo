package cgeo.geocaching.utils;

import android.net.Uri;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class UriUtilsTest {

    @Test
    public void getUriName() {

        //A typical example of a non-trivial SAF Document Uri as returned by the framework
        //for tjis ecxample. Uri.getLastPathSegment gives you: primary:Documents/cgeo/logfiles/logcat_2020-12-28_17-22-20-2.txt
        final String uriExample = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Fcgeo/document/primary%3ADocuments%2Fcgeo%2Flogfiles%2Flogcat_2020-12-28_17-22-20-2.txt";
        final Uri uri = Uri.parse(uriExample);
        assertThat(UriUtils.getFileName(uri)).isEqualTo("logcat_2020-12-28_17-22-20-2.txt");

        //a little stability test
        assertThat(UriUtils.getFileName(null)).isEqualTo("");
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
    public void toStringDecoded() {
        final String uriExample = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Fcgeo/document/primary%3ADocuments%2Fcgeo%2Flogfiles%2Flogcat_2020-12-28_17-22-20-2.txt";
        final String uriExampleDecoded = "content://com.android.externalstorage.documents/tree/primary:Documents/cgeo/document/primary:Documents/cgeo/logfiles/logcat_2020-12-28_17-22-20-2.txt";

        assertThat(UriUtils.toStringDecoded(Uri.parse(uriExample))).isEqualTo(uriExampleDecoded);
    }

}
