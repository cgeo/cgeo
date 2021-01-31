package cgeo.geocaching.storage;

import cgeo.CGeoTestCase;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ContentStorageTest extends CGeoTestCase {

    private String testFolderConfig = null;

    private static final boolean FAIL_DOC_TEST_IF_FOLDER_NOT_ACCESSIBLE = false; // CI does not support Document tests unfortunately...
    private static final boolean KEEP_RESULTS = true;

    //note: must be sorted alphabetically for comparison to work out!
    private static final String COMPLEX_FOLDER_STRUCTURE = "[\"aaa.txt\", \"bbb.txt\", {\"name\": \"ccc\", \"files\": [ \"ccc-aaa.txt\", { \"name\": \"ccc-bbb\", \"files\": [] }, { \"name\": \"ccc-ccc\", \"files\": [ \"ccc-ccc-aaa.txt\", \"ccc-ccc-bbb.txt\" ] }, \"ccc-ddd.txt\" ] }, \"ddd.txt\"]";

    public void testSimpleExample() throws IOException {

        //This is a simple example for usage of contentstore

        //List the content of the LOGFILES directory, write for each file its name, size and whether it is a directory
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.LOGFILES);
        for (ContentStorage.FileInformation file : files) {
            Log.i("  File: " + file.name + ", size: " + file.size + ", isDir: " + file.isDirectory);
        }

        //Create a new subfolder with name "my-unittest-subfolder" in LOGFILES
        final Folder mysubfolder = Folder.fromPersistableFolder(PersistableFolder.LOGFILES, "my-unittest-subfolder");
        ContentStorage.get().ensureFolder(mysubfolder, true);

        //Create a new file in this subfolder
        final Uri myNewFile = ContentStorage.get().create(mysubfolder, "myNewFile.txt"); //in prod code: check for null!

        //write something to that new File
        Writer writer = null;
        try {
            final OutputStream os = ContentStorage.get().openForWrite(myNewFile); //in prod code: check for null!
            writer = new OutputStreamWriter(os, "UTF-8");
            writer.write("This is a test");

        } finally {
            IOUtils.closeQuietly(writer);
        }

        //read the same file out again
        BufferedReader reader = null;
        try {
            final InputStream is = ContentStorage.get().openForRead(myNewFile); //in prod code: check for null!
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            final String line = reader.readLine();
            assertThat(line).isEqualTo("This is a test");

        } finally {
            IOUtils.closeQuietly(reader);
        }

        //delete the created file
        ContentStorage.get().delete(myNewFile);

        //delete the created folder
        ContentStorage.get().delete(mysubfolder.getUri());

        //check out the other functions of ContentStorage
        //More complex operations (e.g. copyAll, deleteAll) can be found in class FolderUtils.

    }

    public void setUp() throws Exception {
        super.setUp();
        //save TEST-FOLDER Uri if there is a user-defined one for later restoring
        if (PersistableFolder.TEST_FOLDER.isUserDefined()) {
            testFolderConfig = PersistableFolder.TEST_FOLDER.getFolder().toConfig();
        }
    }

    public void tearDown() throws Exception {
        cleanup();
        //restore test folder user-defined uri
        PersistableFolder.TEST_FOLDER.setUserDefinedFolder(Folder.fromConfig(testFolderConfig), false);

        //call super.teardown AFTER all own cleanup (because this seems to reset all members vars including testUri)
        super.tearDown();
    }

    //a first small test to see how CI handles it
    public void testFileSimpleCreateDelete() {
        performSimpleCreateDelete(Folder.FolderType.FILE);
    }

    public void testDocumentSimpleCreateDelete() {
        performSimpleCreateDelete(Folder.FolderType.DOCUMENT);
    }

    private void performSimpleCreateDelete(final Folder.FolderType type) {

        final Folder testFolder = createTestFolder(type, "simpleCreateDelete");

        final Uri uri = ContentStorage.get().create(testFolder, "cgeo-test.txt");
        //final Folder subfolder = Folder.fromFolderLocation(testFolder, "eins");
        final Folder subsubfolder = Folder.fromFolder(testFolder, "eins/zwei");
        final Uri uri2 = ContentStorage.get().create(subsubfolder, "cgeo-test-sub.txt");

        assertThat(ContentStorage.get().delete(uri)).isTrue();
        assertThat(ContentStorage.get().delete(uri2)).isTrue();
    }

    public void testFileStrangeNames()  {
        final Folder folder = createTestFolder(Folder.FolderType.FILE, "strangeNames");
        ContentStorage.get().ensureFolder(folder, true);

        final File dir = new File(folder.getUri().getPath());
        final File f1 = new File(dir, "a b c");
        f1.mkdirs();

        FolderUtils.get().getFolderInfo(folder);

        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(folder);

        assertThat(files).hasSize(1);
        assertThat(files.get(0).name).isEqualTo("a b c");
    }

    public void testFileCopyAll() {
        performCopyAll(Folder.FolderType.FILE, Folder.FolderType.FILE);
    }

    public void testDocumentCopyAll() {
        performCopyAll(Folder.FolderType.DOCUMENT, Folder.FolderType.DOCUMENT);
    }

    private void performCopyAll(final Folder.FolderType typeSource, final Folder.FolderType typeTarget) {

        final Folder sourceFolder = Folder.fromFolder(createTestFolder(typeSource, "copyAll"), "source");
        final Folder targetFolder = Folder.fromFolder(createTestFolder(typeTarget, "copyAll"), "target");

        //create something to copy in source Folder
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE);

        //copy
        FolderUtils.CopyResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false);
        assertCopyResult(result, FolderUtils.CopyResultStatus.OK, 7, 3);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false), COMPLEX_FOLDER_STRUCTURE);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false), COMPLEX_FOLDER_STRUCTURE);

        //move
        assertThat(FolderUtils.get().deleteAll(targetFolder)).isTrue();
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true);
        assertCopyResult(result, FolderUtils.CopyResultStatus.OK, 7, 3);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false), "[]");
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false), COMPLEX_FOLDER_STRUCTURE);
    }

    public void testFileCopyAllAbortAndStatus() {
        performCopyAllAbortAndStatus(Folder.FolderType.FILE, Folder.FolderType.FILE);
    }

    public void testDocumentCopyAllAbortAndStatus() {
        performCopyAllAbortAndStatus(Folder.FolderType.DOCUMENT, Folder.FolderType.DOCUMENT);
    }

    private void performCopyAllAbortAndStatus(final Folder.FolderType typeSource, final Folder.FolderType typeTarget) {

        final Folder sourceFolder = Folder.fromFolder(createTestFolder(typeSource, "copyAllAbortAndStatus"), "source");
        final Folder targetFolder = Folder.fromFolder(createTestFolder(typeTarget, "copyAllAbortAndStatus"), "target");
        final Folder targetFolder2 = Folder.fromFolder(createTestFolder(typeTarget, "copyAllAbortAndStatus"), "target2");

        //create something to copy in source Folder
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE);

        //copy complete
        final List<FolderUtils.CopyStatus> copyStatus = new ArrayList<>();
        final AtomicBoolean cancelFlag = new AtomicBoolean(false);
        FolderUtils.CopyResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false, cancelFlag, cs -> copyStatus.add(cs));
        assertCopyResult(result, FolderUtils.CopyResultStatus.OK, 7, 3);
        //expect one initial status (with files/dirs to copy = -1), one status before (with files/dirs copied = 0 but known nr of files/dirs to copy) and then one for each file/dir
        assertThat(copyStatus).hasSize(2 + 3 + 7);
        assertThat(copyStatus.get(0).filesInSource).isEqualTo(-1);
        assertThat(copyStatus.get(0).dirsInSource).isEqualTo(-1);
        assertThat(copyStatus.get(0).filesCopied + copyStatus.get(0).dirsCopied).isEqualTo(0);
        int prevSum = 0;
        for (int i = 1; i < 2 + 3 + 7; i++) {
            assertThat(copyStatus.get(i).filesInSource).isEqualTo(7);
            assertThat(copyStatus.get(i).dirsInSource).isEqualTo(3);
            assertThat(copyStatus.get(i).filesCopied + copyStatus.get(i).dirsCopied).isEqualTo(prevSum);
            prevSum++;
        }

        //copy aborted
        copyStatus.clear();
        final int abortAfter = 6;
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder2, false, cancelFlag, cs -> {
            copyStatus.add(cs);
            if (copyStatus.size() >= abortAfter) {
                cancelFlag.set(true);
            }
        });
        assertThat(result.status).isEqualTo(FolderUtils.CopyResultStatus.ABORTED);
        //expect one initial status (with files/dirs to copy = -1), one status before (with files/dirs copied = 0 but known nr of files/dirs to copy) and then one for each file/dir
        assertThat(copyStatus).hasSize(abortAfter + 1);
        assertThat(copyStatus.get(0).filesInSource).isEqualTo(-1);
        assertThat(copyStatus.get(0).dirsInSource).isEqualTo(-1);
        assertThat(copyStatus.get(0).filesCopied + copyStatus.get(0).dirsCopied).isEqualTo(0);
        prevSum = 0;
        for (int i = 1; i < abortAfter + 1; i++) {
            assertThat(copyStatus.get(i).filesInSource).isEqualTo(7);
            assertThat(copyStatus.get(i).dirsInSource).isEqualTo(3);
            assertThat(copyStatus.get(i).filesCopied + copyStatus.get(i).dirsCopied).isEqualTo(prevSum);
            prevSum++;
        }
        //check that result status of different sources match also when copy was aborted
        assertThat(copyStatus.get(abortAfter).filesCopied).isEqualTo(result.filesCopied);
        assertThat(copyStatus.get(abortAfter).dirsCopied).isEqualTo(result.dirsCopied);
        final ImmutablePair<Integer, Integer> targetInfo = FolderUtils.get().getFolderInfo(targetFolder2);
        assertThat(targetInfo).isEqualTo(new ImmutablePair<>(result.filesCopied, result.dirsCopied));
     }

    public void testFileCopyAllSameDir() {
        performCopyAllSameDir(Folder.FolderType.FILE);
    }

    public void testDocumentCopyAllSameDir() {
        performCopyAllSameDir(Folder.FolderType.DOCUMENT);
    }


    private void performCopyAllSameDir(final Folder.FolderType typeSource) {

        final Folder sourceTargetFolder = Folder.fromFolder(createTestFolder(typeSource, "copyAllSameDir"), "sourceTarget");

        createTree(sourceTargetFolder, COMPLEX_FOLDER_STRUCTURE);

        final FolderUtils.CopyResult result = FolderUtils.get().copyAll(sourceTargetFolder, sourceTargetFolder, true);
        assertCopyResult(result, FolderUtils.CopyResultStatus.OK, 0, 0);

        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceTargetFolder, false, false), COMPLEX_FOLDER_STRUCTURE);
    }

    public void testFileCopyAllTargetInSource() {
        performCopyAllTargetInSource(Folder.FolderType.FILE);
    }

    public void testDocumentCopyAllTargetInSource() {
        performCopyAllTargetInSource(Folder.FolderType.DOCUMENT);
    }

    private void performCopyAllTargetInSource(final Folder.FolderType typeSource) {

        final Folder sourceFolder = Folder.fromFolder(createTestFolder(typeSource, "copyAllTargetInSource"), "source");
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE);
        final Folder targetFolder = Folder.fromFolder(sourceFolder, "ccc/ccc-ccc");

        //copy
        FolderUtils.CopyResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false);
        assertCopyResult(result, FolderUtils.CopyResultStatus.OK, 7, 3);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false),
            "[\"aaa.txt\", \"bbb.txt\", {\"name\": \"ccc\", \"files\": " +
                "[ \"ccc-aaa.txt\", { \"name\": \"ccc-bbb\", \"files\": [] }, { \"name\": \"ccc-ccc\", \"files\": " +
                    "[ \"aaa.txt\", \"bbb.txt\", {\"name\": \"ccc\", \"files\": " +
                        "[ \"ccc-aaa.txt\", { \"name\": \"ccc-bbb\", \"files\": [] }, { \"name\": \"ccc-ccc\", \"files\": " +
                            "[\"ccc-ccc-aaa.txt\", \"ccc-ccc-bbb.txt\" ] }, " +
                        "\"ccc-ddd.txt\" ] " +
                    "}, \"ccc-ccc-aaa.txt\", \"ccc-ccc-bbb.txt\", \"ddd.txt\" ] " +
                "}, \"ccc-ddd.txt\" ] " +
            "}, \"ddd.txt\"]");

        //move
        FolderUtils.get().deleteAll(sourceFolder);
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE);
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true);
        assertCopyResult(result, FolderUtils.CopyResultStatus.OK, 7, 3);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false), COMPLEX_FOLDER_STRUCTURE);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false),
            "[{\"name\": \"ccc\", \"files\": [ { \"name\": \"ccc-ccc\", \"files\": " +
                COMPLEX_FOLDER_STRUCTURE + "} ] } ]");
    }

    public void testFileCopyAllSourceInTarget() {
        performCopyAllSourceInTarget(Folder.FolderType.FILE);
    }

    public void testDocumentCopyAllSourceInTarget() {
        performCopyAllSourceInTarget(Folder.FolderType.DOCUMENT);
    }

    private void performCopyAllSourceInTarget(final Folder.FolderType typeSource) {

        final Folder targetFolder = Folder.fromFolder(createTestFolder(typeSource, "copyAllSourceInTarget"), "target");
        createTree(targetFolder, COMPLEX_FOLDER_STRUCTURE);
        final Folder sourceFolder = Folder.fromFolder(targetFolder, "ccc");

        //copy
        FolderUtils.CopyResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false);
        assertCopyResult(result, FolderUtils.CopyResultStatus.OK, 4, 2);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false),
            "[\"aaa.txt\",\"bbb.txt\",{\"name\":\"ccc\",\"files\":[\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\"]},\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\",\"ddd.txt\"]"
            );

        //move
        FolderUtils.get().deleteAll(targetFolder);
        createTree(targetFolder, COMPLEX_FOLDER_STRUCTURE);
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true);
        assertCopyResult(result, FolderUtils.CopyResultStatus.OK, 4, 2);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false),
            "[\"aaa.txt\",\"bbb.txt\",{\"name\":\"ccc\",\"files\":[" +
                //"\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\"" +
                "]},\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\",\"ddd.txt\"]"
        );
    }


    private void assertEqualsWithoutWhitespaces(final String value, final String expected) {
        assertThat(value.replaceAll("[\\s]", "")).isEqualTo(expected.replaceAll("[\\s]", ""));
    }

    public void testFileCreateUniqueFilenames() {
        performCreateUniqueFilenames(Folder.FolderType.FILE);
    }

    public void testDocumentCreateUniqueFilenames() {
        performCreateUniqueFilenames(Folder.FolderType.DOCUMENT);
    }

    private void performCreateUniqueFilenames(final Folder.FolderType type) {

        final Folder testFolder = createTestFolder(type, "createUniqueFilenames");

        assertFileDirCount(testFolder, 0, 0);

        //create two times with same name
        final Uri uri = ContentStorage.get().create(testFolder, "test");
        final Uri uri2 = ContentStorage.get().create(testFolder, "test");

        final Uri uriWithSuffix = ContentStorage.get().create(testFolder, "testwithsuffix.txt");
        final Uri uriWithSuffix2 = ContentStorage.get().create(testFolder, "testwithsuffix.txt");

        assertFileDirCount(testFolder, 4, 0);

        assertThat(UriUtils.getLastPathSegment(uri)).isEqualTo("test");
        assertThat(UriUtils.getLastPathSegment(uri)).isNotEqualTo(UriUtils.getLastPathSegment(uri2));

        assertThat(UriUtils.getLastPathSegment(uriWithSuffix)).isEqualTo("testwithsuffix.txt");
        assertThat(UriUtils.getLastPathSegment(uriWithSuffix)).isNotEqualTo(UriUtils.getLastPathSegment(uriWithSuffix2));
        assertThat(UriUtils.getLastPathSegment(uriWithSuffix2)).endsWith(".txt");
    }

    public void testFileWriteReadFile() throws IOException {
        performWriteReadFile(Folder.FolderType.FILE);
    }

    public void testDocumentWriteReadFile() throws IOException {
        performWriteReadFile(Folder.FolderType.DOCUMENT);
    }


    private void performWriteReadFile(final Folder.FolderType type) throws IOException {

        final Folder testFolder = createTestFolder(type, "createUniqueFilenames");

        final String testtext = "This is a test text";

        final Uri uri = ContentStorage.get().create(testFolder, "test.txt");

        //write to new file
        try (OutputStreamWriter writer = new OutputStreamWriter(ContentStorage.get().openForWrite(uri))) {
            writer.write(testtext);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ContentStorage.get().openForRead(uri)))) {
            final String s = reader.readLine();
            assertThat(s).isEqualTo(testtext);
        }

        //append
        try (OutputStreamWriter writer = new OutputStreamWriter(ContentStorage.get().openForWrite(uri, true))) {
            writer.write(testtext);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ContentStorage.get().openForRead(uri)))) {
            final String s = reader.readLine();
            assertThat(s).isEqualTo(testtext + testtext);
        }

        //overwrite
        try (OutputStreamWriter writer = new OutputStreamWriter(ContentStorage.get().openForWrite(uri))) {
            writer.write(testtext);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ContentStorage.get().openForRead(uri)))) {
            final String s = reader.readLine();
            assertThat(s).isEqualTo(testtext);
        }
    }

    public void testFileBasicFolderOperations() throws IOException {
        performBasicFolderOperations(Folder.FolderType.FILE);
    }

    public void testDocumentBasicFolderOperations() throws IOException {
        performBasicFolderOperations(Folder.FolderType.DOCUMENT);
    }


    private void performBasicFolderOperations(final Folder.FolderType type) throws IOException {

        final Folder testFolder = createTestFolder(type, "basicFolderOperations");

        List<ContentStorage.FileInformation> list = ContentStorage.get().list(testFolder);
        assertThat(list).isEmpty();

        //test that some methods fail as expected
        assertThat(ContentStorage.get().exists(testFolder, "test-nonexisting.txt")).isFalse();
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test-nonexisting.txt")).isNull();
        assertThat(ContentStorage.get().delete(Uri.fromFile(new File("/test/test.txt")))).isFalse();
        assertThat(ContentStorage.get().getName(Uri.fromFile(new File("/test/test.txt")))).isNull();
        assertThat(ContentStorage.get().exists(testFolder, null)).isFalse();
        assertThat(ContentStorage.get().getFileInfo(testFolder, null)).isNull();
        assertThat(ContentStorage.get().delete(null)).isFalse();
        assertThat(ContentStorage.get().getName(null)).isNull();

        //create a new file
        final Uri fileUri = ContentStorage.get().create(testFolder, "test.txt");
        list = ContentStorage.get().list(testFolder);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).name).isEqualTo("test.txt");

        //get that file
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test.txt").name).isEqualTo("test.txt");
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test.txt").uri).isEqualTo(fileUri);
        assertThat(ContentStorage.get().exists(testFolder, "test.txt")).isTrue();

        //create file with same name, ask for returning same and check if is is in fact the same
        final Uri file2Uri = ContentStorage.get().create(testFolder, FileNameCreator.forName("test.txt"), true);
        assertThat(file2Uri).isEqualTo(fileUri);
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test.txt").name).isEqualTo("test.txt");
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test.txt").uri).isEqualTo(fileUri);
        list = ContentStorage.get().list(testFolder);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).name).isEqualTo("test.txt");

        //create fiole with same name, ask for creating it anew
        final Uri file3Uri = ContentStorage.get().create(testFolder, FileNameCreator.forName("test.txt"), false);
        final String newName = ContentStorage.get().getName(file3Uri);
        assertThat(newName).startsWith("test");
        assertThat(newName).endsWith(".txt");

        assertThat(file3Uri).isNotEqualTo(fileUri);
        final ContentStorage.FileInformation fileInfo = ContentStorage.get().getFileInfo(testFolder, newName);
        assertThat(fileInfo.name).isEqualTo(newName);
        assertThat(fileInfo.uri).isNotEqualTo(fileUri);
        list = ContentStorage.get().list(testFolder, true);
        assertThat(list).hasSize(2);
        final Set<String> names = new HashSet<>();
        for (ContentStorage.FileInformation fi : list) {
            names.add(fi.name);
        }
        assertThat(names).contains("test.txt", newName);

        //delete the second file
        assertThat(ContentStorage.get().delete(file3Uri)).isTrue();
        list = ContentStorage.get().list(testFolder);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).name).isEqualTo("test.txt");

        //create a subfolder
        final Folder subfolder = Folder.fromFolder(testFolder, "subfolder");
        assertThat(ContentStorage.get().ensureFolder(subfolder, true)).isTrue();
        list = ContentStorage.get().list(testFolder, true);
        // "subfolder" is alphabetically before "test.txt"
        assertThat(list).hasSize(2);
        assertThat(list.get(0).name).isEqualTo("subfolder");
        assertThat(list.get(0).isDirectory).isTrue();
        assertThat(list.get(0).dirLocation).isEqualTo(subfolder);
        assertThat(ContentStorage.get().getName(list.get(0).uri)).isEqualTo("subfolder");
        assertThat(list.get(1).name).isEqualTo("test.txt");
        assertThat(list.get(1).isDirectory).isFalse();
        assertThat(list.get(1).dirLocation).isNull();
    }

    public void testPersistableFolderChangeNotification() {

        //create Location based on test folder. Several subfolders.
        final Folder folder = Folder.fromPersistableFolder(PersistableFolder.TEST_FOLDER, "changeNotificatio");
        final Folder folderOne = Folder.fromFolder(folder, "one");
        final Folder folderTwo = Folder.fromFolder(folder, "two");
        final Folder folderNotNotified = Folder.fromPersistableFolder(PersistableFolder.OFFLINE_MAPS, "changeNotificatio");

        final Set<String> notificationMessages = new HashSet<>();

        folderOne.registerChangeListener(this, p -> notificationMessages.add("one:" + p.name()));
        folderTwo.registerChangeListener(this, p -> notificationMessages.add("two:" + p.name()));
        folderNotNotified.registerChangeListener(this, p -> notificationMessages.add("notnotified:" + p.name()));

        //trigger change
        PersistableFolder.TEST_FOLDER.setUserDefinedFolder(null, false);

        //check
        assertThat(notificationMessages.size()).isEqualTo(2);
        assertThat(notificationMessages.contains("one:" + PersistableFolder.TEST_FOLDER.name()));
        assertThat(notificationMessages.contains("two:" + PersistableFolder.TEST_FOLDER.name()));
    }

    public void testFileMimeType() {
        performMimeType(Folder.FolderType.FILE);
    }

    public void testDocumentMimeType() {
        performMimeType(Folder.FolderType.DOCUMENT);
    }

    private void performMimeType(final Folder.FolderType type) {

        final Folder testFolder = createTestFolder(type, "mimeType");

        performMimeTypeTests(testFolder,
            new String[]{"txt", "jpg", "jpeg", "map", "hprof", "gpx", null},
            new String[]{"text/plain", "image/jpeg", "image/jpeg", "application/octet-stream", "application/octet-stream", "application/octet-stream", "application/octet-stream" });
    }

    private void performMimeTypeTests(final Folder testLocation, final String[] suffix, final String[] expectedMimeType) {
        final Map<String, String> mimeTypeMap = new HashMap<>();
        for (int i = 0; i < suffix.length; i++) {
            final String filename = "test" + (suffix[i] == null ? "" : "." + suffix[i]);
            assertThat(ContentStorage.get().create(testLocation, filename)).isNotNull();
            mimeTypeMap.put(filename, expectedMimeType[i]);
        }
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(testLocation);
        assertThat(files.size()).isEqualTo(suffix.length);
        for (ContentStorage.FileInformation fi : files) {
            assertThat(mimeTypeMap.containsKey(fi.name)).as("Unexpected File " + fi.name).isTrue();
            assertThat(fi.mimeType).as("For file " + fi.name).isEqualTo(mimeTypeMap.get(fi.name));
        }
    }

    private void createTree(final Folder folder, final String structure) {
        final JsonNode json = JsonUtils.toNode(structure);
        if (json == null) {
            throw new IllegalArgumentException("Invalid Json structure: " + structure);
        }
        createTree(folder, json);
    }

    private void createTree(final Folder folder, final JsonNode node) {
        final Iterator<JsonNode> it = node.elements();
        while (it.hasNext()) {
            final JsonNode n = it.next();
            if (n.isObject()) {
                //this is a subfolder
               final String folderName = n.get("name").asText();
                final Folder newFolder = Folder.fromFolder(folder, folderName);
                ContentStorage.get().ensureFolder(newFolder, true);
                createTree(newFolder, n.get("files"));
            } else {
                //this is a file
                ContentStorage.get().create(folder, n.textValue());
            }
        }
    }

    private void assertCopyResult(final FolderUtils.CopyResult result, final FolderUtils.CopyResultStatus expectedState, final int expectedFileCount, final int expectedDirCount) {
        assertThat(result.status).isEqualTo(expectedState);
        assertThat(result.filesCopied).isEqualTo(expectedFileCount);
        assertThat(result.dirsCopied).isEqualTo(expectedDirCount);
    }

    private void assertFileDirCount(final Folder folder, final int fileCount, final int dirCount) {
        assertThat(FolderUtils.get().getFolderInfo(folder)).as("File counts of Folder " + folder).isEqualTo(new ImmutablePair<>(fileCount, dirCount));
    }

    private boolean hasValidDocumentTestFolder() {
        return PersistableFolder.TEST_FOLDER.isUserDefined() &&
            ContentStorage.get().ensureFolder(PersistableFolder.TEST_FOLDER.getFolder(), PersistableFolder.TEST_FOLDER.needsWrite(), true);
    }

    private Folder createTestFolder(final Folder.FolderType type, final String context) {

        final Folder testFolder;
        switch (type) {
            case FILE:
                testFolder = Folder.fromFolder(getBaseTestFolder(Folder.FolderType.FILE), "file-" + context);
                break;
            case DOCUMENT:
                if (!hasValidDocumentTestFolder()) {
                    if (FAIL_DOC_TEST_IF_FOLDER_NOT_ACCESSIBLE) {
                        throw new IllegalArgumentException("Document Folder not accessible, test fails: " + PersistableFolder.TEST_FOLDER.getFolder());
                    }
                    Log.iForce("Trying to test for DocumentUri fails; unfortunately there is no DocumentUri configured for TEST-FOLDER. Test with file instead");
                    testFolder =  Folder.fromFolder(getBaseTestFolder(Folder.FolderType.FILE), "doc-" + context);
                } else {
                    testFolder = Folder.fromFolder(getBaseTestFolder(Folder.FolderType.DOCUMENT), context);
                }
                break;
            default:
                testFolder = null;
        }

        if (testFolder != null) {
            FolderUtils.get().deleteAll(testFolder);
        }
        return testFolder;
    }

    private Folder getBaseTestFolder(final Folder.FolderType type) {
        switch (type) {
            case FILE:
                return Folder.fromFolder(Folder.CGEO_PRIVATE_FILES, "unittest");
            case DOCUMENT:
                if (!hasValidDocumentTestFolder()) {
                    return Folder.fromFolder(Folder.CGEO_PRIVATE_FILES, "unittest");
                }
                return PersistableFolder.TEST_FOLDER.getFolder();
            default:
                return null;
        }
    }

     private void cleanup() {
        if (KEEP_RESULTS) {
            return;
        }
         FolderUtils.get().deleteAll(getBaseTestFolder(Folder.FolderType.FILE));
        if (hasValidDocumentTestFolder()) {
            FolderUtils.get().deleteAll(getBaseTestFolder(Folder.FolderType.DOCUMENT));
        }
    }
}
