package cgeo.geocaching.storage;

import cgeo.CGeoTestCase;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

    public void testFileStrangeNames() {
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
        FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false);
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false), COMPLEX_FOLDER_STRUCTURE);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false), COMPLEX_FOLDER_STRUCTURE);

        //move
        assertThat(FolderUtils.get().deleteAll(targetFolder)).isTrue();
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true);
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3);
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
        final List<FolderUtils.FolderProcessStatus> folderProcessStatuses = new ArrayList<>();
        final AtomicBoolean cancelFlag = new AtomicBoolean(false);
        FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false, cancelFlag, folderProcessStatuses::add);
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3);
        //expect one initial status (with files/dirs to copy = -1), one status before (with files/dirs copied = 0 but known nr of files/dirs to copy) and then one for each file/dir
        assertThat(folderProcessStatuses).hasSize(2 + 3 + 7);
        assertThat(folderProcessStatuses.get(0).filesInSource).isEqualTo(-1);
        assertThat(folderProcessStatuses.get(0).dirsInSource).isEqualTo(-1);
        assertThat(folderProcessStatuses.get(0).filesProcessed + folderProcessStatuses.get(0).dirsProcessed).isEqualTo(0);
        int prevSum = 0;
        for (int i = 1; i < 2 + 3 + 7; i++) {
            assertThat(folderProcessStatuses.get(i).filesInSource).isEqualTo(7);
            assertThat(folderProcessStatuses.get(i).dirsInSource).isEqualTo(3);
            assertThat(folderProcessStatuses.get(i).filesProcessed + folderProcessStatuses.get(i).dirsProcessed).isEqualTo(prevSum);
            prevSum++;
        }

        //copy aborted
        folderProcessStatuses.clear();
        final int abortAfter = 6;
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder2, false, cancelFlag, cs -> {
            folderProcessStatuses.add(cs);
            if (folderProcessStatuses.size() >= abortAfter) {
                cancelFlag.set(true);
            }
        });
        assertThat(result.result).isEqualTo(FolderUtils.ProcessResult.ABORTED);
        //expect one initial status (with files/dirs to copy = -1), one status before (with files/dirs copied = 0 but known nr of files/dirs to copy) and then one for each file/dir
        assertThat(folderProcessStatuses).hasSize(abortAfter + 1);
        assertThat(folderProcessStatuses.get(0).filesInSource).isEqualTo(-1);
        assertThat(folderProcessStatuses.get(0).dirsInSource).isEqualTo(-1);
        assertThat(folderProcessStatuses.get(0).filesProcessed + folderProcessStatuses.get(0).dirsProcessed).isEqualTo(0);
        prevSum = 0;
        for (int i = 1; i < abortAfter + 1; i++) {
            assertThat(folderProcessStatuses.get(i).filesInSource).isEqualTo(7);
            assertThat(folderProcessStatuses.get(i).dirsInSource).isEqualTo(3);
            assertThat(folderProcessStatuses.get(i).filesProcessed + folderProcessStatuses.get(i).dirsProcessed).isEqualTo(prevSum);
            prevSum++;
        }
        //check that result status of different sources match also when copy was aborted
        assertThat(folderProcessStatuses.get(abortAfter).filesProcessed).isEqualTo(result.filesModified);
        assertThat(folderProcessStatuses.get(abortAfter).dirsProcessed).isEqualTo(result.dirsModified);

        assertFileDirCount(targetFolder2, result.filesModified, result.dirsModified);
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

        final FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceTargetFolder, sourceTargetFolder, true);
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 0, 0);

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
        FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false);
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3);
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
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3);
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
        FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false);
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 4, 2);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false),
                "[\"aaa.txt\",\"bbb.txt\",{\"name\":\"ccc\",\"files\":[\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\"]},\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\",\"ddd.txt\"]"
        );

        //move
        FolderUtils.get().deleteAll(targetFolder);
        createTree(targetFolder, COMPLEX_FOLDER_STRUCTURE);
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true);
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 4, 2);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false),
                "[\"aaa.txt\",\"bbb.txt\",{\"name\":\"ccc\",\"files\":[" +
                        //"\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\"" +
                        "]},\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\",\"ddd.txt\"]"
        );
    }

    public void testFileGetAllFiles() {
        performGetAllFiles(Folder.FolderType.FILE);
    }

    public void testDocumentGetAllFiles() {
        performGetAllFiles(Folder.FolderType.DOCUMENT);
    }

    private void performGetAllFiles(final Folder.FolderType typeSource) {

        final Folder testFolder = createTestFolder(typeSource, "getAllFiles");
        createTree(testFolder, COMPLEX_FOLDER_STRUCTURE);

        final List<ImmutablePair<ContentStorage.FileInformation, String>> allFiles = FolderUtils.get().getAllFiles(testFolder);
        Collections.sort(allFiles, (e1, e2) -> e1.right.compareTo(e2.right));

        assertThat(allFiles).hasSize(10); //7 files, 3 dirs
        assertFileInfo(allFiles.get(0), "aaa.txt", false, "/aaa.txt");
        assertFileInfo(allFiles.get(1), "bbb.txt", false, "/bbb.txt");
        assertFileInfo(allFiles.get(2), "ccc", true, "/ccc");
        assertFileInfo(allFiles.get(3), "ccc-aaa.txt", false, "/ccc/ccc-aaa.txt");
        assertFileInfo(allFiles.get(4), "ccc-bbb", true, "/ccc/ccc-bbb");
        assertFileInfo(allFiles.get(5), "ccc-ccc", true, "/ccc/ccc-ccc");
        assertFileInfo(allFiles.get(6), "ccc-ccc-aaa.txt", false, "/ccc/ccc-ccc/ccc-ccc-aaa.txt");
        assertFileInfo(allFiles.get(7), "ccc-ccc-bbb.txt", false, "/ccc/ccc-ccc/ccc-ccc-bbb.txt");
        assertFileInfo(allFiles.get(8), "ccc-ddd.txt", false, "/ccc/ccc-ddd.txt");
        assertFileInfo(allFiles.get(9), "ddd.txt", false, "/ddd.txt");
    }

    public void testFileGetFolderInfo() {
        performGetFolderInfo(Folder.FolderType.FILE);
    }

    public void testDocumentGetFolderInfo() {
        performGetFolderInfo(Folder.FolderType.DOCUMENT);
    }

    private void performGetFolderInfo(final Folder.FolderType typeSource) {

        final Folder testFolder = createTestFolder(typeSource, "getFolderInfo");
        createTree(testFolder, COMPLEX_FOLDER_STRUCTURE);

        FolderUtils.FolderInfo info = FolderUtils.get().getFolderInfo(testFolder, 1);
        assertThat(info.resultIsIncomplete).isTrue();
        assertThat(info.dirCount).isEqualTo(3); //subfolder "ccc" is scanned and contains two add. sufolders...
        assertThat(info.fileCount).isEqualTo(5); //...but the subfolders of ccc are NOT scanned any more, thus files in ccc-ccc don't count

        info = FolderUtils.get().getFolderInfo(testFolder, 0);
        assertThat(info.resultIsIncomplete).isTrue();
        assertThat(info.dirCount).isEqualTo(1); //only subfolder "ccc" is recognized...
        assertThat(info.fileCount).isEqualTo(3); //...but files in "ccc" and "ccc-ccc" are NOT

        info = FolderUtils.get().getFolderInfo(testFolder, -1);
        assertThat(info.resultIsIncomplete).isFalse();
        assertThat(info.dirCount).isEqualTo(3);
        assertThat(info.fileCount).isEqualTo(7);

        info = FolderUtils.get().getFolderInfo(testFolder, 3);
        assertThat(info.resultIsIncomplete).isTrue();
        assertThat(info.dirCount).isEqualTo(3);
        assertThat(info.fileCount).isEqualTo(7);

    }

    private void assertFileInfo(final ImmutablePair<ContentStorage.FileInformation, String> entry, final String fileName, final boolean isDir, final String path) {
        assertThat(entry.left.name).isEqualTo(fileName);
        assertThat(entry.left.isDirectory).isEqualTo(isDir);
        assertThat(entry.right).isEqualTo(path);
    }

    public void testFileSynchronizeFolder() throws IOException {
        performSynchronizeFolder(Folder.FolderType.FILE);
    }

    public void testDocumentSynchronizeFolder() throws IOException {
        performSynchronizeFolder(Folder.FolderType.DOCUMENT);
    }

    private void performSynchronizeFolder(final Folder.FolderType typeSource) throws IOException {

        final String folderSyncInfoFilename = FolderUtils.FOLDER_SYNC_INFO_FILENAME;

        final Folder sourceFolder = Folder.fromFolder(createTestFolder(typeSource, "synchronizeFolder"), "source");
        final Folder targetFolder = Folder.fromFolder(createTestFolder(Folder.FolderType.FILE, "synchronizeFolder"), "target-" + typeSource);
        ContentStorage.get().ensureFolder(targetFolder, true);
        final File targetFolderFile = new File(targetFolder.getUri().getPath());
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE, "sync");
        final List<ImmutablePair<ContentStorage.FileInformation, String>> sourceFiles = FolderUtils.get().getAllFiles(sourceFolder);
        Collections.sort(sourceFiles, (e1, e2) -> e1.right.compareTo(e2.right));

        //create an additional file in source which should NOT be synced due to filter
        assertThat(ContentStorage.get().create(sourceFolder, "dontsync.txt")).isNotNull();

        //create some random files in target folder (which should be removed by sync)
        new File(targetFolderFile, "eee.txt").createNewFile();
        new File(targetFolderFile, "ccc").mkdirs();
        new File(targetFolderFile, "ccc/eee.txt").createNewFile();

        //create a file in target and mark it as "already synced" (by creating a fake entry in sync file properties)
        //->this file shall NOT get overridden in following sync
        final String fileSimName = "/ccc/ccc-ddd.txt";
        final String fileSimContent = "this is test content which should NOT get overridden by sync";
        final ContentStorage.FileInformation sourceSimSyncedInfo = sourceFiles.get(8).left;
        final File fileSimDir = new File(targetFolderFile, "ccc");
        fileSimDir.mkdirs();
        final Properties p = new Properties();
        p.setProperty("ccc-ddd.txt", sourceSimSyncedInfo.lastModified + "-" + sourceSimSyncedInfo.size);
        p.store(new FileOutputStream(new File(fileSimDir, folderSyncInfoFilename)), "test");
        writeToUri(Uri.fromFile(new File(fileSimDir, "ccc-ddd.txt")), fileSimContent);

        FolderUtils.get().synchronizeFolder(sourceFolder, targetFolderFile, fi -> !fi.name.equals("dontsync.txt"), null, null);

        //check if source and target files are identical
        final List<ImmutablePair<ContentStorage.FileInformation, String>> targetFiles = FolderUtils.get().getAllFiles(targetFolder);
        Collections.sort(targetFiles, (e1, e2) -> e1.right.compareTo(e2.right));
        final List<ImmutablePair<ContentStorage.FileInformation, String>> targetFilesWithoutSyncFiles = CollectionStream.of(targetFiles)
                .filter(e -> !e.right.endsWith("/" + folderSyncInfoFilename)).toList();

        assertThat(targetFilesWithoutSyncFiles).hasSize(sourceFiles.size());
        for (int i = 0; i < sourceFiles.size(); i++) {
            assertThat(targetFilesWithoutSyncFiles.get(i).right).isEqualTo(sourceFiles.get(i).right);
            assertThat(targetFilesWithoutSyncFiles.get(i).left.name).isEqualTo(sourceFiles.get(i).left.name);

            if (sourceFiles.get(i).right.equals(fileSimName)) {
                assertThat(readFromUri(targetFilesWithoutSyncFiles.get(i).left.uri)).isEqualTo(fileSimContent);
            } else if (!targetFilesWithoutSyncFiles.get(i).left.isDirectory) {
                assertThat(targetFilesWithoutSyncFiles.get(i).left.size).isEqualTo(sourceFiles.get(i).left.size);
                assertThat(readFromUri(targetFilesWithoutSyncFiles.get(i).left.uri)).isEqualTo(readFromUri(sourceFiles.get(i).left.uri));
            }
        }

        //check if written synchronization info files contain expected content
        final List<ImmutablePair<ContentStorage.FileInformation, String>> targetSyncFiles = CollectionStream.of(targetFiles)
                .filter(e -> e.right.endsWith("/" + folderSyncInfoFilename)).toList();
        assertThat(targetSyncFiles).hasSize(3); //root dir, "ccc" dir and "/ccc/ccc-ccc". (/ccc/ccc-bbb is empty and has no sync info file)

        assertPropertyContent(targetSyncFiles.get(0), folderSyncInfoFilename, "/",
                "aaa.txt", getFileSyncToken(sourceFiles.get(0).left),
                "bbb.txt", getFileSyncToken(sourceFiles.get(1).left),
                "ddd.txt", getFileSyncToken(sourceFiles.get(9).left));
        assertPropertyContent(targetSyncFiles.get(1), folderSyncInfoFilename, "/ccc/",
                "ccc-aaa.txt", getFileSyncToken(sourceFiles.get(3).left),
                "ccc-ddd.txt", getFileSyncToken(sourceFiles.get(8).left));
        assertPropertyContent(targetSyncFiles.get(2), folderSyncInfoFilename, "/ccc/ccc-ccc/",
                "ccc-ccc-aaa.txt", getFileSyncToken(sourceFiles.get(6).left),
                "ccc-ccc-bbb.txt", getFileSyncToken(sourceFiles.get(7).left));

    }

    private static String getFileSyncToken(final ContentStorage.FileInformation fi) {
        return fi.lastModified + "-" + fi.size;
    }

    private void assertPropertyContent(final ImmutablePair<ContentStorage.FileInformation, String> propFile, final String name, final String path, final String... entries) {
        assertThat(propFile.left.name).isEqualTo(name);
        assertThat(propFile.right).isEqualTo(path + name);

        try {
            final Properties p = new Properties();
            p.load(ContentStorage.get().openForRead(propFile.left.uri));
            assertThat(p.size()).isEqualTo(entries.length / 2);
            for (int i = 0; i < entries.length; i += 2) {
                assertThat(p.getProperty(entries[i])).isEqualTo(entries[i + 1]);
            }
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Could not read prop file " + propFile, ioe);
        }

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
        final Uri fileBeforeUri = ContentStorage.get().create(testFolder, "test-before.txt");
        list = ContentStorage.get().list(testFolder);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).name).isEqualTo("test-before.txt");

        //get that file
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test-before.txt").name).isEqualTo("test-before.txt");
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test-before.txt").uri).isEqualTo(fileBeforeUri);
        assertThat(ContentStorage.get().exists(testFolder, "test-before.txt")).isTrue();


        //rename the file
        final Uri fileUri = ContentStorage.get().rename(fileBeforeUri, FileNameCreator.forName("test.txt"));
        assertThat(list.get(0).name).isEqualTo("test-before.txt");

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
        list = ContentStorage.get().list(testFolder, true, false);
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
        list = ContentStorage.get().list(testFolder, true, false);
        // "subfolder" is alphabetically before "test.txt"
        assertThat(list).hasSize(2);
        assertThat(list.get(0).name).isEqualTo("subfolder");
        assertThat(list.get(0).isDirectory).isTrue();
        assertThat(list.get(0).dirLocation).isEqualTo(subfolder);
        assertThat(ContentStorage.get().getName(list.get(0).uri)).isEqualTo("subfolder");
        assertThat(list.get(1).name).isEqualTo("test.txt");
        assertThat(list.get(1).isDirectory).isFalse();
        assertThat(list.get(1).dirLocation).isNull();

        //create a file in the subfolder
        ContentStorage.get().create(subfolder, "subfolder-test.txt");
        assertThat(ContentStorage.get().getFileInfo(subfolder, "subfolder-test.txt").name).isEqualTo("subfolder-test.txt");
        assertThat(ContentStorage.get().getFileInfo(testFolder, "subfolder/subfolder-test.txt").name).isEqualTo("subfolder-test.txt");
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
                new String[]{"text/plain", "image/jpeg", "image/jpeg", "application/octet-stream", "application/octet-stream", "application/octet-stream", "application/octet-stream"});
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
        createTree(folder, structure, null);
    }

    private void createTree(final Folder folder, final String structure, final String content) {
        final JsonNode json = JsonUtils.toNode(structure);
        if (json == null) {
            throw new IllegalArgumentException("Invalid Json structure: " + structure);
        }
        createTree(folder, json, content);
    }

    private void createTree(final Folder folder, final JsonNode node, final String content) {
        final Iterator<JsonNode> it = node.elements();
        while (it.hasNext()) {
            final JsonNode n = it.next();
            if (n.isObject()) {
                //this is a subfolder
                final String folderName = n.get("name").asText();
                final Folder newFolder = Folder.fromFolder(folder, folderName);
                ContentStorage.get().ensureFolder(newFolder, true);
                createTree(newFolder, n.get("files"), content);
            } else {
                //this is a file
                final Uri uri = ContentStorage.get().create(folder, n.textValue());
                if (content != null) {
                    writeToUri(uri, content + "-" + n.textValue());
                }
            }
        }
    }

    private void writeToUri(final Uri uri, final String text) {
        try {
            IOUtils.write(text, ContentStorage.get().openForWrite(uri), "UTF-8");
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Could not write to " + uri, ioe);
        }
    }


    private String readFromUri(final Uri uri) {
        try {
            return IOUtils.readLines(ContentStorage.get().openForRead(uri), "UTF-8").get(0);
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Could not read from " + uri, ioe);
        }
    }

    private void assertCopyResult(final FolderUtils.FolderProcessResult result, final FolderUtils.ProcessResult expectedState, final int expectedFileCount, final int expectedDirCount) {
        assertThat(result.result).isEqualTo(expectedState);
        assertThat(result.filesModified).isEqualTo(expectedFileCount);
        assertThat(result.dirsModified).isEqualTo(expectedDirCount);
    }

    private void assertFileDirCount(final Folder folder, final int fileCount, final int dirCount) {
        final FolderUtils.FolderInfo folderInfo = FolderUtils.get().getFolderInfo(folder);
        assertThat(folderInfo.fileCount).as("File counts of Folder " + folder).isEqualTo(fileCount);
        assertThat(folderInfo.dirCount).as("Dir counts of Folder " + folder).isEqualTo(dirCount);
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
                    testFolder = Folder.fromFolder(getBaseTestFolder(Folder.FolderType.FILE), "doc-" + context);
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
