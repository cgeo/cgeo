package cgeo.geocaching;

import cgeo.geocaching.files.FileList;
import cgeo.geocaching.files.GPXParser;
import cgeo.geocaching.files.LocParser;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.util.List;
import java.util.UUID;

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
    protected String[] getBaseFolders() {
        final String base = Environment.getExternalStorageDirectory().toString();
        return new String[] { base + "/gpx" };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            listId = extras.getInt(EXTRAS_LIST_ID);
        }
        if (listId <= 0) {
            listId = 1;
        }

    }

    @Override
    protected void setTitle() {
        setTitle(res.getString(R.string.gpx_import_title));
    }

    public void loadGPX(File file) {

        parseDialog = ProgressDialog.show(
                this,
                res.getString(R.string.gpx_import_title_reading_file),
                res.getString(R.string.gpx_import_loading),
                true,
                false);

        new loadCaches(file).start();
    }

    private class loadCaches extends Thread {

        File file = null;

        public loadCaches(File fileIn) {
            file = fileIn;
        }

        @Override
        public void run() {
            final UUID searchId;
            final String name = file.getName().toLowerCase();
            if (name.endsWith("gpx")) {
                searchId = GPXParser.parseGPX(file, listId, changeParseDialogHandler);
            }
            else {
                searchId = LocParser.parseLoc(file, listId, changeParseDialogHandler);
            }

            loadCachesHandler.sendMessage(loadCachesHandler.obtainMessage(0, app.getCount(searchId), 0));
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
            return !StringUtils.endsWithIgnoreCase(filename, "-wpts.gpx");
        } else {
            return false;
        }
    }
}
