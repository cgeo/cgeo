package cgeo.geocaching;

import cgeo.geocaching.storage.LocalStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class SelectTrackFileActivity extends AbstractSelectFileActivity {

    public SelectTrackFileActivity() {
        super("gpx", Intents.EXTRA_GPX_FILE, null /*Settings.getTrackFile()*/, false);
        setContext(SelectTrackFileActivity.this);
    }

    @Override
    protected List<File> getBaseFolders() {
        final ArrayList<File> list = new ArrayList<>();
        list.add(LocalStorage.getGpxImportDirectory());
        return list;
    }

}
