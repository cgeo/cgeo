package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.system.Os;
import android.system.StructStatVfs;

import androidx.core.util.Predicate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collection of higher-level utility functions for Folders.
 *
 * Since Folders can be based several base types (Files, DOcuments etc) they can be used uniquely
 * for all those types
 */
public class FolderUtils {

    private static final int COPY_FLAG_DIR_BEFORE  = 1;
    private static final int COPY_FLAG_DIR_NEEDED_FOR_TARGET = 2;

    private FolderStorage pls = FolderStorage.get();

    private static final FolderUtils INSTANCE = new FolderUtils();

    public static FolderUtils get() {
        return INSTANCE;
    }



    /** returns number of files (left) and number of (sub)dirs (right) currently in folder */
    public ImmutablePair<Integer, Integer> getFolderInfo(final Folder folder) {
        try (ContextLogger cLog = new ContextLogger("FolderUtils.getFolderInfo: %s", folder)) {

            final List<Integer> result = new ArrayList<>();
            result.add(0);
            result.add(0);
            treeWalk(folder, fi -> {
                if (fi.left.isDirectory && fi.right) {
                    result.set(1, result.get(1) + 1);
                }
                if (!fi.left.isDirectory) {
                    result.set(0, result.get(0) + 1);
                }
                return true;
            });
            cLog.add("#f:%d, #d:#%d", result.get(0), result.get(1));
            return new ImmutablePair<>(result.get(0), result.get(1));
        }
    }

    public boolean deleteAll(final Folder folder) {
        return treeWalk(folder, fi -> {
            if (fi.left.isDirectory) {
                if (fi.right) {
                    return true;
                } else {
                    return pls.delete(fi.left.uri);
                }
            } else {
                return pls.delete(fi.left.uri);
            }
        });
    }

    public enum CopyResult {
        OK, OK_NOTHING_TO_COPY, SOURCE_NOT_READABLE, TARGET_NOT_WRITEABLE, FAILURE_DURING_COPY, FAILURE_DURING_MOVE;

        public boolean isOk() {
            return name().startsWith("OK");
        }
    }

    /**
     * Copies the content of one folder into another. Source and target folder itself remain untouched.
     *
     * Implementation supports handling of case when source and target point to same folder (maybe via different APIs e.g. File vs Document)
     * as well as when source folder is inside target or vice versa
     *
     * @param source source folder with content to copy
     * @param target target folder to copy content to
     * @param move if true, content is MOVED (e.g. sdeleted in source)
     * @return result of copyAll call. "left" is status, "middle" is number of copied files, "right" is number of copied (sub)folders
     */
    public ImmutableTriple<CopyResult, Integer, Integer> copyAll(final Folder source, final Folder target, final boolean move) {

        //the following three-pass-copy/move is very complicated, but it ensures that copying/moving also works when target is a subdir of source or vice versa
        //For every change done here, please make sure that tests in FolderStorageTest are still passing!

        if (!pls.checkAvailability(source, false)) {
            return new ImmutableTriple<>(CopyResult.SOURCE_NOT_READABLE, 0, 0);
        }
        if (!pls.checkAvailability(target, true)) {
            return new ImmutableTriple<>(CopyResult.TARGET_NOT_WRITEABLE, 0, 0);
        }

        // -- first Pass: collect Information
        final List<ImmutableTriple<FolderStorage.FileInformation, Folder, Integer>> fileList = copyAllFirstPassCollectInfo(source, target);
        if (fileList == null) {
            return new ImmutableTriple<>(CopyResult.TARGET_NOT_WRITEABLE, 0, 0);
        } else if (fileList.isEmpty()) {
            return new ImmutableTriple<>(CopyResult.OK_NOTHING_TO_COPY, 0, 0);
        }

        // -- second Pass: do Copy
        final ImmutableTriple<Boolean, Integer, Integer> copyResult = copyAllSecondPassCopy(fileList);
        if (!copyResult.left) {
            return new ImmutableTriple<>(CopyResult.FAILURE_DURING_COPY, copyResult.middle, copyResult.right);
        }

        if (move && !copyAllThirdPassDelete(fileList)) {
            return new ImmutableTriple<>(CopyResult.FAILURE_DURING_MOVE, copyResult.middle, copyResult.right);
        }

        return new ImmutableTriple<>(CopyResult.OK, copyResult.middle, copyResult.right);

    }

    /** copyAll First Pass: collect all files to copy, create target folder for each file, mark source folders to keep on move (if target is in source) */
    @Nullable
    private List<ImmutableTriple<FolderStorage.FileInformation, Folder, Integer>> copyAllFirstPassCollectInfo(final Folder source, final Folder target) {

        //We create a "marker file" in the target folder so we recognize it in tree.
        // That way we can find out whether source=target or target is in source or source is in target
        final String targetMarkerFileName = FileNameCreator.DEFAULT.createName();
        final Uri targetMarkerFileUri = pls.create(target, targetMarkerFileName);
        if (targetMarkerFileUri == null) {
            //this means we can't write to target
            return null;
        }

        final int[] onTargetNeededPath = { -1 }; //helper counter to flag forlders needed for target
        final boolean[] markerFoundInSubdir = { false, false }; //helper flags to flag forlders needed for target
        //triplet of each entry will contain: source file, target folder for that file, flags as above
        final List<ImmutableTriple<FolderStorage.FileInformation, Folder, Integer>> listToCopy = new ArrayList<>();
        final Stack<Folder> targetFolderStack = new Stack<>();
        targetFolderStack.push(target);
        treeWalk(source, fi -> {
            if (fi.left.isDirectory) {
                if (fi.right) {
                    targetFolderStack.push(Folder.fromFolder(targetFolderStack.peek(), fi.left.name));
                    listToCopy.add(new ImmutableTriple<>(fi.left, targetFolderStack.peek(), COPY_FLAG_DIR_BEFORE));
                    if (onTargetNeededPath[0] >= 0) {
                        onTargetNeededPath[0]++;
                    }
                    return true;
                } else {
                    markerFoundInSubdir[1] |= onTargetNeededPath[0] == 0;
                    listToCopy.add(new ImmutableTriple<>(fi.left, targetFolderStack.pop(), onTargetNeededPath[0] == 0 ? COPY_FLAG_DIR_NEEDED_FOR_TARGET : 0));
                    if (onTargetNeededPath[0] > 0) {
                        onTargetNeededPath[0]--;
                    }
                    return true;
                }
            } else {
                Log.iForce("Visited " + fi.left + ", markerfile is: " + targetMarkerFileName);
                if (fi.left.name.equals(targetMarkerFileName)) {
                    markerFoundInSubdir[0] = true;
                    onTargetNeededPath[0] = 0;
                } else {
                    listToCopy.add(new ImmutableTriple<>(fi.left, targetFolderStack.peek(), 0));
                }
                return true;
            }
        });
        //delete marker file
        pls.delete(targetMarkerFileUri);

        final boolean sourceTargetSameDir = markerFoundInSubdir[0] && !markerFoundInSubdir[1];
        return sourceTargetSameDir ? Collections.emptyList() : listToCopy;
    }

    @NotNull
    private ImmutableTriple<Boolean, Integer, Integer> copyAllSecondPassCopy(final List<ImmutableTriple<FolderStorage.FileInformation, Folder, Integer>> fileList) {
        // -- second pass: make all necessary file copies and create necessary target subfolders
        int dirsCopied = 0;
        int filesCopied = 0;
        boolean success = true;
        for (ImmutableTriple<FolderStorage.FileInformation, Folder, Integer> file : fileList) {
            if (file.left.isDirectory) {
                if ((file.right & COPY_FLAG_DIR_BEFORE) > 0) {
                    if (pls.ensureFolder(file.middle)) {
                        dirsCopied++;
                    } else {
                        success = false;
                    }
                }
            } else {
                if (pls.copy(file.left.uri, file.middle, FileNameCreator.forName(file.left.name), false) != null) {
                    filesCopied++;
                } else {
                    success = false;
                }
            }
        }
        return new ImmutableTriple<>(success, filesCopied, dirsCopied);
    }

    private boolean copyAllThirdPassDelete(final List<ImmutableTriple<FolderStorage.FileInformation, Folder, Integer>> fileList) {
        boolean success = true;

        // -- third pass (only for move): delete all source files (leave out folders still needed for target)
        for (ImmutableTriple<FolderStorage.FileInformation, Folder, Integer> file : fileList) {
            if (file.left.isDirectory) {
                if (file.right == 0 && !pls.delete(file.left.uri)) {
                    success = false;

                }
            } else {
                if (!pls.delete(file.left.uri)) {
                    success = false;
                }
            }
        }
        return success;
    }

    /**
     * Generates a string representation of given folder as JSON string
     * @param root folder to generate string rep from
     * @param extendedInfo if false then only folder/file names are contained. If true, then more detailled info per file is contained
     * @param pretty if false then info is delivered as "flat string". If true, info is delivered "pretty-printed" with line-breaks etc
     * @return string rep of folder
     */
    public String folderContentToString(final Folder root, final boolean extendedInfo, final boolean pretty) {
        final Stack<ArrayNode> parents = new Stack<>();
        final ArrayNode currFolder = JsonUtils.createArrayNode();
        parents.push(currFolder);

        treeWalk(root, true, fi -> {
            if (fi.left.isDirectory) {
                if (fi.right) { //before
                    final ArrayNode newDirContent = JsonUtils.createArrayNode();
                    final ObjectNode newDirEntry = JsonUtils.createObjectNode();
                    newDirEntry.put("name", fi.left.name);
                    newDirEntry.set("files", newDirContent);
                    parents.peek().add(newDirEntry);
                    parents.push(newDirContent);
                    return true;
                } else {
                    parents.pop();
                    return true;
                }
            } else {
                if (extendedInfo) {
                    final ObjectNode fileEntry = JsonUtils.createObjectNode();
                    fileEntry.put("name", fi.left.name);
                    fileEntry.put("uri", String.valueOf(fi.left.uri));
                    fileEntry.put("mimeType", fi.left.mimeType);
                    parents.peek().add(fileEntry);
                } else {
                    parents.peek().add(fi.left.name);
                }
                return true;
            }
        });

        return pretty ? currFolder.toPrettyString() : currFolder.toString();

    }


    /** returns: left: free space on folder device (in bytes), right: number of files on device (may be -1 if not calculateable) */
    public ImmutablePair<Long, Long> getDeviceInfo(final Folder folder) {
        try {

            //get free space and number of files
            final ImmutablePair<Long, Long> freeSpaceAndNumberOfFiles;
            switch (folder.getBaseType()) {
                case DOCUMENT:
                    freeSpaceAndNumberOfFiles = getDeviceInfoForDocument(folder);
                    break;
                case FILE:
                default:
                    freeSpaceAndNumberOfFiles = new ImmutablePair<>(FileUtils.getFreeDiskSpace(new File(folder.getUri().getPath())), -1L);
                    break;
            }
            return freeSpaceAndNumberOfFiles;
        } catch (Exception e) {
            Log.i("Exception while getting system information for " + folder, e);
            return new ImmutablePair<>(-1L, -1L);
        }
    }


    /** Returns a pair of longs where left one is free space in bytes and right one is number of files */
    private ImmutablePair<Long, Long> getDeviceInfoForDocument(final Folder folder) throws Exception {

        if (CgeoApplication.getInstance() == null) {
            return new ImmutablePair<>(-1L, -1L);
        }

        final ImmutablePair<Long, Long> emptyResult = new ImmutablePair<>(-1L, -1L);

        final Uri treeUri = folder.getUri();
        if (treeUri == null) {
            return emptyResult;
        }
        final Uri docTreeUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
        if (docTreeUri == null) {
            return emptyResult;
        }

        final ParcelFileDescriptor pfd = CgeoApplication.getInstance().getApplicationContext().getContentResolver().openFileDescriptor(docTreeUri, "r");
        if (pfd == null) {
            return emptyResult;
        }
        final StructStatVfs stats = Os.fstatvfs(pfd.getFileDescriptor());
        if (stats == null) {
            return emptyResult;
        }

        return new ImmutablePair<>(stats.f_bavail * stats.f_bsize, stats.f_files);
    }



    private boolean treeWalk(final Folder root, final Predicate<ImmutablePair<FolderStorage.FileInformation, Boolean>> callback) {
        return treeWalk(root, false, callback);
    }

    private boolean treeWalk(final Folder root, final boolean ordered, final Predicate<ImmutablePair<FolderStorage.FileInformation, Boolean>> callback) {
        final List<FolderStorage.FileInformation> files = pls.list(root);
        if (ordered) {
            Collections.sort(files, (o1, o2) -> o1.name.compareTo(o2.name));
        }
        for (FolderStorage.FileInformation fi : files) {
            if (!callback.test(new ImmutablePair<>(fi, true))) {
                return false;
            }
            if (fi.isDirectory) {
                if (!treeWalk(fi.dirLocation, ordered, callback)) {
                    return false;
                }
                if (!callback.test(new ImmutablePair<>(fi, false))) {
                    return false;
                }
            }
        }
        return true;
    }

}
