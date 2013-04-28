package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.files.AbstractFileListActivity;
import cgeo.geocaching.files.GPXImporter;
import cgeo.geocaching.ui.GPXListAdapter;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class GpxFileListActivity extends AbstractFileListActivity<GPXListAdapter> {

    public GpxFileListActivity() {
        super(new String[] { "gpx", "loc", "zip" });
    }

    @Override
    protected GPXListAdapter getAdapter(List<File> files) {
        return new GPXListAdapter(this, files);
    }

    @Override
    protected List<File> getBaseFolders() {
        return Collections.singletonList(new File(Settings.getGpxImportDir()));
    }

    public static void startSubActivity(Activity fromActivity, int listId) {
        final Intent intent = new Intent(fromActivity, GpxFileListActivity.class);
        intent.putExtra(Intents.EXTRA_LIST_ID, listId);
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
