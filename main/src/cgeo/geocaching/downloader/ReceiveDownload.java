package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

class ReceiveDownload {
    private Uri uri = null;
    private String filename = null;

    private String sourceURL = "";
    private long sourceDate = 0;
    private int offlineMapTypeId = Download.DownloadType.DEFAULT;
    private AbstractDownloader downloader;


    //copy task
    private long bytesCopied = 0;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);


    protected enum CopyStates {
        SUCCESS, CANCELLED, IO_EXCEPTION, FILENOTFOUND_EXCEPTION, UNKNOWN_STATE
    }

    /**
     * receives a file downloaded with system's DownloadManager, copying it to the appropriate c:geo folder
     * called internally by {@link ReceiveDownloadService} or {@link ReceiveDownloadWorker}
     */
    Worker.Result receiveDownload(final Context context, final NotificationManagerCompat notificationManager, final NotificationCompat.Builder notification, final Runnable updateForegroundNotification,
                         final Uri uri, final String preset, final String sourceURL, final long sourceDate, final int offlineMapTypeId) {
        this.uri = uri;
        this.filename = preset;
        this.sourceURL = sourceURL;
        this.sourceDate = sourceDate;
        this.offlineMapTypeId = offlineMapTypeId;
        this.downloader = Download.DownloadType.getInstance(offlineMapTypeId);
        if (downloader == null) {
            return Worker.Result.failure();
        }

        final boolean mapDirIsReady = ContentStorage.get().ensureFolder(downloader.targetFolder);

        if (mapDirIsReady) {
            // test if ZIP file received
            try (BufferedInputStream bis = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
                 ZipInputStream zis = new ZipInputStream(bis)) {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    String filename = ze.getName();
                    final int posExt = filename.lastIndexOf('.');
                    if (posExt != -1 && (StringUtils.equalsIgnoreCase(FileUtils.MAP_FILE_EXTENSION, filename.substring(posExt)))) {
                        filename = downloader.toVisibleFilename(filename);
                        // found map file within zip
                        if (guessFilename(filename)) {
                            return handleMapFile(context, notificationManager, notification, updateForegroundNotification, true, ze.getName());
                        }
                    }
                }
            } catch (IOException | SecurityException e) {
                // ignore ZIP errors
            }
            // if no ZIP file: continue with copying the file
            if (guessFilename(preset)) {
                return handleMapFile(context, notificationManager, notification, updateForegroundNotification, false, null);
            }
        } else {
            notificationManager.notify(Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                    context, NotificationChannels.DOWNLOADER_RESULT_NOTIFICATION, R.string.receivedownload_intenttitle, String.format(context.getString(R.string.downloadmap_target_not_writable), downloader.targetFolder)
            ).build());
        }
        return Worker.Result.failure();
    }

    // try to guess a filename, otherwise chose randomized filename
    private boolean guessFilename(final String preset) {
        filename = StringUtils.isNotBlank(preset) ? preset : uri.getPath();    // uri.getLastPathSegment doesn't help here, if path is encoded
        if (filename != null) {
            filename = FileUtils.getFilenameFromPath(filename);
            if (StringUtils.isNotBlank(downloader.forceExtension)) {
                final int posExt = filename.lastIndexOf('.');
                if (posExt == -1 || !(StringUtils.equalsIgnoreCase(downloader.forceExtension, filename.substring(posExt)))) {
                    filename += downloader.forceExtension;
                }
            }
        }
        if (filename == null) {
            filename = FileNameCreator.OFFLINE_MAPS.createName();
        }
        return true;
    }

    private Worker.Result handleMapFile(final Context context, final NotificationManagerCompat notificationManager, final NotificationCompat.Builder notification, final Runnable updateForegroundNotification,
                               final boolean isZipFile, final String nameWithinZip) {
        cleanupFolder();

        // now start copy task
        final CopyStates status = copyInternal(context, notification, updateForegroundNotification, isZipFile, nameWithinZip);

        final String resultMsg;
        Worker.Result resultId = Worker.Result.failure();
        String fileinfo = filename;
        if (fileinfo != null) {
            fileinfo = fileinfo.substring(0, fileinfo.length() - downloader.forceExtension.length());
        }
        switch (status) {
            case SUCCESS:
                resultMsg = String.format(context.getString(R.string.receivedownload_success), fileinfo);
                if (downloader.useCompanionFiles && StringUtils.isNotBlank(sourceURL)) {
                    CompanionFileUtils.writeInfo(sourceURL, filename, CompanionFileUtils.getDisplayName(fileinfo), sourceDate, offlineMapTypeId);
                }
                TileProviderFactory.buildTileProviderList(true);
                resultId = Worker.Result.success();
                break;
            case CANCELLED:
                resultMsg = context.getString(R.string.receivedownload_cancelled);
                break;
            case IO_EXCEPTION:
                resultMsg = String.format(context.getString(R.string.receivedownload_error_io_exception), downloader.targetFolder.toUserDisplayableValue());
                break;
            case FILENOTFOUND_EXCEPTION:
                resultMsg = context.getString(R.string.receivedownload_error_filenotfound_exception);
                break;
            default:
                resultMsg = context.getString(R.string.receivedownload_error);
                break;
        }
        notificationManager.notify(Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                context, NotificationChannels.DOWNLOADER_RESULT_NOTIFICATION, R.string.receivedownload_intenttitle, resultMsg
        ).build());
        return resultId;
    }

    /** check whether the target file or its companion file already exist, and delete both, if so */
    private void cleanupFolder() {
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(downloader.targetFolder.getFolder(), false, false);

        final Uri companionFile = downloader.useCompanionFiles ? CompanionFileUtils.companionFileExists(files, filename) : null;
        if (companionFile != null) {
            ContentStorage.get().delete(companionFile);
        }

        // check for files in different capitalizations
        final String filenameNormalized = normalized(filename);
        for (ContentStorage.FileInformation fi : files) {
            final String fiNormalized = normalized(fi.name);
            if (fiNormalized.equals(filenameNormalized)) {
                ContentStorage.get().delete(fi.uri);
                // also check companion file for this
                final Uri cf = downloader.useCompanionFiles ? CompanionFileUtils.companionFileExists(files, fi.name) : null;
                if (cf != null) {
                    ContentStorage.get().delete(cf);
                }
            }
        }
    }

    private String normalized(final String filename) {
        return StringUtils.replace(StringUtils.lowerCase(filename), "-", "_");
    }

    private CopyStates copyInternal(final Context context, final NotificationCompat.Builder notification, final Runnable updateForegroundNotification, final boolean isZipFile, final String nameWithinZip) {
        CopyStates status = CopyStates.UNKNOWN_STATE;

        Log.d("start receiving map file: " + filename);
        InputStream inputStream = null;
        final Uri outputUri = ContentStorage.get().create(downloader.targetFolder, filename);

        try {
            inputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
            if (isZipFile) {
                try (ZipInputStream zis = new ZipInputStream(inputStream)) {
                    ZipEntry ze;
                    boolean stillSearching = true;
                    while (stillSearching && (ze = zis.getNextEntry()) != null) {
                        if (ze.getName().equals(nameWithinZip)) {
                            status = doCopy(context, notification, updateForegroundNotification, zis, outputUri);
                            stillSearching = false; // don't continue here, as doCopy also closes the input stream, so further reads would lead to IOException
                        }
                    }
                } catch (IOException e) {
                    Log.e("IOException on receiving map file: " + e.getMessage());
                    status = CopyStates.IO_EXCEPTION;
                }
            } else {
                status = doCopy(context, notification, updateForegroundNotification, inputStream, outputUri);
            }
        } catch (SecurityException e) {
            Log.e("SecurityException on receiving map file: " + e.getMessage());
            return CopyStates.FILENOTFOUND_EXCEPTION;
        } catch (FileNotFoundException e) {
            return CopyStates.FILENOTFOUND_EXCEPTION;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        // clean up
        if (!cancelled.get()) {
            try {
                context.getContentResolver().delete(uri, null, null);
            } catch (IllegalArgumentException iae) {
                Log.w("Deleting Uri '" + uri + "' failed, will be ignored", iae);
            }
            // finalization AFTER deleting source file. This handles the very special case when target folder = Download folder
            downloader.onSuccessfulReceive(outputUri);
        } else {
            ContentStorage.get().delete(outputUri);
            status = CopyStates.CANCELLED;
        }

        return status;
    }

    private CopyStates doCopy(final Context context, final NotificationCompat.Builder notification, final Runnable updateForegroundNotification, final InputStream inputStream, final Uri outputUri) {
        OutputStream outputStream = null;
        long lastPublishTime = System.currentTimeMillis();
        try {
            outputStream = new BufferedOutputStream(ContentStorage.get().openForWrite(outputUri));
            final byte[] buffer = new byte[8192];
            int length = 0;
            while (!cancelled.get() && (length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                bytesCopied += length;
                if ((System.currentTimeMillis() - lastPublishTime) > 500) { // avoid message flooding
                    notification.setContentText(context.getString(R.string.receivedownload_amount_copied, Formatter.formatBytes(bytesCopied)));
                    updateForegroundNotification.run();
                    lastPublishTime = System.currentTimeMillis();
                }
            }
            return CopyStates.SUCCESS;
        } catch (IOException e) {
            Log.e("IOException on receiving map file: " + e.getMessage());
            return CopyStates.IO_EXCEPTION;
        } finally {
            IOUtils.closeQuietly(inputStream, outputStream);
        }
    }

}
