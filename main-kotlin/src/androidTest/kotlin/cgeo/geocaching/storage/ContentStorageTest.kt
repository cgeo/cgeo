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

import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.FileNameCreator
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.UriUtils

import android.net.Uri

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Properties
import java.util.Set
import java.util.concurrent.atomic.AtomicBoolean

import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ContentStorageTest {

    private var testFolderConfig: String = null

    private static val FAIL_DOC_TEST_IF_FOLDER_NOT_ACCESSIBLE: Boolean = false; // CI does not support Document tests unfortunately...
    private static val KEEP_RESULTS: Boolean = true

    //note: must be sorted alphabetically for comparison to work out!
    private static val COMPLEX_FOLDER_STRUCTURE: String = "[\"aaa.txt\", \"bbb.txt\", {\"name\": \"ccc\", \"files\": [ \"ccc-aaa.txt\", { \"name\": \"ccc-bbb\", \"files\": [] }, { \"name\": \"ccc-ccc\", \"files\": [ \"ccc-ccc-aaa.txt\", \"ccc-ccc-bbb.txt\" ] }, \"ccc-ddd.txt\" ] }, \"ddd.txt\"]"

    @Test
    public Unit testSimpleExample() throws IOException {

        //This is a simple example for usage of contentstore

        //List the content of the LOGFILES directory, write for each file its name, size and whether it is a directory
        val files: List<ContentStorage.FileInformation> = ContentStorage.get().list(PersistableFolder.LOGFILES)
        for (ContentStorage.FileInformation file : files) {
            Log.i("  File: " + file.name + ", size: " + file.size + ", isDir: " + file.isDirectory)
        }

        //Create a subfolder with name "my-unittest-subfolder" in LOGFILES
        val mysubfolder: Folder = Folder.fromPersistableFolder(PersistableFolder.LOGFILES, "my-unittest-subfolder")
        ContentStorage.get().ensureFolder(mysubfolder, true)

        //Create a file in this subfolder
        val myNewFile: Uri = ContentStorage.get().create(mysubfolder, "myNewFile.txt"); //in prod code: check for null!

        //write something to that File
        Writer writer = null
        try {
            val os: OutputStream = ContentStorage.get().openForWrite(myNewFile); //in prod code: check for null!
            writer = OutputStreamWriter(os, "UTF-8")
            writer.write("This is a test")

        } finally {
            IOUtils.closeQuietly(writer)
        }

        //read the same file out again
        BufferedReader reader = null
        try {
            val is: InputStream = ContentStorage.get().openForRead(myNewFile); //in prod code: check for null!
            reader = BufferedReader(InputStreamReader(is, "UTF-8"))
            val line: String = reader.readLine()
            assertThat(line).isEqualTo("This is a test")

        } finally {
            IOUtils.closeQuietly(reader)
        }

        //delete the created file
        ContentStorage.get().delete(myNewFile)

        //delete the created folder
        ContentStorage.get().delete(mysubfolder.getUri())

        //check out the other functions of ContentStorage
        //More complex operations (e.g. copyAll, deleteAll) can be found in class FolderUtils.

    }

    @Before
    public Unit setUp() {
        //save TEST-FOLDER Uri if there is a user-defined one for later restoring
        if (PersistableFolder.TEST_FOLDER.isUserDefined()) {
            testFolderConfig = PersistableFolder.TEST_FOLDER.getFolder().toConfig()
        }
    }

    @After
    public Unit tearDown() {
        cleanup()
        //restore test folder user-defined uri
        PersistableFolder.TEST_FOLDER.setUserDefinedFolder(Folder.fromConfig(testFolderConfig), false)
    }

    //a first small test to see how CI handles it
    @Test
    public Unit testFileSimpleCreateDelete() {
        performSimpleCreateDelete(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentSimpleCreateDelete() {
        performSimpleCreateDelete(Folder.FolderType.DOCUMENT)
    }

    private Unit performSimpleCreateDelete(final Folder.FolderType type) {

        val testFolder: Folder = createTestFolder(type, "simpleCreateDelete")

        val uri: Uri = ContentStorage.get().create(testFolder, "cgeo-test.txt")
        //val subfolder: Folder = Folder.fromFolderLocation(testFolder, "eins")
        val subsubfolder: Folder = Folder.fromFolder(testFolder, "eins/zwei")
        val uri2: Uri = ContentStorage.get().create(subsubfolder, "cgeo-test-sub.txt")

        assertThat(ContentStorage.get().delete(uri)).isTrue()
        assertThat(ContentStorage.get().delete(uri2)).isTrue()
    }

    @Test
    public Unit testFileStrangeNames() {
        val folder: Folder = createTestFolder(Folder.FolderType.FILE, "strangeNames")
        ContentStorage.get().ensureFolder(folder, true)

        val dir: File = File(folder.getUri().getPath())
        val f1: File = File(dir, "a b c")
        f1.mkdirs()

        FolderUtils.get().getFolderInfo(folder)

        val files: List<ContentStorage.FileInformation> = ContentStorage.get().list(folder)

        assertThat(files).hasSize(1)
        assertThat(files.get(0).name).isEqualTo("a b c")
    }

    @Test
    public Unit testFileCopyAll() {
        performCopyAll(Folder.FolderType.FILE, Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentCopyAll() {
        performCopyAll(Folder.FolderType.DOCUMENT, Folder.FolderType.DOCUMENT)
    }

    private Unit performCopyAll(final Folder.FolderType typeSource, final Folder.FolderType typeTarget) {

        val sourceFolder: Folder = Folder.fromFolder(createTestFolder(typeSource, "copyAll"), "source")
        val targetFolder: Folder = Folder.fromFolder(createTestFolder(typeTarget, "copyAll"), "target")

        //create something to copy in source Folder
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE)

        //copy
        FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false)
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3)
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false), COMPLEX_FOLDER_STRUCTURE)
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false), COMPLEX_FOLDER_STRUCTURE)

        //move
        assertThat(FolderUtils.get().deleteAll(targetFolder)).isTrue()
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true)
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3)
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false), "[]")
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false), COMPLEX_FOLDER_STRUCTURE)
    }

    @Test
    public Unit testFileCopyAllAbortAndStatus() {
        performCopyAllAbortAndStatus(Folder.FolderType.FILE, Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentCopyAllAbortAndStatus() {
        performCopyAllAbortAndStatus(Folder.FolderType.DOCUMENT, Folder.FolderType.DOCUMENT)
    }

    private Unit performCopyAllAbortAndStatus(final Folder.FolderType typeSource, final Folder.FolderType typeTarget) {

        val sourceFolder: Folder = Folder.fromFolder(createTestFolder(typeSource, "copyAllAbortAndStatus"), "source")
        val targetFolder: Folder = Folder.fromFolder(createTestFolder(typeTarget, "copyAllAbortAndStatus"), "target")
        val targetFolder2: Folder = Folder.fromFolder(createTestFolder(typeTarget, "copyAllAbortAndStatus"), "target2")

        //create something to copy in source Folder
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE)

        //copy complete
        val folderProcessStatuses: List<FolderUtils.FolderProcessStatus> = ArrayList<>()
        val cancelFlag: AtomicBoolean = AtomicBoolean(false)
        FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false, cancelFlag, folderProcessStatuses::add)
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3)
        //expect one initial status (with files/dirs to copy = -1), one status before (with files/dirs copied = 0 but known nr of files/dirs to copy) and then one for each file/dir
        assertThat(folderProcessStatuses).hasSize(2 + 3 + 7)
        assertThat(folderProcessStatuses.get(0).filesInSource).isEqualTo(-1)
        assertThat(folderProcessStatuses.get(0).dirsInSource).isEqualTo(-1)
        assertThat(folderProcessStatuses.get(0).filesProcessed + folderProcessStatuses.get(0).dirsProcessed).isEqualTo(0)
        Int prevSum = 0
        for (Int i = 1; i < 2 + 3 + 7; i++) {
            assertThat(folderProcessStatuses.get(i).filesInSource).isEqualTo(7)
            assertThat(folderProcessStatuses.get(i).dirsInSource).isEqualTo(3)
            assertThat(folderProcessStatuses.get(i).filesProcessed + folderProcessStatuses.get(i).dirsProcessed).isEqualTo(prevSum)
            prevSum++
        }

        //copy aborted
        folderProcessStatuses.clear()
        val abortAfter: Int = 6
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder2, false, cancelFlag, cs -> {
            folderProcessStatuses.add(cs)
            if (folderProcessStatuses.size() >= abortAfter) {
                cancelFlag.set(true)
            }
        })
        assertThat(result.result).isEqualTo(FolderUtils.ProcessResult.ABORTED)
        //expect one initial status (with files/dirs to copy = -1), one status before (with files/dirs copied = 0 but known nr of files/dirs to copy) and then one for each file/dir
        assertThat(folderProcessStatuses).hasSize(abortAfter + 1)
        assertThat(folderProcessStatuses.get(0).filesInSource).isEqualTo(-1)
        assertThat(folderProcessStatuses.get(0).dirsInSource).isEqualTo(-1)
        assertThat(folderProcessStatuses.get(0).filesProcessed + folderProcessStatuses.get(0).dirsProcessed).isEqualTo(0)
        prevSum = 0
        for (Int i = 1; i < abortAfter + 1; i++) {
            assertThat(folderProcessStatuses.get(i).filesInSource).isEqualTo(7)
            assertThat(folderProcessStatuses.get(i).dirsInSource).isEqualTo(3)
            assertThat(folderProcessStatuses.get(i).filesProcessed + folderProcessStatuses.get(i).dirsProcessed).isEqualTo(prevSum)
            prevSum++
        }
        //check that result status of different sources match also when copy was aborted
        assertThat(folderProcessStatuses.get(abortAfter).filesProcessed).isEqualTo(result.filesModified)
        assertThat(folderProcessStatuses.get(abortAfter).dirsProcessed).isEqualTo(result.dirsModified)

        assertFileDirCount(targetFolder2, result.filesModified, result.dirsModified)
    }

    @Test
    public Unit testFileCopyAllSameDir() {
        performCopyAllSameDir(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentCopyAllSameDir() {
        performCopyAllSameDir(Folder.FolderType.DOCUMENT)
    }


    private Unit performCopyAllSameDir(final Folder.FolderType typeSource) {

        val sourceTargetFolder: Folder = Folder.fromFolder(createTestFolder(typeSource, "copyAllSameDir"), "sourceTarget")

        createTree(sourceTargetFolder, COMPLEX_FOLDER_STRUCTURE)

        final FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceTargetFolder, sourceTargetFolder, true)
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 0, 0)

        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceTargetFolder, false, false), COMPLEX_FOLDER_STRUCTURE)
    }

    @Test
    public Unit testFileCopyAllTargetInSource() {
        performCopyAllTargetInSource(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentCopyAllTargetInSource() {
        performCopyAllTargetInSource(Folder.FolderType.DOCUMENT)
    }

    private Unit performCopyAllTargetInSource(final Folder.FolderType typeSource) {

        val sourceFolder: Folder = Folder.fromFolder(createTestFolder(typeSource, "copyAllTargetInSource"), "source")
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE)
        val targetFolder: Folder = Folder.fromFolder(sourceFolder, "ccc/ccc-ccc")

        //copy
        FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false)
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3)
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false),
                "[\"aaa.txt\", \"bbb.txt\", {\"name\": \"ccc\", \"files\": " +
                        "[ \"ccc-aaa.txt\", { \"name\": \"ccc-bbb\", \"files\": [] }, { \"name\": \"ccc-ccc\", \"files\": " +
                        "[ \"aaa.txt\", \"bbb.txt\", {\"name\": \"ccc\", \"files\": " +
                        "[ \"ccc-aaa.txt\", { \"name\": \"ccc-bbb\", \"files\": [] }, { \"name\": \"ccc-ccc\", \"files\": " +
                        "[\"ccc-ccc-aaa.txt\", \"ccc-ccc-bbb.txt\" ] }, " +
                        "\"ccc-ddd.txt\" ] " +
                        "}, \"ccc-ccc-aaa.txt\", \"ccc-ccc-bbb.txt\", \"ddd.txt\" ] " +
                        "}, \"ccc-ddd.txt\" ] " +
                        "}, \"ddd.txt\"]")

        //move
        FolderUtils.get().deleteAll(sourceFolder)
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE)
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true)
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 7, 3)
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false), COMPLEX_FOLDER_STRUCTURE)
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false),
                "[{\"name\": \"ccc\", \"files\": [ { \"name\": \"ccc-ccc\", \"files\": " +
                        COMPLEX_FOLDER_STRUCTURE + "} ] } ]")
    }

    @Test
    public Unit testFileCopyAllSourceInTarget() {
        performCopyAllSourceInTarget(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentCopyAllSourceInTarget() {
        performCopyAllSourceInTarget(Folder.FolderType.DOCUMENT)
    }

    private Unit performCopyAllSourceInTarget(final Folder.FolderType typeSource) {

        val targetFolder: Folder = Folder.fromFolder(createTestFolder(typeSource, "copyAllSourceInTarget"), "target")
        createTree(targetFolder, COMPLEX_FOLDER_STRUCTURE)
        val sourceFolder: Folder = Folder.fromFolder(targetFolder, "ccc")

        //copy
        FolderUtils.FolderProcessResult result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false)
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 4, 2)
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false),
                "[\"aaa.txt\",\"bbb.txt\",{\"name\":\"ccc\",\"files\":[\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\"]},\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\",\"ddd.txt\"]"
        )

        //move
        FolderUtils.get().deleteAll(targetFolder)
        createTree(targetFolder, COMPLEX_FOLDER_STRUCTURE)
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true)
        assertCopyResult(result, FolderUtils.ProcessResult.OK, 4, 2)
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false),
                "[\"aaa.txt\",\"bbb.txt\",{\"name\":\"ccc\",\"files\":[" +
                        //"\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\"" +
                        "]},\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\",\"ddd.txt\"]"
        )
    }

    @Test
    public Unit testFileGetAllFiles() {
        performGetAllFiles(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentGetAllFiles() {
        performGetAllFiles(Folder.FolderType.DOCUMENT)
    }

    private Unit performGetAllFiles(final Folder.FolderType typeSource) {

        val testFolder: Folder = createTestFolder(typeSource, "getAllFiles")
        createTree(testFolder, COMPLEX_FOLDER_STRUCTURE)

        final List<ImmutablePair<ContentStorage.FileInformation, String>> allFiles = FolderUtils.get().getAllFiles(testFolder)
        Collections.sort(allFiles, (e1, e2) -> e1.right.compareTo(e2.right))

        assertThat(allFiles).hasSize(10); //7 files, 3 dirs
        assertFileInfo(allFiles.get(0), "aaa.txt", false, "/aaa.txt")
        assertFileInfo(allFiles.get(1), "bbb.txt", false, "/bbb.txt")
        assertFileInfo(allFiles.get(2), "ccc", true, "/ccc")
        assertFileInfo(allFiles.get(3), "ccc-aaa.txt", false, "/ccc/ccc-aaa.txt")
        assertFileInfo(allFiles.get(4), "ccc-bbb", true, "/ccc/ccc-bbb")
        assertFileInfo(allFiles.get(5), "ccc-ccc", true, "/ccc/ccc-ccc")
        assertFileInfo(allFiles.get(6), "ccc-ccc-aaa.txt", false, "/ccc/ccc-ccc/ccc-ccc-aaa.txt")
        assertFileInfo(allFiles.get(7), "ccc-ccc-bbb.txt", false, "/ccc/ccc-ccc/ccc-ccc-bbb.txt")
        assertFileInfo(allFiles.get(8), "ccc-ddd.txt", false, "/ccc/ccc-ddd.txt")
        assertFileInfo(allFiles.get(9), "ddd.txt", false, "/ddd.txt")
    }

    @Test
    public Unit testFileGetFolderInfo() {
        performGetFolderInfo(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentGetFolderInfo() {
        performGetFolderInfo(Folder.FolderType.DOCUMENT)
    }

    private Unit performGetFolderInfo(final Folder.FolderType typeSource) {

        val testFolder: Folder = createTestFolder(typeSource, "getFolderInfo")
        createTree(testFolder, COMPLEX_FOLDER_STRUCTURE)

        FolderUtils.FolderInfo info = FolderUtils.get().getFolderInfo(testFolder, 1)
        assertThat(info.resultIsIncomplete).isTrue()
        assertThat(info.dirCount).isEqualTo(3); //subfolder "ccc" is scanned and contains two add. sufolders...
        assertThat(info.fileCount).isEqualTo(5); //...but the subfolders of ccc are NOT scanned any more, thus files in ccc-ccc don't count

        info = FolderUtils.get().getFolderInfo(testFolder, 0)
        assertThat(info.resultIsIncomplete).isTrue()
        assertThat(info.dirCount).isEqualTo(1); //only subfolder "ccc" is recognized...
        assertThat(info.fileCount).isEqualTo(3); //...but files in "ccc" and "ccc-ccc" are NOT

        info = FolderUtils.get().getFolderInfo(testFolder, -1)
        assertThat(info.resultIsIncomplete).isFalse()
        assertThat(info.dirCount).isEqualTo(3)
        assertThat(info.fileCount).isEqualTo(7)

        info = FolderUtils.get().getFolderInfo(testFolder, 3)
        assertThat(info.resultIsIncomplete).isTrue()
        assertThat(info.dirCount).isEqualTo(3)
        assertThat(info.fileCount).isEqualTo(7)

    }

    private Unit assertFileInfo(final ImmutablePair<ContentStorage.FileInformation, String> entry, final String fileName, final Boolean isDir, final String path) {
        assertThat(entry.left.name).isEqualTo(fileName)
        assertThat(entry.left.isDirectory).isEqualTo(isDir)
        assertThat(entry.right).isEqualTo(path)
    }

    @Test
    public Unit testFileSynchronizeFolder() throws IOException {
        performSynchronizeFolder(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentSynchronizeFolder() throws IOException {
        performSynchronizeFolder(Folder.FolderType.DOCUMENT)
    }

    private Unit performSynchronizeFolder(final Folder.FolderType typeSource) throws IOException {

        val folderSyncInfoFilename: String = FolderUtils.FOLDER_SYNC_INFO_FILENAME

        val sourceFolder: Folder = Folder.fromFolder(createTestFolder(typeSource, "synchronizeFolder"), "source")
        val targetFolder: Folder = Folder.fromFolder(createTestFolder(Folder.FolderType.FILE, "synchronizeFolder"), "target-" + typeSource)
        ContentStorage.get().ensureFolder(targetFolder, true)
        val targetFolderFile: File = File(targetFolder.getUri().getPath())
        createTree(sourceFolder, COMPLEX_FOLDER_STRUCTURE, "sync")
        final List<ImmutablePair<ContentStorage.FileInformation, String>> sourceFiles = FolderUtils.get().getAllFiles(sourceFolder)
        Collections.sort(sourceFiles, (e1, e2) -> e1.right.compareTo(e2.right))

        //create an additional file in source which should NOT be synced due to filter
        assertThat(ContentStorage.get().create(sourceFolder, "dontsync.txt")).isNotNull()

        //create some random files in target folder (which should be removed by sync)
        File(targetFolderFile, "eee.txt").createNewFile()
        File(targetFolderFile, "ccc").mkdirs()
        File(targetFolderFile, "ccc/eee.txt").createNewFile()

        //create a file in target and mark it as "already synced" (by creating a fake entry in sync file properties)
        //->this file shall NOT get overridden in following sync
        val fileSimName: String = "/ccc/ccc-ddd.txt"
        val fileSimContent: String = "this is test content which should NOT get overridden by sync"
        final ContentStorage.FileInformation sourceSimSyncedInfo = sourceFiles.get(8).left
        val fileSimDir: File = File(targetFolderFile, "ccc")
        fileSimDir.mkdirs()
        val p: Properties = Properties()
        p.setProperty("ccc-ddd.txt", sourceSimSyncedInfo.lastModified + "-" + sourceSimSyncedInfo.size)
        p.store(FileOutputStream(File(fileSimDir, folderSyncInfoFilename)), "test")
        writeToUri(Uri.fromFile(File(fileSimDir, "ccc-ddd.txt")), fileSimContent)

        FolderUtils.get().synchronizeFolder(sourceFolder, targetFolderFile, fi -> !fi.name == ("dontsync.txt"), null, null)

        //check if source and target files are identical
        final List<ImmutablePair<ContentStorage.FileInformation, String>> targetFiles = FolderUtils.get().getAllFiles(targetFolder)
        Collections.sort(targetFiles, (e1, e2) -> e1.right.compareTo(e2.right))
        final List<ImmutablePair<ContentStorage.FileInformation, String>> targetFilesWithoutSyncFiles = CollectionStream.of(targetFiles)
                .filter(e -> !e.right.endsWith("/" + folderSyncInfoFilename)).toList()

        assertThat(targetFilesWithoutSyncFiles).hasSize(sourceFiles.size())
        for (Int i = 0; i < sourceFiles.size(); i++) {
            assertThat(targetFilesWithoutSyncFiles.get(i).right).isEqualTo(sourceFiles.get(i).right)
            assertThat(targetFilesWithoutSyncFiles.get(i).left.name).isEqualTo(sourceFiles.get(i).left.name)

            if (sourceFiles.get(i).right == (fileSimName)) {
                assertThat(readFromUri(targetFilesWithoutSyncFiles.get(i).left.uri)).isEqualTo(fileSimContent)
            } else if (!targetFilesWithoutSyncFiles.get(i).left.isDirectory) {
                assertThat(targetFilesWithoutSyncFiles.get(i).left.size).isEqualTo(sourceFiles.get(i).left.size)
                assertThat(readFromUri(targetFilesWithoutSyncFiles.get(i).left.uri)).isEqualTo(readFromUri(sourceFiles.get(i).left.uri))
            }
        }

        //check if written synchronization info files contain expected content
        final List<ImmutablePair<ContentStorage.FileInformation, String>> targetSyncFiles = CollectionStream.of(targetFiles)
                .filter(e -> e.right.endsWith("/" + folderSyncInfoFilename)).toList()
        assertThat(targetSyncFiles).hasSize(3); //root dir, "ccc" dir and "/ccc/ccc-ccc". (/ccc/ccc-bbb is empty and has no sync info file)

        assertPropertyContent(targetSyncFiles.get(0), folderSyncInfoFilename, "/",
                "aaa.txt", getFileSyncToken(sourceFiles.get(0).left),
                "bbb.txt", getFileSyncToken(sourceFiles.get(1).left),
                "ddd.txt", getFileSyncToken(sourceFiles.get(9).left))
        assertPropertyContent(targetSyncFiles.get(1), folderSyncInfoFilename, "/ccc/",
                "ccc-aaa.txt", getFileSyncToken(sourceFiles.get(3).left),
                "ccc-ddd.txt", getFileSyncToken(sourceFiles.get(8).left))
        assertPropertyContent(targetSyncFiles.get(2), folderSyncInfoFilename, "/ccc/ccc-ccc/",
                "ccc-ccc-aaa.txt", getFileSyncToken(sourceFiles.get(6).left),
                "ccc-ccc-bbb.txt", getFileSyncToken(sourceFiles.get(7).left))

    }

    private static String getFileSyncToken(final ContentStorage.FileInformation fi) {
        return fi.lastModified + "-" + fi.size
    }

    private Unit assertPropertyContent(final ImmutablePair<ContentStorage.FileInformation, String> propFile, final String name, final String path, final String... entries) {
        assertThat(propFile.left.name).isEqualTo(name)
        assertThat(propFile.right).isEqualTo(path + name)

        try {
            val p: Properties = Properties()
            p.load(ContentStorage.get().openForRead(propFile.left.uri))
            assertThat(p.size()).isEqualTo(entries.length / 2)
            for (Int i = 0; i < entries.length; i += 2) {
                assertThat(p.getProperty(entries[i])).isEqualTo(entries[i + 1])
            }
        } catch (IOException ioe) {
            throw IllegalArgumentException("Could not read prop file " + propFile, ioe)
        }

    }


    private Unit assertEqualsWithoutWhitespaces(final String value, final String expected) {
        assertThat(value.replaceAll("[\\s]", "")).isEqualTo(expected.replaceAll("[\\s]", ""))
    }

    @Test
    public Unit testFileCreateUniqueFilenames() {
        performCreateUniqueFilenames(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentCreateUniqueFilenames() {
        performCreateUniqueFilenames(Folder.FolderType.DOCUMENT)
    }

    private Unit performCreateUniqueFilenames(final Folder.FolderType type) {

        val testFolder: Folder = createTestFolder(type, "createUniqueFilenames")

        assertFileDirCount(testFolder, 0, 0)

        //create two times with same name
        val uri: Uri = ContentStorage.get().create(testFolder, "test")
        val uri2: Uri = ContentStorage.get().create(testFolder, "test")

        val uriWithSuffix: Uri = ContentStorage.get().create(testFolder, "testwithsuffix.txt")
        val uriWithSuffix2: Uri = ContentStorage.get().create(testFolder, "testwithsuffix.txt")

        assertFileDirCount(testFolder, 4, 0)

        assertThat(UriUtils.getLastPathSegment(uri)).isEqualTo("test")
        assertThat(UriUtils.getLastPathSegment(uri)).isNotEqualTo(UriUtils.getLastPathSegment(uri2))

        assertThat(UriUtils.getLastPathSegment(uriWithSuffix)).isEqualTo("testwithsuffix.txt")
        assertThat(UriUtils.getLastPathSegment(uriWithSuffix)).isNotEqualTo(UriUtils.getLastPathSegment(uriWithSuffix2))
        assertThat(UriUtils.getLastPathSegment(uriWithSuffix2)).endsWith(".txt")
    }

    @Test
    public Unit testFileWriteReadFile() throws IOException {
        performWriteReadFile(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentWriteReadFile() throws IOException {
        performWriteReadFile(Folder.FolderType.DOCUMENT)
    }


    private Unit performWriteReadFile(final Folder.FolderType type) throws IOException {

        val testFolder: Folder = createTestFolder(type, "createUniqueFilenames")

        val testtext: String = "This is a test text"

        val uri: Uri = ContentStorage.get().create(testFolder, "test.txt")

        //write to file
        try (OutputStreamWriter writer = OutputStreamWriter(ContentStorage.get().openForWrite(uri))) {
            writer.write(testtext)
        }
        try (BufferedReader reader = BufferedReader(InputStreamReader(ContentStorage.get().openForRead(uri)))) {
            val s: String = reader.readLine()
            assertThat(s).isEqualTo(testtext)
        }

        //append
        try (OutputStreamWriter writer = OutputStreamWriter(ContentStorage.get().openForWrite(uri, true))) {
            writer.write(testtext)
        }
        try (BufferedReader reader = BufferedReader(InputStreamReader(ContentStorage.get().openForRead(uri)))) {
            val s: String = reader.readLine()
            assertThat(s).isEqualTo(testtext + testtext)
        }

        //overwrite
        try (OutputStreamWriter writer = OutputStreamWriter(ContentStorage.get().openForWrite(uri))) {
            writer.write(testtext)
        }
        try (BufferedReader reader = BufferedReader(InputStreamReader(ContentStorage.get().openForRead(uri)))) {
            val s: String = reader.readLine()
            assertThat(s).isEqualTo(testtext)
        }
    }

    @Test
    public Unit testFileBasicFolderOperations() throws IOException {
        performBasicFolderOperations(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentBasicFolderOperations() throws IOException {
        performBasicFolderOperations(Folder.FolderType.DOCUMENT)
    }


    private Unit performBasicFolderOperations(final Folder.FolderType type) throws IOException {

        val testFolder: Folder = createTestFolder(type, "basicFolderOperations")

        List<ContentStorage.FileInformation> list = ContentStorage.get().list(testFolder)
        assertThat(list).isEmpty()

        //test that some methods fail as expected
        assertThat(ContentStorage.get().exists(testFolder, "test-nonexisting.txt")).isFalse()
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test-nonexisting.txt")).isNull()
        assertThat(ContentStorage.get().delete(Uri.fromFile(File("/test/test.txt")))).isFalse()
        assertThat(ContentStorage.get().getName(Uri.fromFile(File("/test/test.txt")))).isNull()
        assertThat(ContentStorage.get().exists(testFolder, null)).isFalse()
        assertThat(ContentStorage.get().getFileInfo(testFolder, null)).isNull()
        assertThat(ContentStorage.get().delete(null)).isFalse()
        assertThat(ContentStorage.get().getName(null)).isNull()

        //create a file
        val fileBeforeUri: Uri = ContentStorage.get().create(testFolder, "test-before.txt")
        list = ContentStorage.get().list(testFolder)
        assertThat(list).hasSize(1)
        assertThat(list.get(0).name).isEqualTo("test-before.txt")

        //get that file
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test-before.txt").name).isEqualTo("test-before.txt")
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test-before.txt").uri).isEqualTo(fileBeforeUri)
        assertThat(ContentStorage.get().exists(testFolder, "test-before.txt")).isTrue()


        //rename the file
        val fileUri: Uri = ContentStorage.get().rename(fileBeforeUri, FileNameCreator.forName("test.txt"))
        assertThat(list.get(0).name).isEqualTo("test-before.txt")

        //get that file
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test.txt").name).isEqualTo("test.txt")
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test.txt").uri).isEqualTo(fileUri)
        assertThat(ContentStorage.get().exists(testFolder, "test.txt")).isTrue()

        //create file with same name, ask for returning same and check if is is in fact the same
        val file2Uri: Uri = ContentStorage.get().create(testFolder, FileNameCreator.forName("test.txt"), true)
        assertThat(file2Uri).isEqualTo(fileUri)
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test.txt").name).isEqualTo("test.txt")
        assertThat(ContentStorage.get().getFileInfo(testFolder, "test.txt").uri).isEqualTo(fileUri)
        list = ContentStorage.get().list(testFolder)
        assertThat(list).hasSize(1)
        assertThat(list.get(0).name).isEqualTo("test.txt")

        //create fiole with same name, ask for creating it anew
        val file3Uri: Uri = ContentStorage.get().create(testFolder, FileNameCreator.forName("test.txt"), false)
        val newName: String = ContentStorage.get().getName(file3Uri)
        assertThat(newName).startsWith("test")
        assertThat(newName).endsWith(".txt")

        assertThat(file3Uri).isNotEqualTo(fileUri)
        final ContentStorage.FileInformation fileInfo = ContentStorage.get().getFileInfo(testFolder, newName)
        assertThat(fileInfo.name).isEqualTo(newName)
        assertThat(fileInfo.uri).isNotEqualTo(fileUri)
        list = ContentStorage.get().list(testFolder, true, false)
        assertThat(list).hasSize(2)
        val names: Set<String> = HashSet<>()
        for (ContentStorage.FileInformation fi : list) {
            names.add(fi.name)
        }
        assertThat(names).contains("test.txt", newName)

        //delete the second file
        assertThat(ContentStorage.get().delete(file3Uri)).isTrue()
        list = ContentStorage.get().list(testFolder)
        assertThat(list).hasSize(1)
        assertThat(list.get(0).name).isEqualTo("test.txt")

        //create a subfolder
        val subfolder: Folder = Folder.fromFolder(testFolder, "subfolder")
        assertThat(ContentStorage.get().ensureFolder(subfolder, true)).isTrue()
        list = ContentStorage.get().list(testFolder, true, false)
        // "subfolder" is alphabetically before "test.txt"
        assertThat(list).hasSize(2)
        assertThat(list.get(0).name).isEqualTo("subfolder")
        assertThat(list.get(0).isDirectory).isTrue()
        assertThat(list.get(0).dirLocation).isEqualTo(subfolder)
        assertThat(ContentStorage.get().getName(list.get(0).uri)).isEqualTo("subfolder")
        assertThat(list.get(1).name).isEqualTo("test.txt")
        assertThat(list.get(1).isDirectory).isFalse()
        assertThat(list.get(1).dirLocation).isNull()

        //create a file in the subfolder
        ContentStorage.get().create(subfolder, "subfolder-test.txt")
        assertThat(ContentStorage.get().getFileInfo(subfolder, "subfolder-test.txt").name).isEqualTo("subfolder-test.txt")
        assertThat(ContentStorage.get().getFileInfo(testFolder, "subfolder/subfolder-test.txt").name).isEqualTo("subfolder-test.txt")
    }

    @Test
    public Unit testPersistableFolderChangeNotification() {

        //create Location based on test folder. Several subfolders.
        val folder: Folder = Folder.fromPersistableFolder(PersistableFolder.TEST_FOLDER, "changeNotificatio")
        val folderOne: Folder = Folder.fromFolder(folder, "one")
        val folderTwo: Folder = Folder.fromFolder(folder, "two")
        val folderNotNotified: Folder = Folder.fromPersistableFolder(PersistableFolder.OFFLINE_MAPS, "changeNotificatio")

        val notificationMessages: Set<String> = HashSet<>()

        folderOne.registerChangeListener(this, p -> notificationMessages.add("one:" + p.name()))
        folderTwo.registerChangeListener(this, p -> notificationMessages.add("two:" + p.name()))
        folderNotNotified.registerChangeListener(this, p -> notificationMessages.add("notnotified:" + p.name()))

        //trigger change
        PersistableFolder.TEST_FOLDER.setUserDefinedFolder(null, false)

        //check
        assertThat(notificationMessages.size()).isEqualTo(2)
        assertThat(notificationMessages.contains("one:" + PersistableFolder.TEST_FOLDER.name()))
        assertThat(notificationMessages.contains("two:" + PersistableFolder.TEST_FOLDER.name()))
    }

    @Test
    public Unit testFileMimeType() {
        performMimeType(Folder.FolderType.FILE)
    }

    @Test
    public Unit testDocumentMimeType() {
        performMimeType(Folder.FolderType.DOCUMENT)
    }

    private Unit performMimeType(final Folder.FolderType type) {

        val testFolder: Folder = createTestFolder(type, "mimeType")

        performMimeTypeTests(testFolder,
                String[]{"txt", "jpg", "jpeg", "map", "hprof", "gpx", null},
                String[]{"text/plain", "image/jpeg", "image/jpeg", "application/octet-stream", "application/octet-stream", "application/octet-stream", "application/octet-stream"})
    }

    private Unit performMimeTypeTests(final Folder testLocation, final String[] suffix, final String[] expectedMimeType) {
        val mimeTypeMap: Map<String, String> = HashMap<>()
        for (Int i = 0; i < suffix.length; i++) {
            val filename: String = "test" + (suffix[i] == null ? "" : "." + suffix[i])
            assertThat(ContentStorage.get().create(testLocation, filename)).isNotNull()
            mimeTypeMap.put(filename, expectedMimeType[i])
        }
        val files: List<ContentStorage.FileInformation> = ContentStorage.get().list(testLocation)
        assertThat(files.size()).isEqualTo(suffix.length)
        for (ContentStorage.FileInformation fi : files) {
            assertThat(mimeTypeMap.containsKey(fi.name)).as("Unexpected File " + fi.name).isTrue()
            assertThat(fi.mimeType).as("For file " + fi.name).isEqualTo(mimeTypeMap.get(fi.name))
        }
    }

    private Unit createTree(final Folder folder, final String structure) {
        createTree(folder, structure, null)
    }

    private Unit createTree(final Folder folder, final String structure, final String content) {
        val json: JsonNode = JsonUtils.stringToNode(structure)
        if (json == null) {
            throw IllegalArgumentException("Invalid Json structure: " + structure)
        }
        createTree(folder, json, content)
    }

    private Unit createTree(final Folder folder, final JsonNode node, final String content) {
        val it: Iterator<JsonNode> = node.elements()
        while (it.hasNext()) {
            val n: JsonNode = it.next()
            if (n.isObject()) {
                //this is a subfolder
                val folderName: String = n.get("name").asText()
                val newFolder: Folder = Folder.fromFolder(folder, folderName)
                ContentStorage.get().ensureFolder(newFolder, true)
                createTree(newFolder, n.get("files"), content)
            } else {
                //this is a file
                val uri: Uri = ContentStorage.get().create(folder, n.textValue())
                if (content != null) {
                    writeToUri(uri, content + "-" + n.textValue())
                }
            }
        }
    }

    private Unit writeToUri(final Uri uri, final String text) {
        try {
            IOUtils.write(text, ContentStorage.get().openForWrite(uri), "UTF-8")
        } catch (IOException ioe) {
            throw IllegalArgumentException("Could not write to " + uri, ioe)
        }
    }


    private String readFromUri(final Uri uri) {
        try {
            return IOUtils.readLines(ContentStorage.get().openForRead(uri), "UTF-8").get(0)
        } catch (IOException ioe) {
            throw IllegalArgumentException("Could not read from " + uri, ioe)
        }
    }

    private Unit assertCopyResult(final FolderUtils.FolderProcessResult result, final FolderUtils.ProcessResult expectedState, final Int expectedFileCount, final Int expectedDirCount) {
        assertThat(result.result).isEqualTo(expectedState)
        assertThat(result.filesModified).isEqualTo(expectedFileCount)
        assertThat(result.dirsModified).isEqualTo(expectedDirCount)
    }

    private Unit assertFileDirCount(final Folder folder, final Int fileCount, final Int dirCount) {
        final FolderUtils.FolderInfo folderInfo = FolderUtils.get().getFolderInfo(folder)
        assertThat(folderInfo.fileCount).as("File counts of Folder " + folder).isEqualTo(fileCount)
        assertThat(folderInfo.dirCount).as("Dir counts of Folder " + folder).isEqualTo(dirCount)
    }

    private Boolean hasValidDocumentTestFolder() {
        return PersistableFolder.TEST_FOLDER.isUserDefined() &&
                ContentStorage.get().ensureFolder(PersistableFolder.TEST_FOLDER.getFolder(), PersistableFolder.TEST_FOLDER.needsWrite(), true)
    }

    private Folder createTestFolder(final Folder.FolderType type, final String context) {

        final Folder testFolder
        switch (type) {
            case FILE:
                testFolder = Folder.fromFolder(getBaseTestFolder(Folder.FolderType.FILE), "file-" + context)
                break
            case DOCUMENT:
                if (!hasValidDocumentTestFolder()) {
                    if (FAIL_DOC_TEST_IF_FOLDER_NOT_ACCESSIBLE) {
                        throw IllegalArgumentException("Document Folder not accessible, test fails: " + PersistableFolder.TEST_FOLDER.getFolder())
                    }
                    Log.iForce("Trying to test for DocumentUri fails; unfortunately there is no DocumentUri configured for TEST-FOLDER. Test with file instead")
                    testFolder = Folder.fromFolder(getBaseTestFolder(Folder.FolderType.FILE), "doc-" + context)
                } else {
                    testFolder = Folder.fromFolder(getBaseTestFolder(Folder.FolderType.DOCUMENT), context)
                }
                break
            default:
                testFolder = null
        }

        if (testFolder != null) {
            FolderUtils.get().deleteAll(testFolder)
        }
        return testFolder
    }

    private Folder getBaseTestFolder(final Folder.FolderType type) {
        switch (type) {
            case FILE:
                return Folder.fromFolder(Folder.CGEO_PRIVATE_FILES, "unittest")
            case DOCUMENT:
                if (!hasValidDocumentTestFolder()) {
                    return Folder.fromFolder(Folder.CGEO_PRIVATE_FILES, "unittest")
                }
                return PersistableFolder.TEST_FOLDER.getFolder()
            default:
                return null
        }
    }

    private Unit cleanup() {
        if (KEEP_RESULTS) {
            return
        }
        FolderUtils.get().deleteAll(getBaseTestFolder(Folder.FolderType.FILE))
        if (hasValidDocumentTestFolder()) {
            FolderUtils.get().deleteAll(getBaseTestFolder(Folder.FolderType.DOCUMENT))
        }
    }
}
