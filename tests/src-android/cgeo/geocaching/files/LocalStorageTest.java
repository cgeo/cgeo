package cgeo.geocaching.files;

import static org.assertj.core.api.Assertions.assertThat;

import android.test.AndroidTestCase;

public class LocalStorageTest extends AndroidTestCase {

    public static void testGetExtension() {
        assertThat(LocalStorage.getExtension("foo/bar/xyzzy")).isEqualTo("");
        assertThat(LocalStorage.getExtension("foo/bar/xyzzy.jpg")).isEqualTo(".jpg");
        assertThat(LocalStorage.getExtension("foo/bar/xyzzy.jpeg")).isEqualTo(".jpeg");
        assertThat(LocalStorage.getExtension("foo/bar/xyzzy.mjpeg")).isEqualTo("");
    }

}
