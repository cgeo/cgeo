package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.files.FileList;
import cgeo.geocaching.files.GPXImporter;
import cgeo.geocaching.files.GpxScanChoice;
import cgeo.geocaching.ui.GPXListAdapter;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.io.File;
import java.util.List;

public class cgeogpxes extends FileList<GPXListAdapter> {
    private static final String EXTRAS_LIST_ID = "list";
    private static final String EXTRAS_SCAN_CHOICE = "scanChoice";

    public cgeogpxes() {
        super(new String[] { "gpx", "loc", "zip" });
    }

    private int listId = StoredList.STANDARD_LIST_ID;
    private GpxScanChoice scanChoice = GpxScanChoice.DEFAULT_DIR;

    @Override
    protected GPXListAdapter getAdapter(List<File> files) {
        return new GPXListAdapter(this, files);
    }

    @Override
    protected File[] getBaseFolders() {
        // skip setting default dir scan
        if (scanChoice.equals(GpxScanChoice.FORCE_SCAN)) {
            return new File[0];
        }
        String gpxImportDir = Settings.getGpxImportDir();
        return new File[] { new File(gpxImportDir) };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            listId = extras.getInt(EXTRAS_LIST_ID);
            scanChoice = (GpxScanChoice) extras.get(EXTRAS_SCAN_CHOICE);
        }
        if (listId <= StoredList.TEMPORARY_LIST_ID) {
            listId = StoredList.STANDARD_LIST_ID;
        }
    }

    @Override
    protected void setTitle() {
        setTitle(res.getString(R.string.gpx_import_title));
    }

    public static void startSubActivity(Activity fromActivity, int listId, GpxScanChoice choice) {
        final Intent intent = new Intent(fromActivity, cgeogpxes.class);
        intent.putExtra(EXTRAS_LIST_ID, listId);
        intent.putExtra(EXTRAS_SCAN_CHOICE, choice);
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
            return !StringUtils.containsIgnoreCase(filename, GPXImporter.WAYPOINTS_FILE_SUFFIX);
        }
        return false;
    }

    public int getListId() {
        return listId;
    }

}
