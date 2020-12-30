package cgeo.geocaching.storage;

import cgeo.CGeoTestCase;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class PublicLocalStorageTest extends CGeoTestCase {

    private Uri testUri = null;

    public void setUp() throws Exception {
        super.setUp();
        //save TEST-FOLDER Uri if there is a user-defined one for later restoring
        if (PublicLocalFolder.TEST_FOLDER.isUserDefinedLocation()) {
            testUri = PublicLocalFolder.TEST_FOLDER.getLocation().getUri();
        }
        cleanup();
    }

    public void tearDown() throws Exception {
        cleanup();
        //restore test folder user-defined uri
        PublicLocalFolder.TEST_FOLDER.setUserDefinedLocation(testUri);

        //call super.teardown AFTER all own cleanup (because this seems to reset all members vars including testUri)
        super.tearDown();
    }

    //a first small test to see how CI handles it
    public void testFileSimpleCreateDelete() {
        performSimpleCreateDelete(getFileTestFolder());
    }

    public void testDocumentSimpleCreateDelete() {
        performSimpleCreateDelete(getDocumentTestFolder());
    }

    private void performSimpleCreateDelete(final Folder testFolder) {

        final Uri uri = PublicLocalStorage.get().create(testFolder, FileNameCreator.forName("cgeo-test.txt"));
        //final Folder subfolder = Folder.fromFolderLocation(testFolder, "eins");
        final Folder subsubfolder = Folder.fromFolder(testFolder, "eins/zwei");
        final Uri uri2 = PublicLocalStorage.get().create(subsubfolder, FileNameCreator.forName("cgeo-test-sub.txt"));

        assertThat(PublicLocalStorage.get().delete(uri)).isTrue();
        assertThat(PublicLocalStorage.get().delete(uri2)).isTrue();
    }

    public void testFileCopyAll() {
        final Folder sourceFolder = Folder.fromFolder(getFileTestFolder(), "source");
        final Folder targetFolder = Folder.fromFolder(getFileTestFolder(), "target");
        performCopyAll(sourceFolder, targetFolder);
    }

    public void testDocumentCopyAll() {
        final Folder sourceFolder = Folder.fromFolder(getDocumentTestFolder(), "source");
        final Folder targetFolder = Folder.fromFolder(getDocumentTestFolder(), "target");
        performCopyAll(sourceFolder, targetFolder);
    }

    private void performCopyAll(final Folder sourceFolder, final Folder targetFolder) {

        //create something to copy in source Folder
        final Folder fOne = Folder.fromFolder(sourceFolder, "eins");
        final Folder fTwo = Folder.fromFolder(sourceFolder, "zwei");
        final Folder fThree = Folder.fromFolder(sourceFolder, "drei");
        final Folder fTwoSub = Folder.fromFolder(fTwo, "sub");

        final Folder[] sourceFolders = new Folder[] {fOne, fTwo, fThree, fTwoSub};

        for (int i = 0; i < 20 ; i++) {
            assertThat(PublicLocalStorage.get().create(sourceFolders[i % sourceFolders.length], FileNameCreator.forName("testfile" + i + ".txt"))).isNotNull();
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

    public void testFileCreateUniqueFilenames() {
        performCreateUniqueFilenames(getFileTestFolder());
    }

    public void testDocumentCreateUniqueFilenames() {
        performCreateUniqueFilenames(getDocumentTestFolder());
    }

    private void performCreateUniqueFilenames(final Folder testFolder) {

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

    public void testFileWriteReadFile() throws IOException {
        performWriteReadFile(getFileTestFolder());
    }

    public void testDocumentWriteReadFile() throws IOException {
        performWriteReadFile(getDocumentTestFolder());
    }


    private void performWriteReadFile(final Folder testFolder) throws IOException {

        final String testtext = "This is a test text";

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
        final Folder folder = Folder.fromFolder(Folder.fromPublicFolder(PublicLocalFolder.TEST_FOLDER, "one"), "two");
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

    public void testFileMimeType() {
        performMimeType(getFileTestFolder());
    }

    public void testDocumentMimeType() {
        performMimeType(getDocumentTestFolder());
    }

    private void performMimeType(final Folder testLocation) {
        performMimeTypeTests(testLocation,
            new String[]{"txt", "jpg", "map"},
            new String[]{"text/plain", "image/jpeg", "application/octet-stream"});
    }

    private void performMimeTypeTests(final Folder testLocation, final String[] suffix, final String[] expectedMimeType) {
        final Map<String, String> mimeTypeMap = new HashMap<>();
        for (int i = 0; i < suffix.length; i++) {
            final String filename = "test." + suffix[i];
            assertThat(PublicLocalStorage.get().create(testLocation, filename)).isNotNull();
            mimeTypeMap.put(filename, expectedMimeType[i]);
        }
        final List<PublicLocalStorage.FileInformation> files = PublicLocalStorage.get().list(testLocation);
        assertThat(files.size()).isEqualTo(suffix.length);
        for (PublicLocalStorage.FileInformation fi : files) {
            assertThat(fi.mimeType).as("For file " + fi.name).isEqualTo(mimeTypeMap.get(fi.name));
        }
    }

    private void assertFileDirCount(final Folder folder, final int fileCount, final int dirCount) {
        assertThat(PublicLocalStorage.get().getFileCounts(folder)).as("File counts of Folder " + folder).isEqualTo(new ImmutablePair<>(fileCount, dirCount));
    }

    private boolean hasValidDocumentTestFolder() {
        return PublicLocalFolder.TEST_FOLDER.isUserDefinedLocation() && PublicLocalStorage.get().checkAvailability(PublicLocalFolder.TEST_FOLDER.getLocation(), PublicLocalFolder.TEST_FOLDER.needsWrite(), true);
    }

    private Folder getFileTestFolder() {
        return Folder.fromFolder(Folder.CGEO_PRIVATE_FILES, "unittest");
    }

    private Folder getDocumentTestFolder() {
        if (!hasValidDocumentTestFolder()) {
            Log.iForce("Trying to test for DocumentUri fails; unfortunately there is no DocumentUri configured for TEST-FOLDER. Test with file instead");
            return getFileTestFolder();
        }
        return PublicLocalFolder.TEST_FOLDER.getLocation();
    }

    private void cleanup() {
        PublicLocalStorage.get().deleteAll(getFileTestFolder());
        if (hasValidDocumentTestFolder()) {
            PublicLocalStorage.get().deleteAll(PublicLocalFolder.TEST_FOLDER.getLocation());
        }
    }
}
