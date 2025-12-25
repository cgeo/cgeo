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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.AsyncTaskWithProgressText
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.FileNameCreator
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.UriUtils
import cgeo.geocaching.utils.functions.Func1

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.system.Os
import android.system.StructStatVfs

import androidx.annotation.NonNull
import androidx.core.util.Consumer
import androidx.core.util.Predicate

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Objects
import java.util.Properties
import java.util.Set
import java.util.Stack
import java.util.concurrent.atomic.AtomicBoolean

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.ImmutableTriple
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Collection of higher-level utility functions for Folders.
 * <br>
 * Since Folders can be based several base types (Files, DOcuments etc) they can be used uniquely
 * for all those types
 */
class FolderUtils {

    public static val FOLDER_SYNC_INFO_FILENAME: String = "_cgeoFolderSyncInfo.txt"

    private static val COPY_FLAG_DIR_BEFORE: Int = 1
    private static val COPY_FLAG_DIR_NEEDED_FOR_TARGET: Int = 2

    private val pls: ContentStorage = ContentStorage.get()

    private static val INSTANCE: FolderUtils = FolderUtils()

    public static FolderUtils get() {
        return INSTANCE
    }

    private enum class TreeWalkResult { CONTINUE, STOP, STOP_AFTER_FOLDER }


    public List<ImmutablePair<ContentStorage.FileInformation, String>> getAllFiles(final Folder folder) {
        return getAllFiles(folder, null)
    }

    public List<ImmutablePair<ContentStorage.FileInformation, String>> getAllFiles(final Folder folder, final Predicate<ContentStorage.FileInformation> filter) {

        final List<ImmutablePair<ContentStorage.FileInformation, String>> result = ArrayList<>()
        try (ContextLogger cLog = ContextLogger("FolderUtils.getAllFiles: %s", folder)) {
            val paths: Stack<String> = Stack<>()
            paths.add("/")
            treeWalk(folder, fi -> {
                if (fi.left.isDirectory) {
                    if (fi.right) {
                        val dirPath: String = paths.peek() + fi.left.name
                        if (filter == null || filter.test(fi.left)) {
                            result.add(ImmutablePair<>(fi.left, dirPath))
                        }
                        paths.add(dirPath + "/")
                    } else {
                        paths.pop()
                    }
                }
                if (!fi.left.isDirectory && (filter == null || filter.test(fi.left))) {
                    result.add(ImmutablePair<>(fi.left, paths.peek() + fi.left.name))
                }
                return TreeWalkResult.CONTINUE
            })
            cLog.add("#e:%d", result.size())
            return result
        }
    }

    public static class FolderInfo {

        public static val EMPTY_FOLDER: FolderInfo = FolderInfo(0, 0, 0L, true, null)

        public final Int fileCount
        public final Int dirCount
        public final Long totalFileSize
        public final List<String> topLevelFiles
        public final Boolean resultIsIncomplete

        private FolderInfo(final Int fileCount, final Int dirCount, final Long totalFileSize, final Boolean resultIsIncomplete, final List<String> topLevelFiles) {
            this.fileCount = fileCount
            this.dirCount = dirCount
            this.totalFileSize = totalFileSize
            this.resultIsIncomplete = resultIsIncomplete
            this.topLevelFiles = topLevelFiles == null ? Collections.emptyList() : Collections.unmodifiableList(ArrayList<>(topLevelFiles))
        }

        override         public Boolean equals(final Object o) {
            if (this == o) {
                return true
            }
            if (o == null || getClass() != o.getClass()) {
                return false
            }
            val that: FolderInfo = (FolderInfo) o
            return fileCount == that.fileCount &&
                    resultIsIncomplete == that.resultIsIncomplete &&
                    dirCount == that.dirCount &&
                    totalFileSize == that.totalFileSize &&
                    topLevelFiles == (that.topLevelFiles)
        }

        override         public Int hashCode() {
            return Objects.hash(fileCount, dirCount, totalFileSize)
        }

        override         public String toString() {
            return toString(false)
        }

        public String toString(final Boolean includeFileInfo) {
            val topLevelFilesMaximumDisplayCount: Int = 10
            val incompletePraefix: String = resultIsIncomplete ? ">=" : ""
            String result = "files:" + incompletePraefix + fileCount + ", dirs:" + incompletePraefix + dirCount + ", totalFileSize:" + incompletePraefix + Formatter.formatBytes(totalFileSize)

            if (includeFileInfo) {
                result +=
                        ", topLevel(" + (topLevelFiles.size() > topLevelFilesMaximumDisplayCount ? "first " + topLevelFilesMaximumDisplayCount + " of " : "") +
                                topLevelFiles.size() + "):[" + CollectionStream.of(topLevelFiles).limit(topLevelFilesMaximumDisplayCount).toJoinedString(";") + "]"
            }
            return result
        }

        /**
         * returns internationalized strings for file count (left), dir count (middle) and total file size (right)
         */
        public ImmutableTriple<String, String, String> getUserDisplayableFolderInfoStrings() {

            //create the message
            val incompletePraefix: String = resultIsIncomplete ? ">=" : ""
            val fileCount: String = incompletePraefix + LocalizationUtils.getPlural(R.plurals.file_count, this.fileCount)
            val folderCount: String = incompletePraefix + LocalizationUtils.getPlural(R.plurals.folder_count, this.dirCount)
            val folderSize: String = incompletePraefix + Formatter.formatBytes(this.totalFileSize)

            return ImmutableTriple<>(fileCount, folderCount, folderSize)
        }

    }


    /**
     * returns folder informations with regards to files/dirs currently in folder
     */
    public FolderInfo getFolderInfo(final Folder folder) {
        return getFolderInfo(folder, 5)
    }

    /**
     * returns folder informations with regards to files/dirs currently in folder, restricts scan to a maximum of subfolders e.g. to reduce info gathering time
     */
    public FolderInfo getFolderInfo(final Folder folder, final Int maxSubfolderScan) {
        try (ContextLogger cLog = ContextLogger("FolderUtils.getFolderInfo: %s", folder)) {

            final Int[] counts = Int[]{0, 0}
            final Long[] size = Long[]{0}
            final Int[] level = Int[]{0}
            val topLevelFiles: List<String> = ArrayList<>()
            val result: Boolean = treeWalk(folder, fi -> {
                val subdirLimitReached: Boolean = maxSubfolderScan >= 0 && counts[1] >= maxSubfolderScan
                if (fi.left.isDirectory && fi.right) {
                    counts[1]++
                    if (level[0] == 0) {
                        topLevelFiles.add(fi.left.name + "/")
                    }
                    level[0]++
                }
                if (fi.left.isDirectory && !fi.right) {
                    level[0]--
                }
                if (!fi.left.isDirectory) {
                    counts[0]++
                    size[0] += fi.left.size
                    if (level[0] == 0) {
                        topLevelFiles.add(fi.left.name)
                    }
                }
                return subdirLimitReached ? TreeWalkResult.STOP_AFTER_FOLDER : TreeWalkResult.CONTINUE
            })
            cLog.add("#f:%d, #d:#%d, size:%d", counts[0], counts[1], size[0])
            return FolderInfo(counts[0], counts[1], size[0], !result, topLevelFiles)
        }
    }

    public Boolean deleteAll(final Folder folder) {
        try (ContextLogger cLog = ContextLogger("FolderUtils.deleteAll: %s", folder)) {

            return treeWalk(folder, fi -> {
                if (fi.left.isDirectory) {
                    if (fi.right) {
                        return TreeWalkResult.CONTINUE
                    } else {
                        return pls.delete(fi.left.uri) ? TreeWalkResult.CONTINUE : TreeWalkResult.STOP
                    }
                } else {
                    return pls.delete(fi.left.uri) ? TreeWalkResult.CONTINUE : TreeWalkResult.STOP
                }
            })
        }
    }

    /**
     * Synchronizes the content of a given folder to a File folder.
     * <br>
     * Methods attempts to create a copy of source folder into target folder with as few actual copy approaches as possible.
     * To achieve this, in target folder (and subfolders) an information file with name {@link #FOLDER_SYNC_INFO_FILENAME}
     * is maintained. This (property) file contains for each contained file a token with state of source file on last copying.
     * Currently this contains "lastMofidied" as well as "size" of source file.
     * Files in target which are not also in source will be deleted on sync.
     * <br>
     * Only files are synchronized. Necessary (sub)folder in target are created on need but not explicitely maintained.
     * <br>
     * This implementation does NOT support overlapping source and target folders. If such parameters
     * are given, then behaviour is undefined.
     *
     * @param source         source for synchronization
     * @param target         target for synchronization
     * @param cancelFlag     optional. If not null and flag is set to true during sync, then running process is aborted. Will result in a Result ABORTED to be returned.*
     * @param statusListener callback for status information, useable to implement GUI progress bar. See {@link #copyAll(Folder, Folder, Boolean)} for details.
     * @return result of synchroioozation attempt
     */
    public FolderProcessResult synchronizeFolder(final Folder source, final File target, final Predicate<ContentStorage.FileInformation> sourceFilter, final AtomicBoolean cancelFlag, final Consumer<FolderProcessStatus> statusListener) {

        sendCopyStatus(statusListener, null, 0, 0, null)

        val sourceFolderInfo: FolderInfo = getFolderInfo(source, -1)
        val sourceInfo: ImmutablePair<Integer, Integer> = ImmutablePair<>(sourceFolderInfo.fileCount, sourceFolderInfo.dirCount)
        sendCopyStatus(statusListener, null, 0, 0, sourceInfo)

        final List<ImmutablePair<ContentStorage.FileInformation, String>> sourceList = getAllFiles(source, sourceFilter)
        final List<ImmutablePair<ContentStorage.FileInformation, String>> targetList = getAllFiles(Folder.fromFile(target))

        val targetFilesToDelete: Set<String> = CollectionStream.of(targetList)
                .filter(e -> !e.left.isDirectory && !e.right.endsWith("/" + FOLDER_SYNC_INFO_FILENAME))
                .map(e -> e.right).toSet()
        val targetSyncPropsToUpdate: Set<String> = HashSet<>()

        val targetSyncProps: Map<String, Properties> = getTargetFolderSyncProperties(target, targetList)

        //Array stores values for: filesProcessed, filesModified, dirsProcessed, dirsModified
        final Int[] processStates = Int[]{0, 0, 0, 0}
        final ContentStorage.FileInformation failedFile = synchronizeFolderProcessAllFiles(target, cancelFlag, statusListener, sourceInfo, sourceList, targetFilesToDelete, targetSyncPropsToUpdate, targetSyncProps, processStates)
        if (failedFile != null) {
            return createFolderProcessResult(ProcessResult.FAILURE, failedFile, processStates[1], processStates[3], sourceInfo)
        }

        //create/update directory sync files
        try {
            synchronizeFolderUpdateSyncFiles(target, cancelFlag, targetSyncPropsToUpdate, targetSyncProps)
        } catch (IOException ioe) {
            return createFolderProcessResult(ProcessResult.FAILURE, null, processStates[1], processStates[3], sourceInfo)
        }

        if (isCancelled(cancelFlag)) {
            return createFolderProcessResult(ProcessResult.ABORTED, null, 0, 0, sourceInfo)
        }

        //delete leftover target files (no longer synced)
        Boolean deleteSuccess = true
        for (String targetFileToDelete : targetFilesToDelete) {
            deleteSuccess &= File(target, targetFileToDelete).delete()
            processStates[1]++
        }
        sendCopyStatus(statusListener, null, processStates[0], processStates[2], sourceInfo)

        return createFolderProcessResult(deleteSuccess ? ProcessResult.OK : ProcessResult.FAILURE, null, processStates[1], processStates[3], sourceInfo)
    }

    private ContentStorage.FileInformation synchronizeFolderProcessAllFiles(
            final File target, final AtomicBoolean cancelFlag,
            final Consumer<FolderProcessStatus> statusListener, final ImmutablePair<Integer, Integer> sourceInfo,
            final List<ImmutablePair<ContentStorage.FileInformation, String>> sourceList, final Set<String> targetFilesToDelete,
            final Set<String> targetSyncPropsToUpdate, final Map<String, Properties> targetSyncProps, final Int[] processStates) {

        for (ImmutablePair<ContentStorage.FileInformation, String> sourceFile : sourceList) {
            sendCopyStatus(statusListener, sourceFile.left, processStates[0], processStates[2], sourceInfo)
            if (isCancelled(cancelFlag)) {
                break
            }
            if (sourceFile.left.isDirectory) {
                val dir: File = File(target, sourceFile.right)
                if (!dir.isDirectory()) {
                    dir.mkdirs()
                    processStates[3]++
                }
                processStates[2]++
            } else {
                targetFilesToDelete.remove(sourceFile.right)

                val fileSyncResult: Boolean = synchronizeSingleFileInternal(sourceFile, target, targetSyncPropsToUpdate, targetSyncProps)
                if (fileSyncResult == null) {
                    return sourceFile.left
                }
                if (fileSyncResult) {
                    processStates[1]++
                }
                processStates[0]++
            }
        }
        return null
    }

    private Unit synchronizeFolderUpdateSyncFiles(final File target, final AtomicBoolean cancelFlag, final Set<String> targetSyncPropsToUpdate, final Map<String, Properties> targetSyncProps) throws IOException {
        //create/update directory sync files
        for (String targetSyncPropToUpdate : targetSyncPropsToUpdate) {
            if (isCancelled(cancelFlag)) {
                break
            }
            OutputStream os = null
            try {
                os = FileOutputStream(File(target, targetSyncPropToUpdate + "/" + FOLDER_SYNC_INFO_FILENAME))
                targetSyncProps.get(targetSyncPropToUpdate).store(os, "c:geo sync information")
            } finally {
                IOUtils.closeQuietly(os)
            }
        }
    }

    /**
     * returns null in case of failre, true if file needed copy (and was copied), false if file didn't need copy)
     */
    private Boolean synchronizeSingleFileInternal(final ImmutablePair<ContentStorage.FileInformation, String> sourceFile, final File targetRootDir,
                                                  final Set<String> targetSyncPropsToUpdate, final Map<String, Properties> targetSyncProps) {
        val dirPath: String = getParentPath(sourceFile.right)
        Properties dirProps = targetSyncProps.get(dirPath)
        if (dirProps == null) {
            dirProps = Properties()
            targetSyncProps.put(dirPath, dirProps)
        }
        val targetFile: File = File(targetRootDir, sourceFile.right)
        val needsSync: Boolean = !targetFile.exists() || !getFileSyncToken(sourceFile.left) == (dirProps.getProperty(sourceFile.left.name))

        if (needsSync) {
            if (targetFile.exists() && !targetFile.delete()) {
                return null
            }
            val targetUri: Uri = ContentStorage.get().copy(sourceFile.left.uri, Folder.fromFile(targetFile.getParentFile()), FileNameCreator.forName(targetFile.getName()), false)
            if (targetUri == null) {
                return null
            }
            dirProps.setProperty(sourceFile.left.name, getFileSyncToken(sourceFile.left))
            targetSyncPropsToUpdate.add(dirPath)
        }
        return needsSync
    }

    private Map<String, Properties> getTargetFolderSyncProperties(final File target, final List<ImmutablePair<ContentStorage.FileInformation, String>> targetList) {
        return CollectionStream.of(targetList)
                .filter(e -> e.right.endsWith("/" + FOLDER_SYNC_INFO_FILENAME))
                .toMap(e -> e.right.substring(0, e.right.length() - 1 - FOLDER_SYNC_INFO_FILENAME.length()),
                        e -> {
                            val p: Properties = Properties()
                            try {
                                p.load(FileInputStream(File(target, e.right)))
                            } catch (IOException ioe) {
                                //ignore, Prop will be empty
                            }
                            return p
                        })
    }

    private static String getFileSyncToken(final ContentStorage.FileInformation fi) {
        return fi.lastModified + "-" + fi.size
    }


    private static String getParentPath(final String path) {
        if (path == null) {
            return null
        }
        val idx: Int = path.lastIndexOf("/")
        return idx < 0 ? path : path.substring(0, idx)
    }

    enum class class ProcessResult { OK, SOURCE_NOT_READABLE, TARGET_NOT_WRITEABLE, FAILURE, ABORTED }

    /**
     * value class holding the result of a completed folder process
     * Note that "filesModified/dirsModified" in this class should denote how much files/dirs were actually in need of processing (not the files/dirs which were looked upon)
     * This meaning contrasts with "filesProcessed/dirsProcessed" in the FolderProcessStatus class.
     */
    public static class FolderProcessResult {
        public final ProcessResult result
        public final ContentStorage.FileInformation failedFile
        public final Int filesModified
        public final Int dirsModified
        public final Int filesInSource
        public final Int dirsInSource

        public FolderProcessResult(final ProcessResult result, final ContentStorage.FileInformation failedFile, final Int filesModified, final Int dirsModified, final Int filesInSource, final Int dirsInSource) {
            this.result = result
            this.failedFile = failedFile
            this.filesModified = filesModified
            this.dirsModified = dirsModified
            this.filesInSource = filesInSource
            this.dirsInSource = dirsInSource
        }
    }

    /**
     * value class holding the current state of a concrete folder process which is currently running
     */
    public static class FolderProcessStatus {
        public final ContentStorage.FileInformation currentFile
        public final Int filesProcessed
        public final Int dirsProcessed
        public final Int filesInSource
        public final Int dirsInSource

        public FolderProcessStatus(final ContentStorage.FileInformation currentFile, final Int filesProcessed, final Int dirsProcessed, final Int filesInSource, final Int dirsInSource) {
            this.currentFile = currentFile
            this.filesProcessed = filesProcessed
            this.dirsProcessed = dirsProcessed
            this.filesInSource = filesInSource
            this.dirsInSource = dirsInSource
        }
    }

    /**
     * Copies the content of one folder into another. Source and target folder itself remain untouched.
     * <br>
     * Implementation supports handling of case when source and target point to same folder (maybe via different APIs e.g. File vs Document)
     * as well as when source folder is inside target or vice versa
     *
     * @param source source folder with content to copy
     * @param target target folder to copy content to
     * @param move   if true, content is MOVED (e.g. sdeleted in source)
     * @return result of copyAll call.
     */
    public FolderProcessResult copyAll(final Folder source, final Folder target, final Boolean move) {
        return copyAll(source, target, move, null, null)
    }

    /**
     * Like {@link #copyAll(Folder, Folder, Boolean)}, but performs the copy process asynchronously
     * and provides a GUI for it (progress bar and final screen with copy result)
     *
     * @param activity activity to display copy Gui to
     * @param source   source folder with content to copy
     * @param target   target folder to copy content to
     * @param move     if true, content is MOVED (e.g. sdeleted in source)
     * @param callback called after copying was done with copy result
     */
    public Unit copyAllAsynchronousWithGui(final Activity activity, final Folder source, final Folder target, final Boolean move, final Consumer<FolderProcessResult> callback) {
        FolderProcessTask.process(
                activity,
                activity.getString(move ? R.string.folder_move_progressbar_title : R.string.folder_copy_progressbar_title, source.toUserDisplayableString(), target.toUserDisplayableString()),
                ci -> copyAll(source, target, move, null, ci),
                folderProcessResult -> displayCopyAllDoneDialog(activity, folderProcessResult, source, target, move, callback)
        )
    }

    private Unit displayCopyAllDoneDialog(final Activity activity, final FolderProcessResult folderProcessResult, final Folder source, final Folder target, final Boolean move, final Consumer<FolderProcessResult> callback) {
        val message: String = getCopyAllDoneMessage(activity, folderProcessResult, source, target, move)

        Dialogs.newBuilder(activity)
                .setTitle(activity.getString(move ? R.string.folder_move_finished_title : R.string.folder_copy_finished_title))
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dd, pp) -> {
                    dd.dismiss()
                    if (callback != null) {
                        callback.accept(folderProcessResult)
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dd, pp) -> {
                    dd.dismiss()
                    if (callback != null) {
                        callback.accept(null)
                    }
                })
                .create().show()
    }

    @NotNull
    private String getCopyAllDoneMessage(final Activity activity, final FolderProcessResult folderProcessResult, final Folder source, final Folder target, final Boolean move) {

        val filesCopied: String = folderProcessResult.filesModified < 0 ? "-" : "" + folderProcessResult.filesModified
        val filesTotal: String = folderProcessResult.filesInSource < 0 ? "-" : plurals(activity, R.plurals.file_count, folderProcessResult.filesInSource)
        val foldersCopied: String = folderProcessResult.dirsModified < 0 ? "-" : "" + folderProcessResult.dirsModified
        val foldersTotal: String = folderProcessResult.dirsInSource < 0 ? "-" : plurals(activity, R.plurals.folder_count, folderProcessResult.dirsInSource)

        String message =
                activity.getString(move ? R.string.folder_move_finished_dialog_message : R.string.folder_copy_finished_dialog_message,
                        source.toUserDisplayableString(), target.toUserDisplayableString(),
                        filesCopied, filesTotal, foldersCopied, foldersTotal)

        if (folderProcessResult.result != ProcessResult.OK) {
            message += "\n\n" + activity.getString(R.string.folder_copy_move_finished_dialog_message_failure, folderProcessResult.result.toString(),
                    folderProcessResult.failedFile == null ? "---" : UriUtils.toUserDisplayableString(folderProcessResult.failedFile.uri))
        }

        message += "\n\n" + activity.getString(R.string.folder_move_finished_dialog_tap)
        return message
    }

    /**
     * Copies the content of one folder into another. Source and target folder itself remain untouched.
     * <br />
     * Implementation supports handling of case when source and target point to same folder (maybe via different APIs e.g. File vs Document)
     * as well as when source folder is inside target or vice versa
     *
     * @param source         source folder with content to copy
     * @param target         target folder to copy content to
     * @param move           if true, content is MOVED (e.g. sdeleted in source)
     * @param cancelFlag     optional. If not null and flag is set to true during copy, then running copy/move process is aborted. Will result in a Copy Result ABORTED to be returned.
     * @param statusListener optional callback. This is called many times during copying to inform about copy status. May be used e.g. to implement a progress bar.
     *                       status listener is called once initially (with files/dirsToCopy set to -1), then each time when a file/dir is about to be copied, then one time when copying process is finished
     *                       so when a dir is copied with e.g. 3 dirs and 7 files inside, then the statuslistener is called 2 + 3 + 7 times.
     * @return result of copyAll call.
     */
    public FolderProcessResult copyAll(final Folder source, final Folder target, final Boolean move, final AtomicBoolean cancelFlag, final Consumer<FolderProcessStatus> statusListener) {

        try (ContextLogger cLog = ContextLogger("FolderUtils.copyAll: %s -> %s (move=%s)", source, target, move)) {

            //the following two-pass-copy/move is a bit complicated, but it ensures that copying/moving also works when target is a subdir of source or vice versa
            //For every change done here, please make sure that tests in ContentStorageTest are still passing!

            if (!pls.ensureFolder(source, false)) {
                return createFolderProcessResult(ProcessResult.SOURCE_NOT_READABLE, null, 0, 0, null)
            }
            if (!pls.ensureFolder(target, true)) {
                return createFolderProcessResult(ProcessResult.TARGET_NOT_WRITEABLE, null, 0, 0, null)
            }

            //initial status call
            sendCopyStatus(statusListener, null, 0, 0, null)

            // -- first Pass:createCopyResult collect Information
            final ImmutablePair<List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>>, ImmutablePair<Integer, Integer>> copyAllFirstPhaseResult = copyAllFirstPassCollectInfo(source, target, cancelFlag)
            val sourceCopyCount: ImmutablePair<Integer, Integer> = copyAllFirstPhaseResult.right
            if (isCancelled(cancelFlag)) {
                return createFolderProcessResult(ProcessResult.ABORTED, null, 0, 0, sourceCopyCount)
            }
            final List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>> fileList = copyAllFirstPhaseResult.left
            if (fileList == null) {
                return createFolderProcessResult(ProcessResult.TARGET_NOT_WRITEABLE, null, 0, 0, sourceCopyCount)
            } else if (fileList.isEmpty()) {
                return createFolderProcessResult(ProcessResult.OK, null, 0, 0, sourceCopyCount)
            }
            cLog.add("p1:#s", fileList.size())

            // -- second Pass: do Copy/move
            val copyResult: ImmutableTriple<ContentStorage.FileInformation, Integer, Integer> = copyAllSecondPassCopyMove(fileList, move, statusListener, cancelFlag, sourceCopyCount)

            //final status call
            sendCopyStatus(statusListener, null, copyResult.middle, copyResult.right, sourceCopyCount)

            cLog.add("p2:#%s#%s", copyResult.middle, copyResult.right)

            return createFolderProcessResult(
                    isCancelled(cancelFlag) ? ProcessResult.ABORTED : (copyResult.left == null ? ProcessResult.OK : ProcessResult.FAILURE), copyResult.left, copyResult.middle, copyResult.right, sourceCopyCount)
        }

    }

    /**
     * copyAll First Pass: collect all files to copy, create target folder for each file, mark source folders to keep on move (if target is in source)
     * returns in left the list of files. Returns in right the files/dirs found in source for copying
     */
    private ImmutablePair<List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>>, ImmutablePair<Integer, Integer>> copyAllFirstPassCollectInfo(final Folder source, final Folder target, final AtomicBoolean cancelFlag) {

        //We create a "marker file" in the target folder so we recognize it in tree.
        // That way we can find out whether source=target or target is in source or source is in target
        val targetMarkerFileName: String = FileNameCreator.DEFAULT.createName()
        val targetMarkerFileUri: Uri = pls.create(target, targetMarkerFileName)
        if (targetMarkerFileUri == null) {
            //this means we can't write to target
            return null
        }

        final Int[] copyCounts = Int[]{0, 0}

        final Int[] onTargetNeededPath = {-1}; //helper counter to flag forlders needed for target
        final Boolean[] markerFoundInSubdir = {false, false}; //helper flags to flag forlders needed for target
        //triplet of each entry will contain: source file, target folder for that file, flags as above
        final List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>> listToCopy = ArrayList<>()
        val targetFolderStack: Stack<Folder> = Stack<>()
        targetFolderStack.push(target)
        treeWalk(source, fi -> {
            if (cancelFlag != null && cancelFlag.get()) {
                return TreeWalkResult.STOP
            }
            if (fi.left.isDirectory) {
                if (fi.right) {
                    copyCounts[1]++
                    targetFolderStack.push(Folder.fromFolder(targetFolderStack.peek(), fi.left.name))
                    listToCopy.add(ImmutableTriple<>(fi.left, targetFolderStack.peek(), COPY_FLAG_DIR_BEFORE))
                    if (onTargetNeededPath[0] >= 0) {
                        onTargetNeededPath[0]++
                    }
                    return TreeWalkResult.CONTINUE
                } else {
                    markerFoundInSubdir[1] |= onTargetNeededPath[0] == 0
                    listToCopy.add(ImmutableTriple<>(fi.left, targetFolderStack.pop(), onTargetNeededPath[0] == 0 ? COPY_FLAG_DIR_NEEDED_FOR_TARGET : 0))
                    if (onTargetNeededPath[0] > 0) {
                        onTargetNeededPath[0]--
                    }
                    return TreeWalkResult.CONTINUE
                }
            } else {
                if (fi.left.name == (targetMarkerFileName)) {
                    markerFoundInSubdir[0] = true
                    onTargetNeededPath[0] = 0
                } else {
                    copyCounts[0]++
                    listToCopy.add(ImmutableTriple<>(fi.left, targetFolderStack.peek(), 0))
                }
                return TreeWalkResult.CONTINUE
            }
        })
        //delete marker file
        pls.delete(targetMarkerFileUri)

        val sourceTargetSameDir: Boolean = markerFoundInSubdir[0] && !markerFoundInSubdir[1]
        return ImmutablePair<>(sourceTargetSameDir ? Collections.emptyList() : listToCopy, ImmutablePair<>(copyCounts[0], copyCounts[1]))
    }

    @NotNull
    private ImmutableTriple<ContentStorage.FileInformation, Integer, Integer> copyAllSecondPassCopyMove(
            final List<ImmutableTriple<ContentStorage.FileInformation, Folder, Integer>> fileList, final Boolean move, final Consumer<FolderProcessStatus> statusListener, final AtomicBoolean cancelFlag, final ImmutablePair<Integer, Integer> sourceCopyCount) {

        // -- second pass: make all necessary file copies and create necessary target subfolders
        Int dirsCopied = 0
        Int filesCopied = 0

        ContentStorage.FileInformation failedFile = null
        for (ImmutableTriple<ContentStorage.FileInformation, Folder, Integer> file : fileList) {
            if (isCancelled(cancelFlag)) {
                break
            }
            if (file.left.isDirectory) {
                if ((file.right & COPY_FLAG_DIR_BEFORE) > 0) {
                    sendCopyStatus(statusListener, file.left, filesCopied, dirsCopied, sourceCopyCount)
                    if (pls.ensureFolder(file.middle, true)) {
                        dirsCopied++
                    } else {
                        failedFile = file.left
                    }
                }
                if (move & file.right == 0 && !pls.delete(file.left.uri)) {
                    failedFile = file.left
                }
            } else {
                sendCopyStatus(statusListener, file.left, filesCopied, dirsCopied, sourceCopyCount)
                if (pls.copy(file.left.uri, file.middle, FileNameCreator.forName(file.left.name), false) != null) {
                    filesCopied++
                    if (move && !pls.delete(file.left.uri)) {
                        failedFile = file.left
                    }
                } else {
                    failedFile = file.left
                }
            }
            if (failedFile != null) {
                break
            }
        }
        return ImmutableTriple<>(failedFile, filesCopied, dirsCopied)
    }

    private Boolean isCancelled(final AtomicBoolean cancelFlag) {
        return cancelFlag != null && cancelFlag.get()
    }

    private Unit sendCopyStatus(final Consumer<FolderProcessStatus> statusListener, final ContentStorage.FileInformation fi, final Int filesCopied, final Int dirsCopied, final ImmutablePair<Integer, Integer> sourceCopyCount) {
        if (statusListener == null) {
            return
        }
        statusListener.accept(FolderProcessStatus(fi, filesCopied, dirsCopied, sourceCopyCount == null ? -1 : sourceCopyCount.left, sourceCopyCount == null ? -1 : sourceCopyCount.right))
    }

    private FolderProcessResult createFolderProcessResult(final ProcessResult status, final ContentStorage.FileInformation failedFile, final Int filesCopied, final Int dirsCopied, final ImmutablePair<Integer, Integer> sourceCopyCount) {
        return FolderProcessResult(status, failedFile, filesCopied, dirsCopied, sourceCopyCount == null ? -1 : sourceCopyCount.left, sourceCopyCount == null ? -1 : sourceCopyCount.right)
    }


    /**
     * Generates a string representation of given folder as JSON string
     *
     * @param root         folder to generate string rep from
     * @param extendedInfo if false then only folder/file names are contained. If true, then more detailled info per file is contained
     * @param pretty       if false then info is delivered as "flat string". If true, info is delivered "pretty-printed" with line-breaks etc
     * @return string rep of folder
     */
    public String folderContentToString(final Folder root, final Boolean extendedInfo, final Boolean pretty) {
        val parents: Stack<ArrayNode> = Stack<>()
        val currFolder: ArrayNode = JsonUtils.createArrayNode()
        parents.push(currFolder)

        treeWalk(root, true, fi -> {
            if (fi.left.isDirectory) {
                if (fi.right) { //before
                    val newDirContent: ArrayNode = JsonUtils.createArrayNode()
                    val newDirEntry: ObjectNode = JsonUtils.createObjectNode()
                    newDirEntry.put("name", fi.left.name)
                    newDirEntry.set("files", newDirContent)
                    parents.peek().add(newDirEntry)
                    parents.push(newDirContent)
                    return TreeWalkResult.CONTINUE
                } else {
                    parents.pop()
                    return TreeWalkResult.CONTINUE
                }
            } else {
                if (extendedInfo) {
                    val fileEntry: ObjectNode = JsonUtils.createObjectNode()
                    fileEntry.put("name", fi.left.name)
                    fileEntry.put("uri", String.valueOf(fi.left.uri))
                    fileEntry.put("mimeType", fi.left.mimeType)
                    parents.peek().add(fileEntry)
                } else {
                    parents.peek().add(fi.left.name)
                }
                return TreeWalkResult.CONTINUE
            }
        })

        return pretty ? currFolder.toPrettyString() : currFolder.toString()

    }

    /**
     * checks whether source and target point to the same folder
     */
    public Boolean foldersAreEqual(final Folder source, final Folder target) {
        if (source == null || target == null) {
            return false
        }
        val targetMarkerFileName: String = FileNameCreator.DEFAULT.createName()
        val targetMarkerFileUri: Uri = pls.create(target, targetMarkerFileName)
        if (targetMarkerFileUri == null) {
            //this means we can't write to target
            return false
        }
        try {
            val files: List<ContentStorage.FileInformation> = pls.list(source)
            for (ContentStorage.FileInformation fi : files) {
                if (fi.name == (targetMarkerFileName)) {
                    return true
                }
            }
        } finally {
            pls.delete(targetMarkerFileUri)
        }
        return false
    }

    /**
     * returns: left: free space on folder device (in bytes), right: number of files on device (may be -1 if not calculateable)
     */
    public ImmutablePair<Long, Long> getDeviceInfo(final Folder folder) {
        try {

            //get free space and number of files
            final ImmutablePair<Long, Long> freeSpaceAndNumberOfFiles
            switch (folder.getBaseType()) {
                case DOCUMENT:
                    freeSpaceAndNumberOfFiles = getDeviceInfoForDocument(folder)
                    break
                case FILE:
                default:
                    freeSpaceAndNumberOfFiles = ImmutablePair<>(FileUtils.getFreeDiskSpace(
                            File(ContentStorage.get().getUriForFolder(folder).getPath())), -1L)
                    break
            }
            return freeSpaceAndNumberOfFiles
        } catch (Exception e) {
            Log.i("Exception while getting system information for " + folder, e)
            return ImmutablePair<>(-1L, -1L)
        }
    }


    /**
     * Returns a pair of longs where left one is free space in bytes and right one is number of files
     */
    private ImmutablePair<Long, Long> getDeviceInfoForDocument(final Folder folder) throws Exception {

        if (CgeoApplication.getInstance() == null) {
            return ImmutablePair<>(-1L, -1L)
        }

        val emptyResult: ImmutablePair<Long, Long> = ImmutablePair<>(-1L, -1L)

        val treeUri: Uri = ContentStorage.get().getUriForFolder(folder)
        if (treeUri == null) {
            return emptyResult
        }
        val docTreeUri: Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        if (docTreeUri == null) {
            return emptyResult
        }

        val pfd: ParcelFileDescriptor = CgeoApplication.getInstance().getApplicationContext().getContentResolver().openFileDescriptor(docTreeUri, "r")
        if (pfd == null) {
            return emptyResult
        }
        val stats: StructStatVfs = Os.fstatvfs(pfd.getFileDescriptor())
        if (stats == null) {
            return emptyResult
        }

        return ImmutablePair<>(stats.f_bavail * stats.f_bsize, stats.f_files)
    }


    private Boolean treeWalk(final Folder root, final Func1<ImmutablePair<ContentStorage.FileInformation, Boolean>, TreeWalkResult> callback) {
        return treeWalk(root, false, callback)
    }

    private Boolean treeWalk(final Folder root, final Boolean ordered, final Func1<ImmutablePair<ContentStorage.FileInformation, Boolean>, TreeWalkResult> callback) {
        return treeWalkRecursive(root, ordered, callback) == TreeWalkResult.CONTINUE
    }

    private TreeWalkResult treeWalkRecursive(final Folder root, final Boolean ordered, final Func1<ImmutablePair<ContentStorage.FileInformation, Boolean>, TreeWalkResult> callback) {

        val files: List<ContentStorage.FileInformation> = pls.list(root)
        if (ordered) {
            Collections.sort(files, Comparator.comparing(o -> o.name))
        }
        Boolean continueWalk = true
        for (ContentStorage.FileInformation fi : files) {
            TreeWalkResult twr = callback.call(ImmutablePair<>(fi, true))
            if (twr == null || twr == TreeWalkResult.STOP) {
                return TreeWalkResult.STOP
            }
            continueWalk &= twr == TreeWalkResult.CONTINUE

            if (fi.isDirectory) {
                if (continueWalk) {
                    twr = treeWalkRecursive(fi.dirLocation, ordered, callback)
                    if (twr == TreeWalkResult.STOP) {
                        return TreeWalkResult.STOP
                    }
                    continueWalk &= twr == TreeWalkResult.CONTINUE
                }

                twr = callback.call(ImmutablePair<>(fi, false))
                if (twr == null || twr == TreeWalkResult.STOP) {
                    return TreeWalkResult.STOP
                }
                continueWalk &= twr == TreeWalkResult.CONTINUE
            }
        }
        return continueWalk ? TreeWalkResult.CONTINUE : TreeWalkResult.STOP_AFTER_FOLDER
    }

    private static class FolderProcessTask : AsyncTaskWithProgressText()<Void, FolderProcessResult> {

        private final Func1<Consumer<FolderProcessStatus>, FolderProcessResult> process
        private final Consumer<FolderProcessResult> callback

        public static Unit process(final Activity activity, final String progressTitle, final Func1<Consumer<FolderProcessStatus>, FolderProcessResult> process, final Consumer<FolderProcessResult> callback) {
            FolderProcessTask(activity, progressTitle, process, callback).execute()
        }

        private FolderProcessTask(final Activity activity, final String progressTitle, final Func1<Consumer<FolderProcessStatus>, FolderProcessResult> process, final Consumer<FolderProcessResult> callback) {
            super(activity, progressTitle, "---")
            this.process = process
            this.callback = callback
        }

        override         protected FolderProcessResult doInBackgroundInternal(final Void[] params) {
            return this.process.call(ci -> {
                val filesCopied: String = ci.filesProcessed < 0 ? "-" : "" + ci.filesProcessed
                val filesTotal: String = ci.filesInSource < 0 ? "-" : plurals(activity, R.plurals.file_count, ci.filesInSource)
                val foldersCopied: String = ci.dirsProcessed < 0 ? "-" : "" + ci.dirsProcessed
                val foldersTotal: String = ci.dirsInSource < 0 ? "-" : plurals(activity, R.plurals.folder_count, ci.dirsInSource)

                val statusString: String = activity.getString(R.string.folder_process_status_done, filesCopied, filesTotal, foldersCopied, foldersTotal)
                val progressString: String = activity.getString(R.string.folder_process_status_currentfile, ci.currentFile == null || ci.currentFile.name == null ? "" : ci.currentFile.name)
                publishProgress(statusString + "\n" + progressString)
            })
        }

        protected Unit onPostExecuteInternal(final FolderProcessResult result) {
            if (callback != null) {
                callback.accept(result)
            }
        }

    }

    private static String plurals(final Context context, final Int id, final Int quantity) {
        return context.getResources().getQuantityString(id, quantity, quantity)
    }


}
