package cgeo.geocaching;

import cgeo.geocaching.files.FileList;
import cgeo.geocaching.files.GPXParser;
import cgeo.geocaching.files.LocParser;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class cgeogpxes extends FileList<cgGPXListAdapter> {

    private static final String EXTRAS_LIST_ID = "list";

    public cgeogpxes() {
        super(new String[] { "gpx", "loc" });
    }

    private ProgressDialog parseDialog = null;
    private int listId = 1;

    final private Handler changeParseDialogHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (parseDialog != null) {
                parseDialog.setMessage(res.getString(msg.arg1) + " " + msg.arg2);
                if (msg.obj != null) {
                    final int progress = (Integer) msg.obj;
                    parseDialog.setProgress(progress);
                }
            }
        }
    };

    final private Handler loadCachesHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (parseDialog != null) {
                parseDialog.dismiss();
            }

            helpDialog(res.getString(R.string.gpx_import_title_caches_imported), msg.arg1 + " " + res.getString(R.string.gpx_import_caches_imported));
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
                            try {
                                final InputStream attachment = getContentResolver().openInputStream(getIntent().getData());
                                importGPX(attachment);
                            } catch (FileNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
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
        createProgressDialog((int) file.length());
        if (StringUtils.endsWithIgnoreCase(file.getName(), GPXParser.GPX_FILE_EXTENSION)) {
            new ImportGpxFileThread(file, listId, loadCachesHandler, changeParseDialogHandler).start();
        } else {
            new ImportLocFileThread(file, listId, loadCachesHandler, changeParseDialogHandler).start();
        }
    }

    public void importGPX(final InputStream stream) {
        createProgressDialog(-1);
        new ImportStreamThread(stream, listId, loadCachesHandler, changeParseDialogHandler).start();
    }

    private void createProgressDialog(int maxBytes) {
        parseDialog = new ProgressDialog(this);
        parseDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        parseDialog.setTitle(res.getString(R.string.gpx_import_title_reading_file));
        parseDialog.setMessage(res.getString(R.string.gpx_import_loading));
        parseDialog.setCancelable(false);
        parseDialog.setMax(maxBytes);
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
            final cgSearch search = doImport();
            importStepHandler.sendMessage(importStepHandler.obtainMessage(0, cgeoapplication.getCount(search), 0, search));
        }

        protected abstract cgSearch doImport();
    }

    static class ImportGpxFileThread extends ImportThread {
        private final File file;

        public ImportGpxFileThread(final File file, int listId, Handler importStepHandler, Handler progressHandler) {
            super(listId, importStepHandler, progressHandler);
            this.file = file;
        }

        @Override
        protected cgSearch doImport() {
            return GPXParser.importGPX(file, listId, progressHandler);
        }
    }

    static class ImportLocFileThread extends ImportThread {
        private final File file;

        public ImportLocFileThread(final File file, int listId, Handler importStepHandler, Handler progressHandler) {
            super(listId, importStepHandler, progressHandler);
            this.file = file;
        }

        @Override
        protected cgSearch doImport() {
            return LocParser.parseLoc(file, listId, progressHandler);
        }
    }

    static class ImportStreamThread extends ImportThread {
        private final InputStream stream;

        public ImportStreamThread(InputStream stream, int listId, Handler importStepHandler, Handler progressHandler) {
            super(listId, importStepHandler, progressHandler);
            this.stream = stream;
        }

        @Override
        protected cgSearch doImport() {
            return GPXParser.importGPX(stream, listId, progressHandler);
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
            return !StringUtils.endsWithIgnoreCase(filename, GPXParser.WAYPOINTS_FILE_SUFFIX_AND_EXTENSION);
        }
        return false;
    }
}
