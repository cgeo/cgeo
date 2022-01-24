package cgeo.geocaching.downloader;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.service.AbstractForegroundIntentService;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

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


public class ReceiveDownloadService extends AbstractForegroundIntentService {
    static {
        logTag = "ReceiveDownloadService";
    }

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

    @Override
    public NotificationCompat.Builder createInitialNotification() {
        return Notifications.createNotification(this, NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION, R.string.notification_download_receiver_title)
                .setProgress(100, 0, true)
                .setOnlyAlertOnce(true);
    }

    @Override
    protected void onHandleIntent(final @Nullable Intent intent) {

        uri = intent.getData();
        final String preset = intent.getStringExtra(Intents.EXTRA_FILENAME);
        sourceURL = intent.getStringExtra(DownloaderUtils.RESULT_CHOSEN_URL);
        sourceDate = intent.getLongExtra(DownloaderUtils.RESULT_DATE, 0);
        offlineMapTypeId = intent.getIntExtra(DownloaderUtils.RESULT_TYPEID, Download.DownloadType.DEFAULT);
        downloader = Download.DownloadType.getInstance(offlineMapTypeId);


        final boolean mapDirIsReady = ContentStorage.get().ensureFolder(downloader.targetFolder);

        if (mapDirIsReady) {
            boolean foundMapInZip = false;
            // test if ZIP file received
            try (BufferedInputStream bis = new BufferedInputStream(getContentResolver().openInputStream(uri));
                 ZipInputStream zis = new ZipInputStream(bis)) {
                ZipEntry ze;
                while (!foundMapInZip && (ze = zis.getNextEntry()) != null) {
                    String filename = ze.getName();
                    final int posExt = filename.lastIndexOf('.');
                    if (posExt != -1 && (StringUtils.equalsIgnoreCase(FileUtils.MAP_FILE_EXTENSION, filename.substring(posExt)))) {
                        filename = downloader.toVisibleFilename(filename);
                        // found map file within zip
                        if (guessFilename(filename)) {
                            handleMapFile(true, ze.getName());
                            foundMapInZip = true;
                        }
                    }
                }
            } catch (IOException | SecurityException e) {
                // ignore ZIP errors
            }
            // if no ZIP file: continue with copying the file
            if (!foundMapInZip && guessFilename(preset)) {
                handleMapFile(false, null);
            }
        } else {
            notificationManager.notify(Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                    this, NotificationChannels.DOWNLOADER_RESULT_NOTIFICATION, R.string.receivedownload_intenttitle, String.format(getString(R.string.downloadmap_target_not_writable), downloader.targetFolder)
            ).build());
        }
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

    private void handleMapFile(final boolean isZipFile, final String nameWithinZip) {
        // check whether the target file or its companion file already exist, and delete both, if so
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

        // now start copy task
        final CopyStates status = copyInternal(isZipFile, nameWithinZip);

        final String result;
        String fileinfo = filename;
        if (fileinfo != null) {
            fileinfo = fileinfo.substring(0, fileinfo.length() - downloader.forceExtension.length());
        }
        switch (status) {
            case SUCCESS:
                result = String.format(getString(R.string.receivedownload_success), fileinfo);
                if (downloader.useCompanionFiles && StringUtils.isNotBlank(sourceURL)) {
                    CompanionFileUtils.writeInfo(sourceURL, filename, CompanionFileUtils.getDisplayName(fileinfo), sourceDate, offlineMapTypeId);
                }
                break;
            case CANCELLED:
                result = getString(R.string.receivedownload_cancelled);
                break;
            case IO_EXCEPTION:
                result = String.format(getString(R.string.receivedownload_error_io_exception), downloader.targetFolder.toUserDisplayableValue());
                break;
            case FILENOTFOUND_EXCEPTION:
                result = getString(R.string.receivedownload_error_filenotfound_exception);
                break;
            default:
                result = getString(R.string.receivedownload_error);
                break;
        }
        notificationManager.notify(Settings.getUniqueNotificationId(), Notifications.createTextContentNotification(
                this, NotificationChannels.DOWNLOADER_RESULT_NOTIFICATION, R.string.receivedownload_intenttitle, result
        ).build());
    }

    private String normalized(final String filename) {
        return StringUtils.replace(StringUtils.lowerCase(filename), "-", "_");
    }

    private CopyStates copyInternal(final boolean isZipFile, final String nameWithinZip) {
        CopyStates status = CopyStates.UNKNOWN_STATE;

        Log.d("start receiving map file: " + filename);
        InputStream inputStream = null;
        final Uri outputUri = ContentStorage.get().create(downloader.targetFolder, filename);

        try {
            inputStream = new BufferedInputStream(getContentResolver().openInputStream(uri));
            if (isZipFile) {
                try (ZipInputStream zis = new ZipInputStream(inputStream)) {
                    ZipEntry ze;
                    boolean stillSearching = true;
                    while (stillSearching && (ze = zis.getNextEntry()) != null) {
                        if (ze.getName().equals(nameWithinZip)) {
                            status = doCopy(zis, outputUri);
                            stillSearching = false; // don't continue here, as doCopy also closes the input stream, so further reads would lead to IOException
                        }
                    }
                } catch (IOException e) {
                    Log.e("IOException on receiving map file: " + e.getMessage());
                    status = CopyStates.IO_EXCEPTION;
                }
            } else {
                status = doCopy(inputStream, outputUri);
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
                getContentResolver().delete(uri, null, null);
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

    private CopyStates doCopy(final InputStream inputStream, final Uri outputUri) {
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
                    notification.setContentText(getString(R.string.receivedownload_amount_copied, Formatter.formatBytes(bytesCopied)));
                    notificationManager.notify(foregroundNotificationId, notification.build());
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
