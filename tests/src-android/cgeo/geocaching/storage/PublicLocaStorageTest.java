package cgeo.geocaching.storage;

import cgeo.CGeoTestCase;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.content.UriPermission;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class PublicLocaStorageTest extends CGeoTestCase {

    //a first small test to see how CI handles it
    public static void testCreateDelete() throws IOException {

        //try to get this info into the jenkins log
        final List<UriPermission> uriPerms = CgeoApplication.getInstance().getApplicationContext().getContentResolver().getPersistedUriPermissions();
        final StringBuilder body = new StringBuilder();
        body.append("\nPersisted Uri Permissions: #").append(uriPerms.size());
        for (UriPermission uriPerm : uriPerms) {
            body.append("\n- ").append(UriUtils.uriPermissionToString(uriPerm));
        }

        System.out.println("Via System.out: " + body);
        Log.iForce("Via Log.iForce: " + body);

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
