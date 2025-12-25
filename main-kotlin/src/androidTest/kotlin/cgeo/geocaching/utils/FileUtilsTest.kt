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

package cgeo.geocaching.utils

import cgeo.geocaching.storage.LocalStorage

import java.io.File
import java.io.IOException

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class FileUtilsTest {

    val testDir: File = LocalStorage.getGeocacheDataDirectory("automated-tests")

    @Test
    public Unit testGetUniqueNamedFile() throws IOException {
        FileUtils.deleteDirectory(testDir)
        assertThat(testDir.mkdirs()).isTrue()
        try {
            val baseFile: File = File(testDir, "prefix.ext")
            val alternative1: File = File(testDir, "prefix_2.ext")
            val alternative2: File = File(testDir, "prefix_3.ext")

            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(baseFile)
            assertThat(baseFile.createNewFile()).isTrue()
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(alternative1)
            assertThat(alternative1.createNewFile()).isTrue()
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(alternative2)
            assertThat(FileUtils.getUniqueNamedFile(baseFile)).isEqualTo(alternative2)
        } finally {
            FileUtils.deleteDirectory(testDir)
        }
    }

    @Test
    public Unit testFileUrl() {
        assertThat(FileUtils.isFileUrl("file:///tmp/foo/bar")).isTrue()
        assertThat(FileUtils.isFileUrl("http://www.google.com")).isFalse()
        assertThat(FileUtils.fileToUrl(File("/tmp/foo/bar"))).isEqualTo("file:///tmp/foo/bar")
        assertThat(FileUtils.urlToFile("file:///tmp/foo/bar").getPath()).isEqualTo("/tmp/foo/bar")
    }

    @Test
    public Unit testCreateRemoveDirectories() {
        FileUtils.deleteDirectory(testDir)
        assertThat(testDir).doesNotExist()
        FileUtils.mkdirs(testDir)
        assertThat(testDir).exists()
        FileUtils.deleteDirectory(testDir)
        assertThat(testDir).doesNotExist()
    }

    @Test
    public Unit testGetExtension() {
        assertThat(FileUtils.getExtension("foo/bar/xyzzy")).isEqualTo("")
        assertThat(FileUtils.getExtension("foo/bar/xyzzy.jpg")).isEqualTo(".jpg")
        assertThat(FileUtils.getExtension("foo/bar/xyzzy.jpeg")).isEqualTo(".jpeg")
        assertThat(FileUtils.getExtension("foo/bar/xyzzy.mjpeg")).isEqualTo("")
    }

}
