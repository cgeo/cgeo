package cgeo.geocaching.storage;

import android.net.Uri;

import java.io.File;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FolderTest {

    private static final String DOC_URI_EXAMPLE = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Fcgeo/document/primary%3ADocuments%2Fcgeo%2Flogfiles%2Flogcat_2020-12-28_17-22-20-2.txt";
    private static final String DOC_URI_EXAMPLE_DECODED = "content://com.android.externalstorage.documents/tree/primary:Documents/cgeo/document/primary:Documents/cgeo/logfiles/logcat_2020-12-28_17-22-20-2.txt";

    @Test
    public void testEquals() {
        final Folder folder1 = Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE));
        final Folder folder2 = Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE));
        final Folder folder3 = Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE_DECODED));
        final Folder folderDifferent = Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE.substring(0, DOC_URI_EXAMPLE.length() - 10)));

        assertThat(folder1).isEqualTo(folder1);
        assertThat(folder1).isEqualTo(folder2);
        assertThat(folder1).isEqualTo(folder3);
        assertThat(folder1).isNotEqualTo(folderDifferent);
    }

    @Test
    public void testToFromConfigs() {
        toFromConfig(Folder.fromDocumentUri(Uri.parse(DOC_URI_EXAMPLE), "/eins/zwei"));
        toFromConfig(Folder.fromPersistableFolder(PersistableFolder.TEST_FOLDER, "/eins/zwei"));
        toFromConfig(Folder.fromFile(new File("/nonexisting"), "/eins/zwei"));
    }

    @Test
    public void testDocConfig() {
        final String docConfig = "DOCUMENT::content://com.android.externalstorage.documents/tree/primary%3Acgeo%2Funittest-doc::";
        final Folder docFolder = Folder.fromConfig(docConfig);
        assertThat(docFolder.getBaseType()).isEqualTo(Folder.FolderType.DOCUMENT);
        assertThat(docFolder.getBaseUri()).isEqualTo(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Acgeo%2Funittest-doc"));
        assertThat(docFolder.getSubdirsToBase()).isEmpty();
    }

    @Test
    public void testLegacyDocumentConfig() {
        final Uri docUri = Uri.parse(DOC_URI_EXAMPLE);
        final Folder docFolder = Folder.fromConfig(DOC_URI_EXAMPLE);
        assertThat(docFolder.getBaseType()).isEqualTo(Folder.FolderType.DOCUMENT);
        assertThat(docFolder.getBaseUri()).isEqualTo(docUri);
    }

    @Test
    public void testLegacyFileConfig() {
        final File file = new File("/nonexisting/eins/zwei");
        final Uri fileUri = Uri.fromFile(file);

        final Folder pureFileFolder = Folder.fromConfig(file.getAbsolutePath());
        assertThat(pureFileFolder.getBaseType()).isEqualTo(Folder.FolderType.FILE);
        assertThat(new File(pureFileFolder.getBaseUri().getPath())).isEqualTo(file);

        final Folder uriFileFolder = Folder.fromConfig(fileUri.toString());
        assertThat(uriFileFolder.getBaseType()).isEqualTo(Folder.FolderType.FILE);
        assertThat(new File(pureFileFolder.getBaseUri().getPath())).isEqualTo(file);
    }

    @Test
    public void testInvalidConfig() {
        assertThat(Folder.fromConfig(null)).isNull();
        assertThat(Folder.fromConfig("nothing-parseable")).isNull();
    }

    private Folder toFromConfig(final Folder folder) {
        final String config = folder.toConfig();
        final Folder newFolder = Folder.fromConfig(config);
        assertThat(newFolder).isEqualTo(folder);
        return newFolder;
    }

}
