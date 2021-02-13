package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.system.Os;
import android.system.StructStatVfs;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Predicate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final ContentStorage pls = ContentStorage.get();

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
        try (ContextLogger cLog = new ContextLogger("FolderUtils.deleteAll: %s", folder)) {

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
    }

    public enum CopyResultStatus { OK, SOURCE_NOT_READABLE, TARGET_NOT_WRITEABLE, FAILURE, ABORTED }

    /** value class holding the result of a completed copy process */
    public static class CopyResult {
        public final CopyResultStatus status;
        public final ContentStorage.FileInformation failedFile;
        public final int filesCopied;
        public final int dirsCopied;
        public final int filesInSource;
        public final int dirsInSource;

        public CopyResult(final CopyResultStatus status, final ContentStorage.FileInformation failedFile, final int filesCopied, final int dirsCopied, final int filesInSource, final int dirsInSource) {
            this.status = status;
            this.failedFile = failedFile;
            this.filesCopied = filesCopied;
            this.dirsCopied = dirsCopied;
            this.filesInSource = filesInSource;
            this.dirsInSource = dirsInSource;
        }
    }

    /** value class holding the current state of a concrete copy process which is currently running */
    public static class CopyStatus {
        public final ContentStorage.FileInformation currentFile;
        public final int filesCopied;
        public final int dirsCopied;
        public final int filesInSource;
        public final int dirsInSource;

        public CopyStatus(final ContentStorage.FileInformation currentFile, final int filesCopied, final int dirsCopied, final int filesInSource, final int dirsInSource) {
            this.currentFile = currentFile;
            this.filesCopied = filesCopied;
            this.dirsCopied = dirsCopied;
            this.filesInSource = filesInSource;
            this.dirsInSource = dirsInSource;
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
     * @return result of copyAll call.
     */
    public CopyResult copyAll(final Folder source, final Folder target, final boolean move) {
        return copyAll(source, target, move, null, null);
    }

    /**
     * Like {@link #copyAll(Folder, Folder, boolean)}, but performs the copy process asynchronously
     * and provides a GUI for it (progress bar and final screen with copy result)
     * @param activity activity to display copy Gui to
     * @param source source folder with content to copy
     * @param target target folder to copy content to
     * @param move if true, content is MOVED (e.g. sdeleted in source)
     * @param callback called after copying was done with copy result
     */
    public void copyAllAsynchronousWithGui(final Activity activity, final Folder source, final Folder target, final boolean move, final Consumer<CopyResult> callback) {
        new CopyTask(activity, source, target, move, copyResult ->
            displayCopyAllDoneDialog(activity, copyResult, source, target, move, callback)
        ).execute();
    }

    private void displayCopyAllDoneDialog(final Activity activity, final CopyResult copyResult, final Folder source, final Folder target, final boolean move, final Consumer<CopyResult> callback) {
        final String message = getCopyAllDoneMessage(activity, copyResult, source, target, move);

        Dialogs.newBuilder(activity)
            .setTitle(activity.getString(move ? R.string.folder_move_finished_title : R.string.folder_copy_finished_title))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, (dd, pp) -> {
                dd.dismiss();
                if (callback != null) {
                    callback.accept(copyResult);
                }
            })
            .setNegativeButton(android.R.string.cancel, (dd, pp) -> {
                dd.dismiss();
                if (callback != null) {
                    callback.accept(null);
                }
            })
            .create().show();
    }

    @NotNull
    private String getCopyAllDoneMessage(final Activity activity, final CopyResult copyResult, final Folder source, final Folder target, final boolean move) {

        final String filesCopied = copyResult.filesCopied < 0 ? "-" : "" + copyResult.filesCopied;
        final String filesTotal = copyResult.filesInSource < 0 ? "-" : plurals(activity, R.plurals.file_count, copyResult.filesInSource);
        final String foldersCopied = copyResult.dirsCopied < 0 ? "-" : "" + copyResult.dirsCopied;
        final String foldersTotal = copyResult.dirsInSource < 0 ? "-" : plurals(activity, R.plurals.folder_count, copyResult.dirsInSource);

        String message =
            activity.getString(move ? R.string.folder_move_finished_dialog_message : R.string.folder_copy_finished_dialog_message,
                source.toUserDisplayableString(), target.toUserDisplayableString(),
                filesCopied, filesTotal, foldersCopied, foldersTotal);

        if (copyResult.status != CopyResultStatus.OK) {
            message += "\n\n" + activity.getString(R.string.folder_copy_move_finished_dialog_message_failure, copyResult.status.toString(),
                copyResult.failedFile == null ? "---" : UriUtils.toUserDisplayableString(copyResult.failedFile.uri));
        }

        message += "\n\n" + activity.getString(R.string.folder_move_finished_dialog_tap);
        return message;
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
     * @param cancelFlag optional. If not null and flag is set to true during copy, then running copy/move process is aborted. Will result in a Copy Result ABORTED to be returned.
     * @param statusListener optional callback. This is called many times during copying to inform about copy status. May be used e.g. to implement a progress bar.
     *   status listener is called once initially (with files/dirsToCopy set to -1), then each time when a new file/dir is about to be copied, then one time when copying process is finished
     *   so when a dir is copied with e.g. 3 dirs and 7 files inside, then the statuslistener is called 2 + 3 + 7 times.
     * @return result of copyAll call.
     */
    public CopyResult copyAll(final Folder source, final Folder target, final boolean move, final AtomicBoolean cancelFlag, final Consumer<CopyStatus> statusListener)  {

        try (ContextLogger cLog = new ContextLogger("FolderUtils.copyAll: %s -> %s (move=%s)", source, target, move)) {

            //the following two-pass-copy/move is a bit complicated, but it ensures that copying/moving also works when target is a subdir of source or vice versa
            //For every change done here, please make sure that tests in ContentStorageTest are still passing!

            if (!pls.ensureFolder(source, false)) {
                return createCopyResult(CopyResultStatus.SOURCE_NOT_READABLE, null, 0, 0, null);
            }
            if (!pls.ensureFolder(target, true)) {
                return createCopyResult(CopyResultStatus.TARGET_NOT_WRITEABLE, null, 0, 0, null);
            }

            //initial status call
            sendCopyStatus(statusListener, null, 0, 0, null);

            // -- first Pass:createCopyResult collect Information
            final ImmutablePair<List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>>, ImmutablePair<Integer, Integer>> copyAllFirstPhaseResult = copyAllFirstPassCollectInfo(source, target, cancelFlag);
            final ImmutablePair<Integer, Integer> sourceCopyCount = copyAllFirstPhaseResult.right;
            if (isCancelled(cancelFlag)) {
                return createCopyResult(CopyResultStatus.ABORTED, null, 0, 0, sourceCopyCount);
            }
            final List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>> fileList = copyAllFirstPhaseResult.left;
            if (fileList == null) {
                return createCopyResult(CopyResultStatus.TARGET_NOT_WRITEABLE, null, 0, 0, sourceCopyCount);
            } else if (fileList.isEmpty()) {
                return createCopyResult(CopyResultStatus.OK, null, 0, 0, sourceCopyCount);
            }
            cLog.add("p1:#s", fileList.size());

            // -- second Pass: do Copy/move
            final ImmutableTriple<ContentStorage.FileInformation, Integer, Integer> copyResult = copyAllSecondPassCopyMove(fileList, move, statusListener, cancelFlag, sourceCopyCount);

            //final status call
            sendCopyStatus(statusListener, null, copyResult.middle, copyResult.right, sourceCopyCount);

            cLog.add("p2:#%s#%s", copyResult.middle, copyResult.right);

            return createCopyResult(
                isCancelled(cancelFlag) ? CopyResultStatus.ABORTED : (copyResult.left == null ? CopyResultStatus.OK : CopyResultStatus.FAILURE), copyResult.left, copyResult.middle, copyResult.right, sourceCopyCount);
        }

    }

    /**
     * copyAll First Pass: collect all files to copy, create target folder for each file, mark source folders to keep on move (if target is in source)
     * returns in left the list of files. Returns in right the files/dirs found in source for copying
     * */
    @Nullable
    private ImmutablePair<List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>>, ImmutablePair<Integer, Integer>> copyAllFirstPassCollectInfo(final Folder source, final Folder target, final AtomicBoolean cancelFlag) {

        //We create a "marker file" in the target folder so we recognize it in tree.
        // That way we can find out whether source=target or target is in source or source is in target
        final String targetMarkerFileName = FileNameCreator.DEFAULT.createName();
        final Uri targetMarkerFileUri = pls.create(target, targetMarkerFileName);
        if (targetMarkerFileUri == null) {
            //this means we can't write to target
            return null;
        }

        final int[] copyCounts = new int[]{0, 0};

        final int[] onTargetNeededPath = { -1 }; //helper counter to flag forlders needed for target
        final boolean[] markerFoundInSubdir = { false, false }; //helper flags to flag forlders needed for target
        //triplet of each entry will contain: source file, target folder for that file, flags as above
        final List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>> listToCopy = new ArrayList<>();
        final Stack<Folder> targetFolderStack = new Stack<>();
        targetFolderStack.push(target);
        treeWalk(source, fi -> {
            if (cancelFlag != null && cancelFlag.get()) {
                return false;
            }
            if (fi.left.isDirectory) {
                if (fi.right) {
                    copyCounts[1]++;
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
                if (fi.left.name.equals(targetMarkerFileName)) {
                    markerFoundInSubdir[0] = true;
                    onTargetNeededPath[0] = 0;
                } else {
                    copyCounts[0]++;
                    listToCopy.add(new ImmutableTriple<>(fi.left, targetFolderStack.peek(), 0));
                }
                return true;
            }
        });
        //delete marker file
        pls.delete(targetMarkerFileUri);

        final boolean sourceTargetSameDir = markerFoundInSubdir[0] && !markerFoundInSubdir[1];
        return new ImmutablePair<>(sourceTargetSameDir ? Collections.emptyList() : listToCopy, new ImmutablePair<>(copyCounts[0], copyCounts[1]));
    }

    @NotNull
    private ImmutableTriple<ContentStorage.FileInformation, Integer, Integer> copyAllSecondPassCopyMove(
        final List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>> fileList, final boolean move, final Consumer<CopyStatus> statusListener, final AtomicBoolean cancelFlag, final ImmutablePair<Integer, Integer> sourceCopyCount) {

        // -- second pass: make all necessary file copies and create necessary target subfolders
        int dirsCopied = 0;
        int filesCopied = 0;

        ContentStorage.FileInformation failedFile = null;
        for (ImmutableTriple<ContentStorage.FileInformation, Folder, Integer> file : fileList) {
            if (isCancelled(cancelFlag)) {
                break;
            }
            if (file.left.isDirectory) {
                if ((file.right & COPY_FLAG_DIR_BEFORE) > 0) {
                    sendCopyStatus(statusListener, file.left, filesCopied, dirsCopied, sourceCopyCount);
                    if (pls.ensureFolder(file.middle, true)) {
                        dirsCopied++;
                    } else {
                        failedFile = file.left;
                    }
                }
                if (move & file.right == 0 && !pls.delete(file.left.uri)) {
                    failedFile = file.left;
                }
            } else {
                sendCopyStatus(statusListener, file.left, filesCopied, dirsCopied, sourceCopyCount);
                if (pls.copy(file.left.uri, file.middle, FileNameCreator.forName(file.left.name), false) != null) {
                    filesCopied++;
                    if (move && !pls.delete(file.left.uri)) {
                        failedFile = file.left;
                    }
                } else {
                    failedFile = file.left;
                }
            }
            if (failedFile != null) {
                break;
            }
        }
        return new ImmutableTriple<>(failedFile, filesCopied, dirsCopied);
    }

    private boolean isCancelled(final AtomicBoolean cancelFlag) {
        return cancelFlag != null && cancelFlag.get();
    }

    private void sendCopyStatus(final Consumer<CopyStatus> statusListener, final ContentStorage.FileInformation fi, final int filesCopied, final int dirsCopied, final ImmutablePair<Integer, Integer> sourceCopyCount) {
        if (statusListener == null) {
            return;
        }
        statusListener.accept(new CopyStatus(fi, filesCopied, dirsCopied, sourceCopyCount == null ? -1 : sourceCopyCount.left, sourceCopyCount == null ? -1 : sourceCopyCount.right));
    }

    private CopyResult createCopyResult(final CopyResultStatus status, final ContentStorage.FileInformation failedFile, final int filesCopied, final int dirsCopied, final ImmutablePair<Integer, Integer> sourceCopyCount) {
        return new CopyResult(status, failedFile, filesCopied, dirsCopied, sourceCopyCount == null ? -1 : sourceCopyCount.left, sourceCopyCount == null ? -1 : sourceCopyCount.right);
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

    /**
     * checks whether source and target point to the same folder
     */
    public boolean foldersAreEqual(final Folder source, final Folder target) {
        if (source == null || target == null) {
            return false;
        }
        final String targetMarkerFileName = FileNameCreator.DEFAULT.createName();
        final Uri targetMarkerFileUri = pls.create(target, targetMarkerFileName);
        if (targetMarkerFileUri == null) {
            //this means we can't write to target
            return false;
        }
        try {
            final List<ContentStorage.FileInformation> files = pls.list(source);
            for (ContentStorage.FileInformation fi : files) {
                if (fi.name.equals(targetMarkerFileName)) {
                    return true;
                }
            }
        } finally {
            pls.delete(targetMarkerFileUri);
        }
        return false;
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
                    freeSpaceAndNumberOfFiles = new ImmutablePair<>(FileUtils.getFreeDiskSpace(
                        new File(ContentStorage.get().getUriForFolder(folder).getPath())), -1L);
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

        final Uri treeUri = ContentStorage.get().getUriForFolder(folder);
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



    private boolean treeWalk(final Folder root, final Predicate<ImmutablePair<ContentStorage.FileInformation, Boolean>> callback) {
        return treeWalk(root, false, callback);
    }

    private boolean treeWalk(final Folder root, final boolean ordered, final Predicate<ImmutablePair<ContentStorage.FileInformation, Boolean>> callback) {
        final List<ContentStorage.FileInformation> files = pls.list(root);
        if (ordered) {
            Collections.sort(files, (o1, o2) -> o1.name.compareTo(o2.name));
        }
        for (ContentStorage.FileInformation fi : files) {
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

    private static class CopyTask extends AsyncTaskWithProgressText<Void, CopyResult> {

        private final Folder source;
        private final Folder target;
        private final boolean doMove;
        private final Consumer<FolderUtils.CopyResult> callback;

        CopyTask(@NonNull final Activity activity, final Folder source, final Folder target, final boolean doMove, final Consumer<FolderUtils.CopyResult> callback) {
            super(
                activity,
                activity.getString(doMove ? R.string.folder_move_progressbar_title : R.string.folder_copy_progressbar_title, source.toUserDisplayableString(), target.toUserDisplayableString()),
                "---");
            this.source = source;
            this.target = target;
            this.doMove = doMove;
            this.callback = callback;
        }

        @Override
        protected FolderUtils.CopyResult doInBackgroundInternal(final Void[] params) {
            return FolderUtils.get().copyAll(source, target, doMove, null, ci -> {
                final String filesCopied = ci.filesCopied < 0 ? "-" : "" + ci.filesCopied;
                final String filesTotal = ci.filesInSource < 0 ? "-" : plurals(activity, R.plurals.file_count, ci.filesInSource);
                final String foldersCopied = ci.dirsCopied < 0 ? "-" : "" + ci.dirsCopied;
                final String foldersTotal = ci.dirsInSource < 0 ? "-" : plurals(activity, R.plurals.folder_count, ci.dirsInSource);

                final String statusString = activity.getString(doMove ? R.string.folder_move_progressbar_status_done : R.string.folder_copy_progressbar_status_done, filesCopied, filesTotal, foldersCopied, foldersTotal);
                final String progressString = activity.getString(R.string.folder_copy_move_progressbar_status_processed_file, ci.currentFile == null || ci.currentFile.name == null ? "" : ci.currentFile.name);
                publishProgress(statusString + "\n" + progressString);
            });
        }

        protected void onPostExecuteInternal(final FolderUtils.CopyResult result) {
            if (callback != null) {
                callback.accept(result);
            }
        }

    }

    private static String plurals(final Context context, final int id, final int quantity) {
        return context.getResources().getQuantityString(id, quantity, quantity);
    }


}
