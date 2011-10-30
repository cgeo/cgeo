package cgeo.geocaching;

import cgeo.geocaching.files.FileList;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.files.GPX11Parser;
import cgeo.geocaching.files.GPXParser;
import cgeo.geocaching.files.LocParser;
import cgeo.geocaching.files.ParserException;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class cgeogpxes extends FileList<cgGPXListAdapter> {
    public static final String GPX_FILE_EXTENSION = ".gpx";
    public static final String WAYPOINTS_FILE_SUFFIX_AND_EXTENSION = "-wpts.gpx";

    private static final String EXTRAS_LIST_ID = "list";

    static final int IMPORT_STEP_READ_FILE = 1;
    static final int IMPORT_STEP_READ_WPT_FILE = 2;
    static final int IMPORT_STEP_STORE_CACHES = 3;
    static final int IMPORT_STEP_FINISHED = 4;
    static final int IMPORT_STEP_FINISHED_WITH_ERROR = 5;

    public cgeogpxes() {
        super(new String[] { "gpx", "loc" });
    }

    private ProgressDialog parseDialog = null;
    private int listId = 1;

    final private Handler progressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            parseDialog.setProgress(msg.arg1);
        }
    };

    final private Handler importStepHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case IMPORT_STEP_READ_FILE:
                case IMPORT_STEP_READ_WPT_FILE:
                case IMPORT_STEP_STORE_CACHES:
                    parseDialog.setMessage(res.getString(msg.arg1));
                    parseDialog.setMax(msg.arg2);
                    parseDialog.setProgress(0);
                    break;

                case IMPORT_STEP_FINISHED:
                    parseDialog.dismiss();
                    helpDialog(res.getString(R.string.gpx_import_title_caches_imported), msg.arg1 + " " + res.getString(R.string.gpx_import_caches_imported));
                    break;

                case IMPORT_STEP_FINISHED_WITH_ERROR:
                    parseDialog.dismiss();
                    helpDialog(res.getString(R.string.gpx_import_title_caches_import_failed), res.getString(msg.arg1) + "\n\n" + msg.obj);
                    break;
            }
        }
    };

    @Override
    protected cgGPXListAdapter getAdapter(List<File> files) {
        return new cgGPXListAdapter(this, files);
    }

    @Override
    protected File[] getBaseFolders() {
        return new File[] { new File(Environment.getExternalStorageDirectory(), "gpx") };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            listId = extras.getInt(EXTRAS_LIST_ID);
        }
        if (listId <= 0) {
            listId = cgList.STANDARD_LIST_ID;
        }

        if ("content".equals(getIntent().getScheme())) {
            new AlertDialog.Builder(this)
                    .setTitle(res.getString(R.string.gpx_import_title))
                    .setMessage(res.getString(R.string.gpx_import_confirm))
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            importGPX((Uri) getIntent().getData(), getContentResolver());
                        }
                    })
                    .setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .create()
                    .show();
        }
    }

    @Override
    protected void setTitle() {
        setTitle(res.getString(R.string.gpx_import_title));
    }

    public void importGPX(final File file) {
        createProgressDialog();
        if (StringUtils.endsWithIgnoreCase(file.getName(), GPX_FILE_EXTENSION)) {
            new ImportGpxFileThread(file, listId, importStepHandler, progressHandler).start();
        } else {
            new ImportLocFileThread(file, listId, importStepHandler, progressHandler).start();
        }
    }

    public void importGPX(final Uri uri, ContentResolver contentResolver) {
        createProgressDialog();
        new ImportAttachmentThread(uri, contentResolver, listId, importStepHandler, progressHandler).start();
    }

    private void createProgressDialog() {
        parseDialog = new ProgressDialog(this);
        parseDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        parseDialog.setTitle(res.getString(R.string.gpx_import_title_reading_file));
        parseDialog.setMessage(res.getString(R.string.gpx_import_loading));
        parseDialog.setCancelable(false);
        parseDialog.setMax(-1);
        parseDialog.show();
    }

    static abstract class ImportThread extends Thread {
        final int listId;
        final Handler importStepHandler;
        final Handler progressHandler;

        public ImportThread(int listId, Handler importStepHandler, Handler progressHandler) {
            this.listId = listId;
            this.importStepHandler = importStepHandler;
            this.progressHandler = progressHandler;
        }

        @Override
        public void run() {
            final Collection<cgCache> caches;
            try {
                caches = doImport();

                importStepHandler.sendMessage(importStepHandler.obtainMessage(IMPORT_STEP_STORE_CACHES, R.string.gpx_import_storing, caches.size()));
                cgSearch search = storeParsedCaches(caches);
                Log.i(Settings.tag, "Imported successfully " + caches.size() + " caches.");

                importStepHandler.sendMessage(importStepHandler.obtainMessage(IMPORT_STEP_FINISHED, cgeoapplication.getCount(search), 0, search));
            } catch (IOException e) {
                Log.i(Settings.tag, "Importing caches failed - error reading data: " + e.getMessage());
                importStepHandler.sendMessage(importStepHandler.obtainMessage(IMPORT_STEP_FINISHED_WITH_ERROR, R.string.gpx_import_error_io, 0, e.getLocalizedMessage()));
            } catch (ParserException e) {
                Log.i(Settings.tag, "Importing caches failed - data format error" + e.getMessage());
                importStepHandler.sendMessage(importStepHandler.obtainMessage(IMPORT_STEP_FINISHED_WITH_ERROR, R.string.gpx_import_error_parser, 0, e.getLocalizedMessage()));
            } catch (Exception e) {
                Log.e(Settings.tag, "Importing caches failed - unknown error: ", e);
                importStepHandler.sendMessage(importStepHandler.obtainMessage(IMPORT_STEP_FINISHED_WITH_ERROR, R.string.gpx_import_error_unexpected, 0, e.getLocalizedMessage()));
            }
        }

        protected abstract Collection<cgCache> doImport() throws IOException, ParserException;

        private cgSearch storeParsedCaches(Collection<cgCache> caches) {
            final cgSearch search = new cgSearch();
            final cgeoapplication app = cgeoapplication.getInstance();
            int storedCaches = 0;
            for (cgCache cache : caches) {
                // remove from cache because a cache might be re-imported
                app.removeCacheFromCache(cache.geocode);
                app.addCacheToSearch(search, cache);
                progressHandler.sendMessage(progressHandler.obtainMessage(0, ++storedCaches, 0));
            }
            return search;
        }
    }

    static class ImportGpxFileThread extends ImportThread {
        private final File cacheFile;

        public ImportGpxFileThread(final File file, int listId, Handler importStepHandler, Handler progressHandler) {
            super(listId, importStepHandler, progressHandler);
            this.cacheFile = file;
        }

        @Override
        protected Collection<cgCache> doImport() throws IOException, ParserException {
            Log.i(Settings.tag, "Import GPX file: " + cacheFile.getAbsolutePath());
            importStepHandler.sendMessage(importStepHandler.obtainMessage(IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches, (int) cacheFile.length()));
            Collection<cgCache> caches;
            GPXParser parser;
            try {
                // try to parse cache file as GPX 10
                parser = new GPX10Parser(listId);
                caches = parser.parse(cacheFile, progressHandler);
            } catch (ParserException pe) {
                // didn't work -> lets try GPX11
                parser = new GPX11Parser(listId);
                caches = parser.parse(cacheFile, progressHandler);
            }

            final File wptsFile = getWaypointsFileForGpx(cacheFile);
            if (wptsFile != null && wptsFile.canRead()) {
                Log.i(Settings.tag, "Import GPX waypoint file: " + wptsFile.getAbsolutePath());
                importStepHandler.sendMessage(importStepHandler.obtainMessage(IMPORT_STEP_READ_WPT_FILE, R.string.gpx_import_loading_waypoints, (int) wptsFile.length()));
                caches = parser.parse(wptsFile, progressHandler);
            }

            return caches;
        }
    }

    static class ImportLocFileThread extends ImportThread {
        private final File file;

        public ImportLocFileThread(final File file, int listId, Handler importStepHandler, Handler progressHandler) {
            super(listId, importStepHandler, progressHandler);
            this.file = file;
        }

        @Override
        protected Collection<cgCache> doImport() throws IOException, ParserException {
            Log.i(Settings.tag, "Import LOC file: " + file.getAbsolutePath());
            importStepHandler.sendMessage(importStepHandler.obtainMessage(IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches, (int) file.length()));
            LocParser parser = new LocParser(listId);
            return parser.parse(file, progressHandler);
        }
    }

    static class ImportAttachmentThread extends ImportThread {
        private final Uri uri;
        private ContentResolver contentResolver;

        public ImportAttachmentThread(Uri uri, ContentResolver contentResolver, int listId, Handler importStepHandler, Handler progressHandler) {
            super(listId, importStepHandler, progressHandler);
            this.uri = uri;
            this.contentResolver = contentResolver;
        }

        @Override
        protected Collection<cgCache> doImport() throws IOException, ParserException {
            Log.i(Settings.tag, "Import GPX from uri: " + uri);
            importStepHandler.sendMessage(importStepHandler.obtainMessage(IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches, -1));

            Collection<cgCache> caches;
            GPXParser parser;
            try {
                // try to parse cache file as GPX 10
                parser = new GPX10Parser(listId);
                caches = parseAttachement(parser);
            } catch (ParserException pe) {
                // didn't work -> lets try GPX11
                parser = new GPX11Parser(listId);
                caches = parseAttachement(parser);
            }
            return caches;
        }

        private Collection<cgCache> parseAttachement(GPXParser parser) throws IOException, ParserException {
            InputStream is = contentResolver.openInputStream(uri);
            try {
                return parser.parse(is, progressHandler);
            } finally {
                is.close();
            }
        }
    }

    public static void startSubActivity(Activity fromActivity, int listId) {
        final Intent intent = new Intent(fromActivity, cgeogpxes.class);
        intent.putExtra(EXTRAS_LIST_ID, listId);
        fromActivity.startActivityForResult(intent, 0);
    }

    @Override
    protected boolean filenameBelongsToList(final String filename) {
        if (super.filenameBelongsToList(filename)) {
            // filter out waypoint files
            return !StringUtils.endsWithIgnoreCase(filename, WAYPOINTS_FILE_SUFFIX_AND_EXTENSION);
        }
        return false;
    }

    // 1234567.gpx -> 1234567-wpts.gpx
    static File getWaypointsFileForGpx(File file) {
        final String name = file.getName();
        if (StringUtils.endsWithIgnoreCase(name, GPX_FILE_EXTENSION) && (StringUtils.length(name) > GPX_FILE_EXTENSION.length())) {
            final String wptsName = StringUtils.substringBeforeLast(name, ".") + WAYPOINTS_FILE_SUFFIX_AND_EXTENSION;
            return new File(file.getParentFile(), wptsName);
        } else {
            return null;
        }
    }
}
