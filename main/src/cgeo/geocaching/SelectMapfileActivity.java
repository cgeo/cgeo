package cgeo.geocaching;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PublicLocalFolder;


import java.io.File;
import java.util.List;

public class SelectMapfileActivity extends AbstractSelectFileActivity {

    public SelectMapfileActivity() {
        super("map", Intents.EXTRA_MAP_FILE, Settings.getPublicLocalFolderUri(PublicLocalFolder.OFFLINE_MAPS).toString(), true);
        setContext(SelectMapfileActivity.this);
    }

    @Override
    protected List<File> getBaseFolders() {
        return LocalStorage.getMapDirectories();
    }

}
