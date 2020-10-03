package cgeo.geocaching.storage;

import cgeo.CGeoTestCase;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.UriUtils;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.commons.lang3.tuple.ImmutablePair;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class PublicLocalStorageTest extends CGeoTestCase {

    public void setUp() {
        cleanup();
    }

    public void tearDown() {
        cleanup();
    }

    //a first small test to see how CI handles it
    public void testSimpleCreateDelete() {
        final FolderLocation testFolder = getTestFolder();

        final Uri uri = PublicLocalStorage.get().create(testFolder, FileNameCreator.forName("cgeo-test.txt"));
        final FolderLocation subfolder = FolderLocation.fromFolderLocation(testFolder, "eins");
        final FolderLocation subsubfolder = FolderLocation.fromFolderLocation(subfolder, "zwei");
        final Uri uri2 = PublicLocalStorage.get().create(subsubfolder, FileNameCreator.forName("cgeo-test-sub.txt"));

        assertThat(PublicLocalStorage.get().delete(uri)).isTrue();
        assertThat(PublicLocalStorage.get().delete(uri2)).isTrue();
    }

    public void testCopyAll() {
        final FolderLocation sourceFolder = FolderLocation.fromFolderLocation(getTestFolder(), "source");
        final FolderLocation targetFolder = FolderLocation.fromFolderLocation(getTestFolder(), "target");

        //create something to copy in source Folder
        final FolderLocation fOne = FolderLocation.fromFolderLocation(sourceFolder, "eins");
        final FolderLocation fTwo = FolderLocation.fromFolderLocation(sourceFolder, "zwei");
        final FolderLocation fThree = FolderLocation.fromFolderLocation(sourceFolder, "drei");
        final FolderLocation fTwoSub = FolderLocation.fromFolderLocation(fTwo, "sub");

        final FolderLocation[] sourceFolders = new FolderLocation[] {fOne, fTwo, fThree, fTwoSub};

        for (int i = 0; i < 20 ; i++) {
            PublicLocalStorage.get().create(sourceFolders[i % sourceFolders.length], FileNameCreator.forName("testfile" + i + ".txt"));
        }
        assertThat(PublicLocalStorage.get().getFileCounts(sourceFolder)).isEqualTo(new ImmutablePair<>(20, 4));
        assertThat(PublicLocalStorage.get().getFileCounts(targetFolder)).isEqualTo(new ImmutablePair<>(0, 0));

        //copy
        PublicLocalStorage.get().copyAll(sourceFolder, targetFolder, false);
        //after copy, source should be unchanged
        assertThat(PublicLocalStorage.get().getFileCounts(sourceFolder)).isEqualTo(new ImmutablePair<>(20, 4));
        //after copy, target should be identical to source
        assertThat(PublicLocalStorage.get().getFileCounts(targetFolder)).isEqualTo(new ImmutablePair<>(20, 4));


        //move
        PublicLocalStorage.get().copyAll(sourceFolder, targetFolder, true);

        //after move, source should be empty
        assertThat(PublicLocalStorage.get().getFileCounts(sourceFolder)).isEqualTo(new ImmutablePair<>(0, 0));
        //we expect now the DOUBLE amount of files in target, since PublicLocalStorage never overwrites existing files in create, always created new ones!
        assertThat(PublicLocalStorage.get().getFileCounts(targetFolder)).isEqualTo(new ImmutablePair<>(40, 4));
    }

    public void testCreateUniqueFilenames() {

        final FolderLocation testFolder = getTestFolder();
        assertFileDirCount(testFolder, 0, 0);

        //create two times with same name
        final Uri uri = PublicLocalStorage.get().create(testFolder, FileNameCreator.forName("test"));
        final Uri uri2 = PublicLocalStorage.get().create(testFolder, FileNameCreator.forName("test"));

        final Uri uriWithSuffix = PublicLocalStorage.get().create(testFolder, FileNameCreator.forName("testwithsuffix.txt"));
        final Uri uriWithSuffix2 = PublicLocalStorage.get().create(testFolder, FileNameCreator.forName("testwithsuffix.txt"));

        assertFileDirCount(testFolder, 4, 0);

        assertThat(UriUtils.getFileName(uri)).isEqualTo("test");
        assertThat(UriUtils.getFileName(uri)).isNotEqualTo(UriUtils.getFileName(uri2));

        assertThat(UriUtils.getFileName(uriWithSuffix)).isEqualTo("testwithsuffix.txt");
        assertThat(UriUtils.getFileName(uriWithSuffix)).isNotEqualTo(UriUtils.getFileName(uriWithSuffix2));
        assertThat(UriUtils.getFileName(uriWithSuffix2)).endsWith(".txt");
    }



    public void testWriteReadFile() throws IOException {

        final String testtext = "This is a test text";

        final FolderLocation testFolder = getTestFolder();
        final Uri uri = PublicLocalStorage.get().create(testFolder, "test.txt");
        try (OutputStreamWriter writer = new OutputStreamWriter(PublicLocalStorage.get().openForWrite(uri))) {
            writer.write(testtext);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(PublicLocalStorage.get().openForRead(uri)))) {
            final String s = reader.readLine();
            assertThat(s).isEqualTo(testtext);
        }
    }

    public void testPublicLocalFolderChangeNotification() {

        //initialize
        PublicLocalFolder.TEST_FOLDER.setUserDefinedLocation(null);
        //create Location based on test folder. Several subfolders.
        final FolderLocation folder = FolderLocation.fromFolderLocation(FolderLocation.fromPublicFolder(PublicLocalFolder.TEST_FOLDER, "one"), "two");
        //ensure that cache is filled
        PublicLocalStorage.get().list(folder);
        assertThat(folder.getCachedDocFile()).isNotNull();

        //change PublicLocalFolder
        PublicLocalFolder.TEST_FOLDER.setUserDefinedLocation(Uri.fromFile(new File("abc")));

        //assert that cache is cleared now
        assertThat(folder.getCachedDocFile()).isNull();

        //cleanup
        PublicLocalFolder.TEST_FOLDER.setUserDefinedLocation(null);
    }

    private void assertFileDirCount(final FolderLocation folder, final int fileCount, final int dirCount) {
        assertThat(PublicLocalStorage.get().getFileCounts(folder)).as("File counts of Folder " + folder).isEqualTo(new ImmutablePair<>(fileCount, dirCount));
    }

    private static FolderLocation getTestFolder() {
        return FolderLocation.fromFolderLocation(FolderLocation.CGEO_PRIVATE_FILES, "unittest");
    }

    private static void cleanup() {
        final FolderLocation folder = getTestFolder();
        PublicLocalStorage.get().deleteAll(folder);
    }
}
