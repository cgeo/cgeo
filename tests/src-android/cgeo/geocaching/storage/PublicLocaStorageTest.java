package cgeo.geocaching.storage;

import cgeo.CGeoTestCase;
import cgeo.geocaching.utils.FileNameCreator;

import android.net.Uri;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class PublicLocaStorageTest extends CGeoTestCase {

    //a first small test to see how CI handles it
    public static void testCreateDelete() throws IOException {

        //a test dir is needed
        final File tempFile = File.createTempFile("abcdef", "tmp");
        final File tempFolder = tempFile.getParentFile();
        final FolderLocation tempLocation = FolderLocation.fromFile(tempFolder);

        final Uri uri = PublicLocalStorage.get().create("TEST", tempLocation, FileNameCreator.forName("cgeo-test.txt"));

        assertThat(PublicLocalStorage.get().delete(uri)).isTrue();

        //cleanup
        assertThat(tempFile.delete()).isTrue();

    }
}
