package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.files.LocalStorage;

import junit.framework.TestCase;

import android.content.Context;
import android.test.ServiceTestCase;

import java.io.File;
import java.io.IOException;

public class FileUtilsTest extends TestCase {

    final File testDir = LocalStorage.getStorageDir("automated-tests");
    final File baseFile = new File(testDir, "prefix.ext");
    final File alternative1 = new File(testDir, "prefix_1.ext");
    final File alternative2 = new File(testDir, "prefix_2.ext");

    private Context getTestContext() {
        try {
            return (Context) ServiceTestCase.class.getMethod("getTestContext").invoke(this);
        } catch (final Exception e) {
            Log.e("Unable to get test context", e);
            return null;
        }
    }

    public void testGetUniqueNamedFile() throws IOException {
        FileUtils.deleteDirectory(testDir);
        testDir.mkdirs();
        try {
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(baseFile);
            baseFile.createNewFile();
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(alternative1);
            alternative1.createNewFile();
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(alternative2);
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(alternative2);
        } finally {
            FileUtils.deleteDirectory(testDir);
        }
    }

    public void testFileUrl() {
        assertThat(FileUtils.isFileUrl("file:///tmp/foo/bar")).isTrue();
        assertThat(FileUtils.isFileUrl("http://www.google.com")).isFalse();
        assertThat(FileUtils.fileToUrl(new File("/tmp/foo/bar"))).isEqualTo("file:///tmp/foo/bar");
        assertThat(FileUtils.urlToFile("file:///tmp/foo/bar").getPath()).isEqualTo("/tmp/foo/bar");
    }

    public void testCreateRemoveDirectories() {
        FileUtils.deleteDirectory(testDir);
        assertThat(testDir.exists()).isFalse();
        FileUtils.mkdirs(testDir);
        assertThat(testDir.exists()).isTrue();
        FileUtils.deleteDirectory(testDir);
        assertThat(testDir.exists()).isFalse();
    }
}
