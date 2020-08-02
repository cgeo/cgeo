package cgeo.geocaching;

import cgeo.geocaching.storage.LocalStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectIndividualRouteFileActivity extends AbstractSelectFileActivity {

    public SelectIndividualRouteFileActivity() {
        super("gpx", Intents.EXTRA_GPX_FILE, "route.gpx", false);
        setContext(SelectIndividualRouteFileActivity.this);
    }

    @Override
    protected List<File> getBaseFolders() {
        final ArrayList<File> list = new ArrayList<>();
        list.add(LocalStorage.getGpxImportDirectory());
        return list;
    }

}
