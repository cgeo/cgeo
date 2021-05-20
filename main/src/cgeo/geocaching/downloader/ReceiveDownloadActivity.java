package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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

/**
 * Receives a map file via intent, moves it to the currently set map directory and sets it as current map source.
 * If no map directory is set currently, default map directory is used, created if needed, and saved as map directory in preferences.
 * If the map file already exists under that name in the map directory, you have the option to either overwrite it or save it under a randomly generated name.
 */
public class ReceiveDownloadActivity extends AbstractActivity {

    public static final String EXTRA_FILENAME = "filename";

    private Uri uri = null;
    private String filename = null;

    private String sourceURL = "";
    private long sourceDate = 0;
    private int offlineMapTypeId = Download.DownloadType.DEFAULT;
    private AbstractDownloader downloader;

    protected enum CopyStates {
        SUCCESS, CANCELLED, IO_EXCEPTION, FILENOTFOUND_EXCEPTION, UNKNOWN_STATE
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Intent intent = getIntent();
        uri = intent.getData();
        final String preset = intent.getStringExtra(EXTRA_FILENAME);
        sourceURL = intent.getStringExtra(DownloaderUtils.RESULT_CHOSEN_URL);
        sourceDate = intent.getLongExtra(DownloaderUtils.RESULT_DATE, 0);
        offlineMapTypeId = intent.getIntExtra(DownloaderUtils.RESULT_TYPEID, Download.DownloadType.DEFAULT);
        downloader = Download.DownloadType.getInstance(offlineMapTypeId);

        DownloaderUtils.checkTargetDirectory(this, downloader.targetFolder, false, (folder, isWritable) -> {
            if (isWritable) {
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
                                handleMapFile(this,  true, ze.getName());
                                foundMapInZip = true;
                            }
                        }
                    }
                } catch (IOException | SecurityException e) {
                    // ignore ZIP errors
                }
                // if no ZIP file: continue with copying the file
                if (!foundMapInZip && guessFilename(preset)) {
                    handleMapFile(this, false, null);
                }
            } else {
                finish();
            }
        });
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

    private void handleMapFile(final Activity activity, final boolean isZipFile, final String nameWithinZip) {
        // check whether the target file or its companion file already exist, and delete both, if so
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(downloader.targetFolder.getFolder(), false, false);

        final Uri companionFile = downloader.useCompanionFiles ? CompanionFileUtils.companionFileExists(files, filename) : null;
        if (companionFile != null) {
            ContentStorage.get().delete(companionFile);
        }

        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.equals(filename)) {
                ContentStorage.get().delete(fi.uri);
                break;
            }
        }

        // now start copy task
        new CopyTask(this, isZipFile, nameWithinZip).execute();
    }

    protected class CopyTask extends AsyncTaskWithProgressText<String, CopyStates> {
        private long bytesCopied = 0;
        private final String progressFormat = getString(R.string.receivedownload_amount_copied);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final Activity context;
        private final boolean isZipFile;
        private final String nameWithinZip;

        CopyTask(final Activity activity, final boolean isZipFile, final String nameWithinZip) {
            super(activity, activity.getString(R.string.receivedownload_intenttitle), "");
            setOnCancelListener((dialog, which) -> cancelled.set(true));
            context = activity;
            this.isZipFile = isZipFile;
            this.nameWithinZip = nameWithinZip;
            publishProgress(String.format(getString(R.string.receivedownload_receiving_file), filename));
        }

        @Override
        protected CopyStates doInBackgroundInternal(final String[] logTexts) {
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
                    if ((System.currentTimeMillis() - lastPublishTime) > 1000) { // avoid message flooding
                        publishProgress(String.format(progressFormat, Formatter.formatBytes(bytesCopied)));
                        lastPublishTime = System.currentTimeMillis();
                        // give UI some time to catch up
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }
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

        @Override
        protected void onPostExecuteInternal(final CopyStates status) {
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
            Dialogs.message(context, getString(R.string.receivedownload_intenttitle), result, getString(android.R.string.ok), (dialog, button) -> doFinish());
        }

    }

    private void doFinish() {
        finish();
    }

}
