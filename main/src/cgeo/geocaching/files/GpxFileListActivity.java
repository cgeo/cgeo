package cgeo.geocaching.files;

import cgeo.geocaching.Intents;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class GpxFileListActivity extends AbstractFileListActivity<GPXListAdapter> {

    public GpxFileListActivity() {
        super(supportedFileTypes());
    }

    private static String[] supportedFileTypes() {
        final ArrayList<String> result = new ArrayList<>();
        for (final String dotExtension : Arrays.asList(GPXImporter.GPX_FILE_EXTENSION, GPXImporter.LOC_FILE_EXTENSION, GPXImporter.COMPRESSED_GPX_FILE_EXTENSION, GPXImporter.ZIP_FILE_EXTENSION)) {
            result.add(StringUtils.substringAfter(dotExtension, "."));
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    protected GPXListAdapter getAdapter(final List<File> files) {
        return new GPXListAdapter(this, files);
    }

    @Override
    protected List<File> getBaseFolders() {
        return Collections.singletonList(new File(Settings.getGpxImportDir()));
    }

    public static void startSubActivity(final Activity fromActivity, final int listId, final int requestCode) {
        final Intent intent = new Intent(fromActivity, GpxFileListActivity.class);
        intent.putExtra(Intents.EXTRA_LIST_ID, StoredList.getConcreteList(listId));
        fromActivity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected boolean filenameBelongsToList(@NonNull final String filename) {
        if (super.filenameBelongsToList(filename)) {
            if (StringUtils.endsWithIgnoreCase(filename, GPXImporter.ZIP_FILE_EXTENSION)) {
                for (final IConnector connector : ConnectorFactory.getConnectors()) {
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
