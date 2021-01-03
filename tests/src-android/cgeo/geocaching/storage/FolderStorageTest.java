package cgeo.geocaching.storage;

import cgeo.CGeoTestCase;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.net.Uri;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FolderStorageTest extends CGeoTestCase {

    private String testFolderConfig = null;

    private static final boolean FAIL_DOC_TEST_IF_FOLDER_NOT_ACCESSIBLE = false; // CI does not support Document tests unfortunately...
    private static final boolean KEEP_RESULTS = true;

    //note: must be sorted alphabetically for comparison to work out!
    private static final String COMPLEX_FOLDER_STRUCTURE = "[\"aaa.txt\", \"bbb.txt\", {\"name\": \"ccc\", \"files\": [ \"ccc-aaa.txt\", { \"name\": \"ccc-bbb\", \"files\": [] }, { \"name\": \"ccc-ccc\", \"files\": [ \"ccc-ccc-aaa.txt\", \"ccc-ccc-bbb.txt\" ] }, \"ccc-ddd.txt\" ] }, \"ddd.txt\"]";


    public void setUp() throws Exception {
        super.setUp();
        //save TEST-FOLDER Uri if there is a user-defined one for later restoring
        if (ConfigurableFolder.TEST_FOLDER.isUserDefined()) {
            testFolderConfig = ConfigurableFolder.TEST_FOLDER.getFolder().toConfig();
        }
    }

    public void tearDown() throws Exception {
        cleanup();
        //restore test folder user-defined uri
        ConfigurableFolder.TEST_FOLDER.setUserDefinedFolder(Folder.fromConfig(testFolderConfig));

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

        final Uri uri = FolderStorage.get().create(testFolder, FileNameCreator.forName("cgeo-test.txt"));
        //final Folder subfolder = Folder.fromFolderLocation(testFolder, "eins");
        final Folder subsubfolder = Folder.fromFolder(testFolder, "eins/zwei");
        final Uri uri2 = FolderStorage.get().create(subsubfolder, FileNameCreator.forName("cgeo-test-sub.txt"));

        assertThat(FolderStorage.get().delete(uri)).isTrue();
        assertThat(FolderStorage.get().delete(uri2)).isTrue();
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
        ImmutableTriple<FolderUtils.CopyResult, Integer, Integer > result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false);
        assertThat(result).isEqualTo(new ImmutableTriple<>(FolderUtils.CopyResult.OK, 7, 3));
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false), COMPLEX_FOLDER_STRUCTURE);
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false), COMPLEX_FOLDER_STRUCTURE);

        //move
        FolderUtils.get().deleteAll(targetFolder);
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true);
        assertThat(result).isEqualTo(new ImmutableTriple<>(FolderUtils.CopyResult.OK, 7, 3));
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(sourceFolder, false, false), "[]");
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false), COMPLEX_FOLDER_STRUCTURE);
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

        final ImmutableTriple<FolderUtils.CopyResult, Integer, Integer> result = FolderUtils.get().copyAll(sourceTargetFolder, sourceTargetFolder, true);
        assertThat(result).isEqualTo(new ImmutableTriple<>(FolderUtils.CopyResult.OK_NOTHING_TO_COPY, 0, 0));

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
        ImmutableTriple<FolderUtils.CopyResult, Integer, Integer > result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false);
        assertThat(result).isEqualTo(new ImmutableTriple<>(FolderUtils.CopyResult.OK, 7, 3));
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
        assertThat(result).isEqualTo(new ImmutableTriple<>(FolderUtils.CopyResult.OK, 7, 3));
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
        ImmutableTriple<FolderUtils.CopyResult, Integer, Integer > result = FolderUtils.get().copyAll(sourceFolder, targetFolder, false);
        assertThat(result).isEqualTo(new ImmutableTriple<>(FolderUtils.CopyResult.OK, 4, 2));
        assertEqualsWithoutWhitespaces(FolderUtils.get().folderContentToString(targetFolder, false, false),
            "[\"aaa.txt\",\"bbb.txt\",{\"name\":\"ccc\",\"files\":[\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\"]},\"ccc-aaa.txt\",{\"name\":\"ccc-bbb\",\"files\":[]},{\"name\":\"ccc-ccc\",\"files\":[\"ccc-ccc-aaa.txt\",\"ccc-ccc-bbb.txt\"]},\"ccc-ddd.txt\",\"ddd.txt\"]"
            );

        //move
        FolderUtils.get().deleteAll(targetFolder);
        createTree(targetFolder, COMPLEX_FOLDER_STRUCTURE);
        result = FolderUtils.get().copyAll(sourceFolder, targetFolder, true);
        assertThat(result).isEqualTo(new ImmutableTriple<>(FolderUtils.CopyResult.OK, 4, 2));
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
        final Uri uri = FolderStorage.get().create(testFolder, FileNameCreator.forName("test"));
        final Uri uri2 = FolderStorage.get().create(testFolder, FileNameCreator.forName("test"));

        final Uri uriWithSuffix = FolderStorage.get().create(testFolder, FileNameCreator.forName("testwithsuffix.txt"));
        final Uri uriWithSuffix2 = FolderStorage.get().create(testFolder, FileNameCreator.forName("testwithsuffix.txt"));

        assertFileDirCount(testFolder, 4, 0);

        assertThat(UriUtils.getFileName(uri)).isEqualTo("test");
        assertThat(UriUtils.getFileName(uri)).isNotEqualTo(UriUtils.getFileName(uri2));

        assertThat(UriUtils.getFileName(uriWithSuffix)).isEqualTo("testwithsuffix.txt");
        assertThat(UriUtils.getFileName(uriWithSuffix)).isNotEqualTo(UriUtils.getFileName(uriWithSuffix2));
        assertThat(UriUtils.getFileName(uriWithSuffix2)).endsWith(".txt");
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

        final Uri uri = FolderStorage.get().create(testFolder, "test.txt");
        try (OutputStreamWriter writer = new OutputStreamWriter(FolderStorage.get().openForWrite(uri))) {
            writer.write(testtext);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(FolderStorage.get().openForRead(uri)))) {
            final String s = reader.readLine();
            assertThat(s).isEqualTo(testtext);
        }
    }

    public void testConfigurableFolderChangeNotification() {

        //create Location based on test folder. Several subfolders.
        final Folder folder = Folder.fromConfigurableFolder(ConfigurableFolder.TEST_FOLDER, "changeNotificatio");
        final Folder folderOne = Folder.fromFolder(folder, "one");
        final Folder folderTwo = Folder.fromFolder(folder, "two");
        final Folder folderNotNotified = Folder.fromConfigurableFolder(ConfigurableFolder.OFFLINE_MAPS, "changeNotificatio");

        final Set<String> notificationMessages = new HashSet<>();

        folderOne.registerChangeListener(this, p -> notificationMessages.add("one:" + p.name()));
        folderTwo.registerChangeListener(this, p -> notificationMessages.add("two:" + p.name()));
        folderNotNotified.registerChangeListener(this, p -> notificationMessages.add("notnotified:" + p.name()));

        //trigger change
        ConfigurableFolder.TEST_FOLDER.setUserDefinedFolder(null);

        //check
        assertThat(notificationMessages.size()).isEqualTo(2);
        assertThat(notificationMessages.contains("one:" + ConfigurableFolder.TEST_FOLDER.name()));
        assertThat(notificationMessages.contains("two:" + ConfigurableFolder.TEST_FOLDER.name()));
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
            new String[]{"txt", "jpg", "map"},
            new String[]{"text/plain", "image/jpeg", "application/octet-stream"});
    }

    private void performMimeTypeTests(final Folder testLocation, final String[] suffix, final String[] expectedMimeType) {
        final Map<String, String> mimeTypeMap = new HashMap<>();
        for (int i = 0; i < suffix.length; i++) {
            final String filename = "test." + suffix[i];
            assertThat(FolderStorage.get().create(testLocation, filename)).isNotNull();
            mimeTypeMap.put(filename, expectedMimeType[i]);
        }
        final List<FolderStorage.FileInformation> files = FolderStorage.get().list(testLocation);
        assertThat(files.size()).isEqualTo(suffix.length);
        for (FolderStorage.FileInformation fi : files) {
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
                FolderStorage.get().ensureFolder(newFolder);
                createTree(newFolder, n.get("files"));
            } else {
                //this is a file
                FolderStorage.get().create(folder, n.textValue());
            }
        }
    }

    private void assertFileDirCount(final Folder folder, final int fileCount, final int dirCount) {
        assertThat(FolderUtils.get().getFolderInfo(folder)).as("File counts of Folder " + folder).isEqualTo(new ImmutablePair<>(fileCount, dirCount));
    }

    private boolean hasValidDocumentTestFolder() {
        return ConfigurableFolder.TEST_FOLDER.isUserDefined() &&
            FolderStorage.get().checkAvailability(ConfigurableFolder.TEST_FOLDER.getFolder(), ConfigurableFolder.TEST_FOLDER.needsWrite(), true);
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
                        throw new IllegalArgumentException("Document Folder not accessible, test fails: " + ConfigurableFolder.TEST_FOLDER.getFolder());
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
                return ConfigurableFolder.TEST_FOLDER.getFolder();
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
