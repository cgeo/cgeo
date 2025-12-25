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

package cgeo.geocaching.storage

import android.net.Uri

import java.io.File

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class FolderTest {

    private static val DOC_URI_EXAMPLE: String = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Fcgeo/document/primary%3ADocuments%2Fcgeo%2Flogfiles%2Flogcat_2020-12-28_17-22-20-2.txt"
    private static val DOC_URI_EXAMPLE_DECODED: String = "content://com.android.externalstorage.documents/tree/primary:Documents/cgeo/document/primary:Documents/cgeo/logfiles/logcat_2020-12-28_17-22-20-2.txt"

    @Test
    public Unit testEquals() {
        val folder1: Folder = Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE))
        val folder2: Folder = Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE))
        val folder3: Folder = Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE_DECODED))
        val folderDifferent: Folder = Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE.substring(0, DOC_URI_EXAMPLE.length() - 10)))

        assertThat(folder1).isEqualTo(folder1)
        assertThat(folder1).isEqualTo(folder2)
        assertThat(folder1).isEqualTo(folder3)
        assertThat(folder1).isNotEqualTo(folderDifferent)
    }

    @Test
    public Unit testToFromConfigs() {
        toFromConfig(Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE), "/eins/zwei"))
        toFromConfig(Folder.fromPersistableFolder(PersistableFolder.TEST_FOLDER, "/eins/zwei"))
        toFromConfig(Folder.fromFile(File("/nonexisting"), "/eins/zwei"))
    }

    @Test
    public Unit testDocConfig() {
        val docConfig: String = "DOCUMENT::content://com.android.externalstorage.documents/tree/primary%3Acgeo%2Funittest-doc::"
        val docFolder: Folder = Folder.fromConfig(docConfig)
        assertThat(docFolder.getBaseType()).isEqualTo(Folder.FolderType.DOCUMENT)
        assertThat(docFolder.getBaseUri()).isEqualTo(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Acgeo%2Funittest-doc"))
        assertThat(docFolder.getSubdirsToBase()).isEmpty()
    }

    @Test
    public Unit testLegacyDocumentConfig() {
        val docUri: Uri = Uri.parse(DOC_URI_EXAMPLE)
        val docFolder: Folder = Folder.fromConfig(DOC_URI_EXAMPLE)
        assertThat(docFolder.getBaseType()).isEqualTo(Folder.FolderType.DOCUMENT)
        assertThat(docFolder.getBaseUri()).isEqualTo(docUri)
    }

    @Test
    public Unit testLegacyFileConfig() {
        val file: File = File("/nonexisting/eins/zwei")
        val fileUri: Uri = Uri.fromFile(file)

        val pureFileFolder: Folder = Folder.fromConfig(file.getAbsolutePath())
        assertThat(pureFileFolder.getBaseType()).isEqualTo(Folder.FolderType.FILE)
        assertThat(File(pureFileFolder.getBaseUri().getPath())).isEqualTo(file)

        val uriFileFolder: Folder = Folder.fromConfig(fileUri.toString())
        assertThat(uriFileFolder.getBaseType()).isEqualTo(Folder.FolderType.FILE)
        assertThat(File(pureFileFolder.getBaseUri().getPath())).isEqualTo(file)
    }

    @Test
    public Unit testInvalidConfig() {
        assertThat(Folder.fromConfig(null)).isNull()
        assertThat(Folder.fromConfig("nothing-parseable")).isNull()
    }

    private Folder toFromConfig(final Folder folder) {
        val config: String = folder.toConfig()
        val newFolder: Folder = Folder.fromConfig(config)
        assertThat(newFolder).isEqualTo(folder)
        return newFolder
    }

}
