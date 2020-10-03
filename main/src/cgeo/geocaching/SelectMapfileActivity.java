package cgeo.geocaching;

import cgeo.geocaching.storage.ConfigurableFolder;
import cgeo.geocaching.storage.LocalStorage;

import java.io.File;
import java.util.List;

public class SelectMapfileActivity extends AbstractSelectFileActivity {

    public SelectMapfileActivity() {
        super("map", Intents.EXTRA_MAP_FILE, ConfigurableFolder.OFFLINE_MAPS.getFolder().getUri().toString(), true);
        setContext(SelectMapfileActivity.this);
    }

    @Override
    protected List<File> getBaseFolders() {
        return LocalStorage.getMapDirectories();
    }

}
