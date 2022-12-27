package cgeo.geocaching.utils;

import cgeo.geocaching.storage.LocalStorage;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FileUtilsTest {

    final File testDir = LocalStorage.getGeocacheDataDirectory("automated-tests");

    @Test
    public void testGetUniqueNamedFile() throws IOException {
        FileUtils.deleteDirectory(testDir);
        assertThat(testDir.mkdirs()).isTrue();
        try {
            final File baseFile = new File(testDir, "prefix.ext");
            final File alternative1 = new File(testDir, "prefix_2.ext");
            final File alternative2 = new File(testDir, "prefix_3.ext");

            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(baseFile);
            assertThat(baseFile.createNewFile()).isTrue();
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(alternative1);
            assertThat(alternative1.createNewFile()).isTrue();
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(alternative2);
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(alternative2);
        } finally {
            FileUtils.deleteDirectory(testDir);
        }
    }

    @Test
    public void testFileUrl() {
        assertThat(FileUtils.isFileUrl("file:///tmp/foo/bar")).isTrue();
        assertThat(FileUtils.isFileUrl("http://www.google.com")).isFalse();
        assertThat(FileUtils.fileToUrl(new File("/tmp/foo/bar"))).isEqualTo("file:///tmp/foo/bar");
        assertThat(FileUtils.urlToFile("file:///tmp/foo/bar").getPath()).isEqualTo("/tmp/foo/bar");
    }

    @Test
    public void testCreateRemoveDirectories() {
        FileUtils.deleteDirectory(testDir);
        assertThat(testDir).doesNotExist();
        FileUtils.mkdirs(testDir);
        assertThat(testDir).exists();
        FileUtils.deleteDirectory(testDir);
        assertThat(testDir).doesNotExist();
    }

    @Test
    public void testGetExtension() {
        assertThat(FileUtils.getExtension("foo/bar/xyzzy")).isEqualTo("");
        assertThat(FileUtils.getExtension("foo/bar/xyzzy.jpg")).isEqualTo(".jpg");
        assertThat(FileUtils.getExtension("foo/bar/xyzzy.jpeg")).isEqualTo(".jpeg");
        assertThat(FileUtils.getExtension("foo/bar/xyzzy.mjpeg")).isEqualTo("");
    }

}
