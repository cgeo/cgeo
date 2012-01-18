package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.files.FileList;
import cgeo.geocaching.files.GPXImporter;
import cgeo.geocaching.ui.GPXListAdapter;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.util.List;

public class cgeogpxes extends FileList<GPXListAdapter> {
    private static final String EXTRAS_LIST_ID = "list";

    public cgeogpxes() {
        super(new String[] { "gpx", "loc", "zip" });
    }

    private int listId = 1;

    @Override
    protected GPXListAdapter getAdapter(List<File> files) {
        return new GPXListAdapter(this, files);
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
            listId = StoredList.STANDARD_LIST_ID;
        }
    }

    @Override
    protected void setTitle() {
        setTitle(res.getString(R.string.gpx_import_title));
    }

    public static void startSubActivity(Activity fromActivity, int listId) {
        final Intent intent = new Intent(fromActivity, cgeogpxes.class);
        intent.putExtra(EXTRAS_LIST_ID, listId);
        fromActivity.startActivityForResult(intent, 0);
    }

    @Override
    protected boolean filenameBelongsToList(final String filename) {
        if (super.filenameBelongsToList(filename)) {
            if (StringUtils.endsWithIgnoreCase(filename, GPXImporter.ZIP_FILE_EXTENSION)) {
                for (IConnector connector : ConnectorFactory.getConnectors()) {
                    if (connector.isZippedGPXFile(filename)) {
                        return true;
                    }
                }
                return false;
            }
            // filter out waypoint files
            return StringUtils.lastIndexOfIgnoreCase(filename, GPXImporter.WAYPOINTS_FILE_SUFFIX) == -1;
        }
        return false;
    }

    public int getListId() {
        return listId;
    }

}
